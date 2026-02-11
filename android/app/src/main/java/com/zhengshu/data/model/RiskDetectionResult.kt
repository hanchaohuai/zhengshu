package com.zhengshu.data.model

data class RiskDetectionResult(
    val riskLevel: RiskLevel,
    val riskReason: String,
    val confidence: Float,
    val detectedKeywords: List<String>,
    val detectedBehaviors: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RiskLevel(val displayName: String, val colorRes: Int) {
    HIGH("高风险", android.R.color.holo_red_dark),
    MEDIUM("中风险", android.R.color.holo_orange_dark),
    LOW("低风险", android.R.color.holo_orange_light),
    NONE("无风险", android.R.color.holo_green_dark)
}
