package com.zhengshu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EvidencePackage(
    val id: String,
    val title: String,
    val description: String,
    val screenRecordPath: String? = null,
    val screenshotPaths: List<String> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val environmentData: EnvironmentData,
    val timestamp: Long,
    val hashValue: String,
    val blockchainAddress: String? = null
)

@Serializable
data class EnvironmentData(
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String,
    val networkType: String,
    val ipAddress: String? = null,
    val location: String? = null
)