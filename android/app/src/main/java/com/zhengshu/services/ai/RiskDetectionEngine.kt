package com.zhengshu.services.ai

import android.content.Context
import android.util.Log
import com.zhengshu.data.model.BehaviorData
import com.zhengshu.data.model.BehaviorType
import com.zhengshu.data.model.ChatMessage
import com.zhengshu.data.model.RiskDetectionResult
import com.zhengshu.data.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RiskDetectionEngine(private val context: Context) {
    
    private val keywordLibraryManager = KeywordLibraryManager(context)
    private val behaviorRuleEngine = BehaviorRuleEngine()
    
    companion object {
        private const val TAG = "RiskDetectionEngine"
        
        private val RISKY_PLATFORMS = listOf(
            "unknown", "temp", "test", "临时", "测试", "未知"
        )
        
        private val PHONE_NUMBER_PATTERN = Regex("\\d{11}")
        
        private val SUSPICIOUS_KEYWORDS = listOf(
            "客服", "官方", "专员", "经理", "主管", "总监", "中心",
            "热线", "电话", "手机", "座机", "号码", "联系方式",
            "联系", "QQ", "微信", "钉钉", "企业微信", "工作号"
        )
        
        private val SUSPICIOUS_APPS = listOf(
            "com.unknown", "com.temp", "com.test"
        )
    }
    
    suspend fun analyzeText(text: String): RiskDetectionResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "analyzeText called: ${text.take(100)}")
        
        val keywordMatches = keywordLibraryManager.searchKeywords(text)
        val highestRiskLevel = keywordLibraryManager.getHighestRiskLevel(text)
        val riskReason = keywordLibraryManager.getRiskReason(text)
        
        Log.d(TAG, "analyzeText result: highestRiskLevel=$highestRiskLevel, riskReason=$riskReason, matches=${keywordMatches.size}")
        
        val detectedKeywords = keywordMatches.map { it.keyword }
        val confidence = calculateConfidence(keywordMatches)
        
        RiskDetectionResult(
            riskLevel = highestRiskLevel,
            riskReason = riskReason,
            confidence = confidence,
            detectedKeywords = detectedKeywords,
            detectedBehaviors = emptyList()
        )
    }
    
    suspend fun analyzeChatMessage(message: ChatMessage): RiskDetectionResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "analyzeChatMessage called: sender=${message.sender}, content=${message.content.take(50)}")
        
        val textResult = analyzeText(message.content)
        
        val platformRisk = analyzePlatform(message.platform)
        val senderRisk = analyzeSender(message.sender)
        val timePatternRisk = analyzeTimePattern(message.timestamp)
        
        Log.d(TAG, "Risk levels: text=${textResult.riskLevel}, platform=$platformRisk, sender=$senderRisk, time=$timePatternRisk")
        
        val combinedRiskLevel = combineRiskLevels(
            textResult.riskLevel,
            platformRisk,
            senderRisk,
            timePatternRisk
        )
        
        Log.d(TAG, "Combined risk level: $combinedRiskLevel")
        
        val combinedReason = buildString {
            if (textResult.riskReason.isNotEmpty()) {
                append(textResult.riskReason)
            }
            if (platformRisk != RiskLevel.NONE) {
                if (isNotEmpty()) append("；")
                append("平台来源风险：${message.platform}")
            }
            if (senderRisk != RiskLevel.NONE) {
                if (isNotEmpty()) append("；")
                append("发送者风险：${message.sender}")
            }
            if (timePatternRisk != RiskLevel.NONE) {
                if (isNotEmpty()) append("；")
                append("发送时间异常")
            }
        }
        
        Log.d(TAG, "Combined reason: $combinedReason")
        
        RiskDetectionResult(
            riskLevel = combinedRiskLevel,
            riskReason = combinedReason,
            confidence = textResult.confidence,
            detectedKeywords = textResult.detectedKeywords,
            detectedBehaviors = emptyList()
        )
    }
    
    suspend fun analyzeBehavior(behavior: BehaviorData): RiskDetectionResult = withContext(Dispatchers.Default) {
        val ruleResults = behaviorRuleEngine.evaluate(behavior)
        
        val highestRiskLevel = ruleResults.maxByOrNull { it.riskLevel }?.riskLevel ?: RiskLevel.NONE
        val riskReason = ruleResults
            .filter { it.riskLevel != RiskLevel.NONE }
            .joinToString("；") { it.reason }
        
        val detectedBehaviors = ruleResults.map { it.behaviorType.name }
        
        RiskDetectionResult(
            riskLevel = highestRiskLevel,
            riskReason = riskReason,
            confidence = 0.7f,
            detectedKeywords = emptyList(),
            detectedBehaviors = detectedBehaviors
        )
    }
    
    suspend fun analyzeComprehensive(
        text: String,
        chatMessages: List<ChatMessage>,
        behaviors: List<BehaviorData>
    ): RiskDetectionResult = withContext(Dispatchers.Default) {
        val textResult = analyzeText(text)
        val chatResults = chatMessages.map { analyzeChatMessage(it) }
        val behaviorResults = behaviors.map { analyzeBehavior(it) }
        
        val allResults = listOf(textResult) + chatResults + behaviorResults
        
        val highestRiskLevel = allResults.maxByOrNull { it.riskLevel }?.riskLevel ?: RiskLevel.NONE
        val combinedReason = allResults
            .filter { it.riskReason.isNotEmpty() }
            .joinToString("；") { it.riskReason }
        
        val allKeywords = allResults.flatMap { it.detectedKeywords }.distinct()
        val allBehaviors = allResults.flatMap { it.detectedBehaviors }.distinct()
        
        val avgConfidence = allResults.map { it.confidence }.average().toFloat()
        
        RiskDetectionResult(
            riskLevel = highestRiskLevel,
            riskReason = combinedReason,
            confidence = avgConfidence,
            detectedKeywords = allKeywords,
            detectedBehaviors = allBehaviors
        )
    }
    
    private fun analyzePlatform(platform: String): RiskLevel {
        return if (RISKY_PLATFORMS.any { platform.contains(it, ignoreCase = true) }) {
            RiskLevel.MEDIUM
        } else {
            RiskLevel.NONE
        }
    }
    
    private fun analyzeSender(sender: String): RiskLevel {
        if (PHONE_NUMBER_PATTERN.matches(sender)) {
            return RiskLevel.MEDIUM
        }
        
        val suspiciousPattern = buildString {
            append("(${SUSPICIOUS_KEYWORDS.joinToString("|")})")
            append("(\\d*)?")
            append("((${SUSPICIOUS_KEYWORDS.joinToString("|")})*)?")
        }
        
        return if (Regex(suspiciousPattern, RegexOption.IGNORE_CASE).containsMatchIn(sender)) {
            RiskLevel.MEDIUM
        } else {
            RiskLevel.NONE
        }
    }
    
    private fun analyzeTimePattern(timestamp: Long): RiskLevel {
        val hour = (timestamp / 3600000) % 24
        
        return if (hour in 0..5 || hour == 23) {
            RiskLevel.LOW
        } else {
            RiskLevel.NONE
        }
    }
    
    private fun combineRiskLevels(vararg levels: RiskLevel): RiskLevel {
        return levels.minByOrNull { it.ordinal } ?: RiskLevel.NONE
    }
    
    private fun calculateConfidence(matches: List<KeywordMatch>): Float {
        if (matches.isEmpty()) return 0f
        
        val highRiskCount = matches.count { it.riskLevel == RiskLevel.HIGH }
        val mediumRiskCount = matches.count { it.riskLevel == RiskLevel.MEDIUM }
        val lowRiskCount = matches.count { it.riskLevel == RiskLevel.LOW }
        
        val total = matches.size
        val weightedScore = (highRiskCount * 1.0f + mediumRiskCount * 0.6f + lowRiskCount * 0.3f) / total
        
        return weightedScore.coerceIn(0f, 1f)
    }
}

