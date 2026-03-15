package com.zhengshu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val platform: String,
    val isRevoked: Boolean = false,
    val isDeleted: Boolean = false
)