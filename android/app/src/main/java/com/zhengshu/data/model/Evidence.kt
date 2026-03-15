package com.zhengshu.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evidence")
data class Evidence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val filePath: String,
    val hashValue: String,
    val timestamp: Long,
    val blockchainAddress: String? = null,
    val riskLevel: String,
    val isEncrypted: Boolean = true,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
