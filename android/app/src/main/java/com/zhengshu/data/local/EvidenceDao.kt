package com.zhengshu.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zhengshu.data.model.Evidence
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    
    @Query("SELECT * FROM evidence ORDER BY createdAt DESC")
    fun getAllEvidence(): Flow<List<Evidence>>
    
    @Query("SELECT * FROM evidence WHERE id = :id")
    suspend fun getEvidenceById(id: Long): Evidence?
    
    @Query("SELECT * FROM evidence WHERE riskLevel = :riskLevel ORDER BY createdAt DESC")
    fun getEvidenceByRiskLevel(riskLevel: String): Flow<List<Evidence>>
    
    @Query("SELECT * FROM evidence WHERE isSynced = :isSynced ORDER BY createdAt DESC")
    fun getEvidenceBySyncStatus(isSynced: Boolean): Flow<List<Evidence>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: Evidence): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEvidence(evidenceList: List<Evidence>)
    
    @Update
    suspend fun updateEvidence(evidence: Evidence)
    
    @Delete
    suspend fun deleteEvidence(evidence: Evidence)
    
    @Query("DELETE FROM evidence WHERE id = :id")
    suspend fun deleteEvidenceById(id: Long)
    
    @Query("DELETE FROM evidence WHERE createdAt < :timestamp")
    suspend fun deleteOldEvidence(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM evidence")
    suspend fun getEvidenceCount(): Int
}