data class BehaviorRuleResult(
    val riskLevel: RiskLevel,
    val reason: String,
    val behaviorType: BehaviorType
)

class BehaviorRuleEngine {
    
    private val rules = listOf(
        BehaviorRule(
            name = "高频点击",
            condition = { behavior -> 
                behavior.type == BehaviorType.CLICK && behavior.clickFrequency >= 10 
            },
            riskLevel = RiskLevel.HIGH,
            reason = "检测到异常高频点击行为"
        ),
        BehaviorRule(
            name = "快速输入",
            condition = { behavior -> 
                behavior.type == BehaviorType.INPUT && behavior.inputSpeed > 500 
            },
            riskLevel = RiskLevel.MEDIUM,
            reason = "检测到异常快速输入行为"
        ),
        BehaviorRule(
            name = "异常导航",
            condition = { behavior -> 
                behavior.type == BehaviorType.NAVIGATION && 
                behavior.packageName.contains("unknown", ignoreCase = true)
            },
            riskLevel = RiskLevel.MEDIUM,
            reason = "检测到来自未知来源的导航行为"
        ),
        BehaviorRule(
            name = "可疑应用操作",
            condition = { behavior ->
                val suspiciousApps = listOf(
                    "com.unknown",
                    "com.temp",
                    "com.test"
                )
                suspiciousApps.any { behavior.packageName.contains(it, ignoreCase = true) }
            },
            riskLevel = RiskLevel.HIGH,
            reason = "检测到来自可疑应用的操作"
        ),
        BehaviorRule(
            name = "异常转账行为",
            condition = { behavior ->
                behavior.type == BehaviorType.NAVIGATION &&
                behavior.packageName.contains("bank", ignoreCase = true) &&
                behavior.clickFrequency > 5
            },
            riskLevel = RiskLevel.HIGH,
            reason = "检测到异常的银行应用操作"
        ),
        BehaviorRule(
            name = "频繁切换应用",
            condition = { behavior ->
                behavior.type == BehaviorType.NAVIGATION &&
                behavior.clickFrequency > 20
            },
            riskLevel = RiskLevel.MEDIUM,
            reason = "检测到频繁切换应用的行为"
        )
    )
    
    fun evaluate(behavior: BehaviorData): List<BehaviorRuleResult> {
        return rules.mapNotNull { rule ->
            if (rule.condition(behavior)) {
                BehaviorRuleResult(
                    riskLevel = rule.riskLevel,
                    reason = rule.reason,
                    behaviorType = behavior.type
                )
            } else {
                null
            }
        }
    }
}

data class BehaviorRule(
    val name: String,
    val condition: (BehaviorData) -> Boolean,
    val riskLevel: RiskLevel,
    val reason: String
)
