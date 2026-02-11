package com.zhengshu.data.model

enum class BehaviorType {
    CLICK,
    INPUT,
    SCROLL,
    NAVIGATION
}

data class BehaviorData(
    val type: BehaviorType,
    val packageName: String,
    val className: String,
    val timestamp: Long,
    val clickFrequency: Int = 0,
    val inputSpeed: Int = 0
)