package com.zhengshu.data.repository

import com.zhengshu.data.local.EvidenceDao
import com.zhengshu.data.model.Evidence
import kotlinx.coroutines.flow.Flow

class EvidenceRepository(private val evidenceDao: EvidenceDao) {
    
    fun getAllEvidence(): Flow<List<Evidence>> = evidenceDao.getAllEvidence()
    
    suspend fun getEvidenceById(id: Long): Evidence? = evidenceDao.getEvidenceById(id)
    
    fun getEvidenceByRiskLevel(riskLevel: String): Flow<List<Evidence>> = 
        evidenceDao.getEvidenceByRiskLevel(riskLevel)
    
    fun getEvidenceBySyncStatus(isSynced: Boolean): Flow<List<Evidence>> = 
        evidenceDao.getEvidenceBySyncStatus(isSynced)
    
    suspend fun insertEvidence(evidence: Evidence): Long = 
        evidenceDao.insertEvidence(evidence)
    
    suspend fun updateEvidence(evidence: Evidence) = 
        evidenceDao.updateEvidence(evidence)
    
    suspend fun deleteEvidence(evidence: Evidence) = 
        evidenceDao.deleteEvidence(evidence)
    
    suspend fun deleteEvidenceById(id: Long) = 
        evidenceDao.deleteEvidenceById(id)
    
    suspend fun deleteOldEvidence(timestamp: Long) = 
        evidenceDao.deleteOldEvidence(timestamp)
    
    suspend fun getEvidenceCount(): Int = 
        evidenceDao.getEvidenceCount()
}
