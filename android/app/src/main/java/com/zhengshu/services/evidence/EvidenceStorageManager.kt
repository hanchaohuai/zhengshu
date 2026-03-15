package com.zhengshu.services.evidence

import android.content.Context
import android.util.Log
import com.zhengshu.data.model.Evidence
import com.zhengshu.data.repository.EvidenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class EvidenceStorageManager(
    private val context: Context,
    private val repository: EvidenceRepository
) {

    companion object {
        private const val EVIDENCE_DIR = "evidence"
        private const val CLEANUP_DAYS = 180
        private const val CLEANUP_WARNING_DAYS = 7
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _storageStatusFlow = MutableSharedFlow<StorageStatus>()
    val storageStatusFlow = _storageStatusFlow.asSharedFlow()
    
    private val _cleanupWarningFlow = MutableSharedFlow<CleanupWarning>()
    val cleanupWarningFlow = _cleanupWarningFlow.asSharedFlow()

    private val evidenceDirectory: File
        get() = File(context.getExternalFilesDir(null), EVIDENCE_DIR)

    init {
        if (!evidenceDirectory.exists()) {
            evidenceDirectory.mkdirs()
        }
        
        scope.launch {
            checkCleanupNeeded()
        }
    }

    suspend fun saveEvidence(evidence: Evidence): Result<String> {
        return try {
            val id = repository.insertEvidence(evidence)
            Result.success("证据已保存，ID: $id")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEvidence(evidence: Evidence): Result<String> {
        return try {
            repository.deleteEvidence(evidence)
            deleteEvidenceFiles(evidence)
            Result.success("证据已删除")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEvidenceById(id: Long): Result<String> {
        return try {
            val evidence = repository.getEvidenceById(id)
            if (evidence != null) {
                repository.deleteEvidenceById(id)
                deleteEvidenceFiles(evidence)
                Result.success("证据已删除")
            } else {
                Result.failure(Exception("证据不存在"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteEvidenceFiles(evidence: Evidence) {
        val filePath = evidence.filePath
        if (filePath.isNotEmpty()) {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    suspend fun getStorageInfo(): StorageInfo {
        val totalSpace = evidenceDirectory.totalSpace
        val freeSpace = evidenceDirectory.freeSpace
        val usedSpace = totalSpace - freeSpace
        val usagePercent = (usedSpace.toFloat() / totalSpace * 100).toInt()
        
        val evidenceCount = repository.getEvidenceCount()
        
        return StorageInfo(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            freeSpace = freeSpace,
            usagePercent = usagePercent,
            evidenceCount = evidenceCount
        )
    }

    suspend fun cleanupOldEvidence(): Result<CleanupResult> {
        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(CLEANUP_DAYS.toLong())
            
            val oldEvidenceList = repository.getOldEvidence(cutoffTime).first()
            
            var deletedCount = 0
            var freedSpace = 0L
            
            oldEvidenceList.forEach { evidence ->
                try {
                    val fileSize = File(evidence.filePath).length()
                    repository.deleteEvidence(evidence)
                    deleteEvidenceFiles(evidence)
                    deletedCount++
                    freedSpace += fileSize
                } catch (e: Exception) {
                    Log.w("EvidenceStorageManager", "Failed to delete evidence: ${evidence.id}", e)
                }
            }
            
            val result = CleanupResult(
                deletedCount = deletedCount,
                freedSpace = freedSpace
            )
            
            _storageStatusFlow.emit(StorageStatus.CleanupCompleted(result))
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun checkCleanupNeeded() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis((CLEANUP_DAYS - CLEANUP_WARNING_DAYS).toLong())
        
        val oldEvidenceList = repository.getOldEvidence(cutoffTime).first()
        
        if (oldEvidenceList.isNotEmpty()) {
            _cleanupWarningFlow.emit(
                CleanupWarning(
                    evidenceCount = oldEvidenceList.size,
                    cleanupDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(CLEANUP_WARNING_DAYS.toLong())
                )
            )
        }
    }

    suspend fun exportEvidence(evidence: Evidence, destinationPath: String): Result<String> {
        if (destinationPath.isEmpty()) {
            return Result.failure(Exception("目标路径不能为空"))
        }
        
        return try {
            val sourceFile = File(evidence.filePath)
            val destFile = File(destinationPath)
            
            if (!sourceFile.exists()) {
                return Result.failure(Exception("源文件不存在: ${evidence.filePath}"))
            }
            
            destFile.parentFile?.mkdirs()
            
            sourceFile.copyTo(destFile, overwrite = true)
            
            Result.success("证据已导出到: $destinationPath")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backupEvidence(evidenceList: List<Evidence>, backupPath: String): Result<String> {
        if (backupPath.isEmpty()) {
            return Result.failure(Exception("备份路径不能为空"))
        }
        
        return try {
            val backupDir = File(backupPath)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            var successCount = 0
            var failCount = 0
            
            evidenceList.forEach { evidence ->
                try {
                    val sourceFile = File(evidence.filePath)
                    if (sourceFile.exists()) {
                        val destFile = File(backupDir, "${evidence.id}.proof")
                        sourceFile.copyTo(destFile, overwrite = true)
                        successCount++
                    } else {
                        failCount++
                    }
                } catch (e: Exception) {
                    failCount++
                }
            }
            
            val result = if (failCount == 0) {
                "备份成功，共备份 $successCount 个证据"
            } else {
                "备份完成，成功 $successCount 个，失败 $failCount 个"
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEvidenceByRiskLevel(riskLevel: String): List<Evidence> {
        return repository.getEvidenceByRiskLevel(riskLevel).first()
    }

    suspend fun getEvidenceBySyncStatus(isSynced: Boolean): List<Evidence> {
        return repository.getEvidenceBySyncStatus(isSynced).first()
    }
}

data class StorageInfo(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercent: Int,
    val evidenceCount: Int
)

data class CleanupResult(
    val deletedCount: Int,
    val freedSpace: Long
)

data class CleanupWarning(
    val evidenceCount: Int,
    val cleanupDate: Long
)

sealed class StorageStatus {
    object Idle : StorageStatus()
    object Checking : StorageStatus()
    data class CleanupCompleted(val result: CleanupResult) : StorageStatus()
    data class Error(val message: String) : StorageStatus()
}
