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
    
    suspend fun analyzeText(text: String): RiskDetectionResult = withContext(Dispatchers.Default) {
        Log.d("RiskDetectionEngine", "analyzeText called: ${text.take(100)}")
        
        val keywordMatches = keywordLibraryManager.searchKeywords(text)
        val highestRiskLevel = keywordLibraryManager.getHighestRiskLevel(text)
        val riskReason = keywordLibraryManager.getRiskReason(text)
        
        Log.d("RiskDetectionEngine", "analyzeText result: highestRiskLevel=$highestRiskLevel, riskReason=$riskReason, matches=${keywordMatches.size}")
        
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
        Log.d("RiskDetectionEngine", "analyzeChatMessage called: sender=${message.sender}, content=${message.content.take(50)}")
        
        val textResult = analyzeText(message.content)
        
        val platformRisk = analyzePlatform(message.platform)
        val senderRisk = analyzeSender(message.sender)
        val timePatternRisk = analyzeTimePattern(message.timestamp)
        
        Log.d("RiskDetectionEngine", "Risk levels: text=${textResult.riskLevel}, platform=$platformRisk, sender=$senderRisk, time=$timePatternRisk")
        
        val combinedRiskLevel = combineRiskLevels(
            textResult.riskLevel,
            platformRisk,
            senderRisk,
            timePatternRisk
        )
        
        Log.d("RiskDetectionEngine", "Combined risk level: $combinedRiskLevel")
        
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
        
        Log.d("RiskDetectionEngine", "Combined reason: $combinedReason")
        
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
        val riskyPlatforms = listOf(
            "unknown",
            "temp",
            "test",
            "临时",
            "测试",
            "未知"
        )
        
        return if (riskyPlatforms.any { platform.contains(it, ignoreCase = true) }) {
            RiskLevel.MEDIUM
        } else {
            RiskLevel.NONE
        }
    }
    
    private fun analyzeSender(sender: String): RiskLevel {
        val suspiciousPatterns = listOf(
            Regex("\\d{11}"),
            Regex("客服"),
            Regex("官方"),
            Regex("客服\\d+"),
            Regex("官方\\d+"),
            Regex("客服专员"),
            Regex("客服经理"),
            Regex("客服主管"),
            Regex("客服总监"),
            Regex("官方客服"),
            Regex("官方专员"),
            Regex("官方经理"),
            Regex("官方主管"),
            Regex("官方总监"),
            Regex("客服中心"),
            Regex("官方中心"),
            Regex("客服热线"),
            Regex("官方热线"),
            Regex("客服电话"),
            Regex("官方电话"),
            Regex("客服QQ"),
            Regex("官方QQ"),
            Regex("客服微信"),
            Regex("官方微信"),
            Regex("客服钉钉"),
            Regex("官方钉钉"),
            Regex("客服企业微信"),
            Regex("官方企业微信"),
            Regex("客服工作号"),
            Regex("官方工作号"),
            Regex("客服工作微信"),
            Regex("官方工作微信"),
            Regex("客服工作QQ"),
            Regex("官方工作QQ"),
            Regex("客服工作钉钉"),
            Regex("官方工作钉钉"),
            Regex("客服工作企业微信"),
            Regex("官方工作企业微信"),
            Regex("客服工作热线"),
            Regex("官方工作热线"),
            Regex("客服工作电话"),
            Regex("官方工作电话"),
            Regex("客服工作手机"),
            Regex("官方工作手机"),
            Regex("客服工作座机"),
            Regex("官方工作座机"),
            Regex("客服工作号码"),
            Regex("官方工作号码"),
            Regex("客服工作联系方式"),
            Regex("官方工作联系方式"),
            Regex("客服工作联系"),
            Regex("官方工作联系"),
            Regex("客服工作联系号码"),
            Regex("官方工作联系号码"),
            Regex("客服工作联系手机"),
            Regex("官方工作联系手机"),
            Regex("客服工作联系电话"),
            Regex("官方工作联系电话"),
            Regex("客服工作联系热线"),
            Regex("官方工作联系热线"),
            Regex("客服工作联系QQ"),
            Regex("官方工作联系QQ"),
            Regex("客服工作联系微信"),
            Regex("官方工作联系微信"),
            Regex("客服工作联系钉钉"),
            Regex("官方工作联系钉钉"),
            Regex("客服工作联系企业微信"),
            Regex("官方工作联系企业微信"),
            Regex("客服工作联系工作号"),
            Regex("官方工作联系工作号"),
            Regex("客服工作联系工作微信"),
            Regex("官方工作联系工作微信"),
            Regex("客服工作联系工作QQ"),
            Regex("官方工作联系工作QQ"),
            Regex("客服工作联系工作钉钉"),
            Regex("官方工作联系工作钉钉"),
            Regex("客服工作联系工作企业微信"),
            Regex("官方工作联系工作企业微信"),
            Regex("客服工作联系工作热线"),
            Regex("官方工作联系工作热线"),
            Regex("客服工作联系工作电话"),
            Regex("官方工作联系工作电话"),
            Regex("客服工作联系工作手机"),
            Regex("官方工作联系工作手机"),
            Regex("客服工作联系工作座机"),
            Regex("官方工作联系工作座机"),
            Regex("客服工作联系工作号码"),
            Regex("官方工作联系工作号码"),
            Regex("客服工作联系工作联系方式"),
            Regex("官方工作联系工作联系方式"),
            Regex("客服工作联系工作联系"),
            Regex("官方工作联系工作联系"),
            Regex("客服工作联系工作联系号码"),
            Regex("官方工作联系工作联系号码"),
            Regex("客服工作联系工作联系手机"),
            Regex("官方工作联系工作联系手机"),
            Regex("客服工作联系工作联系电话"),
            Regex("官方工作联系工作联系电话"),
            Regex("客服工作联系工作联系热线"),
            Regex("官方工作联系工作联系热线"),
            Regex("客服工作联系工作联系QQ"),
            Regex("官方工作联系工作联系QQ"),
            Regex("客服工作联系工作联系微信"),
            Regex("官方工作联系工作联系微信"),
            Regex("客服工作联系工作联系钉钉"),
            Regex("官方工作联系工作联系钉钉"),
            Regex("客服工作联系工作联系企业微信"),
            Regex("官方工作联系工作联系企业微信"),
            Regex("客服工作联系工作联系工作号"),
            Regex("官方工作联系工作联系工作号"),
            Regex("客服工作联系工作联系工作微信"),
            Regex("官方工作联系工作联系工作微信"),
            Regex("客服工作联系工作联系工作QQ"),
            Regex("官方工作联系工作联系工作QQ"),
            Regex("客服工作联系工作联系工作钉钉"),
            Regex("官方工作联系工作联系工作钉钉"),
            Regex("客服工作联系工作联系工作企业微信"),
            Regex("官方工作联系工作联系工作企业微信"),
            Regex("客服工作联系工作联系工作热线"),
            Regex("官方工作联系工作联系工作热线"),
            Regex("客服工作联系工作联系电话"),
            Regex("官方工作联系工作联系电话"),
            Regex("客服工作联系工作联系手机"),
            Regex("官方工作联系工作联系手机"),
            Regex("客服工作联系工作联系座机"),
            Regex("官方工作联系工作联系座机"),
            Regex("客服工作联系工作联系号码"),
            Regex("官方工作联系工作联系号码"),
            Regex("客服工作联系工作联系方式"),
            Regex("官方工作联系工作联系方式"),
            Regex("客服工作联系工作联系联系"),
            Regex("官方工作联系工作联系联系"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系电话"),
            Regex("官方工作联系工作联系联系电话"),
            Regex("客服工作联系工作联系联系热线"),
            Regex("官方工作联系工作联系联系热线"),
            Regex("客服工作联系工作联系联系QQ"),
            Regex("官方工作联系工作联系联系QQ"),
            Regex("客服工作联系工作联系联系微信"),
            Regex("官方工作联系工作联系联系微信"),
            Regex("客服工作联系工作联系联系钉钉"),
            Regex("官方工作联系工作联系联系钉钉"),
            Regex("客服工作联系工作联系联系企业微信"),
            Regex("官方工作联系工作联系联系企业微信"),
            Regex("客服工作联系工作联系联系工作号"),
            Regex("官方工作联系工作联系联系工作号"),
            Regex("客服工作联系工作联系联系工作微信"),
            Regex("官方工作联系工作联系联系工作微信"),
            Regex("客服工作联系工作联系联系工作QQ"),
            Regex("官方工作联系工作联系联系工作QQ"),
            Regex("客服工作联系工作联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系工作热线"),
            Regex("官方工作联系工作联系联系工作热线"),
            Regex("客服工作联系工作联系联系电话"),
            Regex("官方工作联系工作联系电话"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系座机"),
            Regex("官方工作联系工作联系联系座机"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系工作联系联系方式"),
            Regex("官方工作联系工作联系联系方式"),
            Regex("客服工作联系工作联系联系联系"),
            Regex("官方工作联系工作联系联系联系"),
            Regex("客服工作联系工作联系联系联系号码"),
            Regex("官方工作联系工作联系联系联系号码"),
            Regex("客服工作联系工作联系联系联系手机"),
            Regex("官方工作联系工作联系联系联系手机"),
            Regex("客服工作联系工作联系联系联系电话"),
            Regex("官方工作联系工作联系联系联系电话"),
            Regex("客服工作联系工作联系联系联系热线"),
            Regex("官方工作联系工作联系联系联系热线"),
            Regex("客服工作联系工作联系联系联系QQ"),
            Regex("官方工作联系工作联系联系联系QQ"),
            Regex("客服工作联系工作联系联系联系微信"),
            Regex("官方工作联系工作联系联系联系微信"),
            Regex("客服工作联系工作联系联系联系钉钉"),
            Regex("官方工作联系工作联系联系联系钉钉"),
            Regex("客服工作联系工作联系联系联系企业微信"),
            Regex("官方工作联系工作联系联系联系企业微信"),
            Regex("客服工作联系工作联系联系联系工作号"),
            Regex("官方工作联系工作联系联系联系工作号"),
            Regex("客服工作联系工作联系联系联系工作微信"),
            Regex("官方工作联系工作联系联系联系工作微信"),
            Regex("客服工作联系工作联系联系联系工作QQ"),
            Regex("官方工作联系工作联系联系联系工作QQ"),
            Regex("客服工作联系工作联系联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系联系工作热线"),
            Regex("官方工作联系工作联系联系联系工作热线"),
            Regex("客服工作联系工作联系联系电话"),
            Regex("官方工作联系工作联系电话"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系座机"),
            Regex("官方工作联系工作联系联系座机"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系工作联系联系方式"),
            Regex("官方工作联系工作联系联系方式"),
            Regex("客服工作联系工作联系联系联系"),
            Regex("官方工作联系工作联系联系联系"),
            Regex("客服工作联系工作联系联系联系号码"),
            Regex("官方工作联系工作联系联系联系号码"),
            Regex("客服工作联系工作联系联系联系手机"),
            Regex("官方工作联系工作联系联系联系手机"),
            Regex("客服工作联系工作联系联系电话"),
            Regex("官方工作联系工作联系电话"),
            Regex("客服工作联系工作联系联系热线"),
            Regex("官方工作联系工作联系联系热线"),
            Regex("客服工作联系工作联系联系QQ"),
            Regex("官方工作联系工作联系联系QQ"),
            Regex("客服工作联系工作联系联系微信"),
            Regex("官方工作联系工作联系联系微信"),
            Regex("客服工作联系工作联系联系钉钉"),
            Regex("官方工作联系工作联系联系钉钉"),
            Regex("客服工作联系工作联系联系企业微信"),
            Regex("官方工作联系工作联系联系企业微信"),
            Regex("客服工作联系工作联系联系工作号"),
            Regex("官方工作联系工作联系联系工作号"),
            Regex("客服工作联系工作联系联系工作微信"),
            Regex("官方工作联系工作联系联系工作微信"),
            Regex("客服工作联系工作联系联系工作QQ"),
            Regex("官方工作联系工作联系联系工作QQ"),
            Regex("客服工作联系工作联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系工作热线"),
            Regex("官方工作联系工作联系联系工作热线"),
            Regex("客服工作联系工作联系电话"),
            Regex("官方工作联系电话"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系座机"),
            Regex("官方工作联系工作联系联系座机"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系工作联系联系方式"),
            Regex("官方工作联系工作联系方式"),
            Regex("客服工作联系工作联系联系联系"),
            Regex("官方工作联系工作联系联系联系"),
            Regex("客服工作联系工作联系联系联系号码"),
            Regex("官方工作联系工作联系联系联系号码"),
            Regex("客服工作联系工作联系联系联系手机"),
            Regex("官方工作联系工作联系联系联系手机"),
            Regex("客服工作联系工作联系电话"),
            Regex("官方工作联系电话"),
            Regex("客服工作联系工作联系联系热线"),
            Regex("官方工作联系工作联系联系热线"),
            Regex("客服工作联系工作联系联系QQ"),
            Regex("官方工作联系工作联系联系QQ"),
            Regex("客服工作联系工作联系联系微信"),
            Regex("官方工作联系工作联系联系微信"),
            Regex("客服工作联系工作联系联系钉钉"),
            Regex("官方工作联系工作联系联系钉钉"),
            Regex("客服工作联系工作联系联系企业微信"),
            Regex("官方工作联系工作联系联系企业微信"),
            Regex("客服工作联系工作联系联系工作号"),
            Regex("官方工作联系工作联系联系工作号"),
            Regex("客服工作联系工作联系联系工作微信"),
            Regex("官方工作联系工作联系联系工作微信"),
            Regex("客服工作联系工作联系联系工作QQ"),
            Regex("官方工作联系工作联系联系工作QQ"),
            Regex("客服工作联系工作联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系工作热线"),
            Regex("官方工作联系工作联系联系工作热线"),
            Regex("客服工作联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系座机"),
            Regex("官方工作联系工作联系联系座机"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系工作联系方式"),
            Regex("官方工作联系方式"),
            Regex("客服工作联系工作联系联系联系"),
            Regex("官方工作联系工作联系联系联系"),
            Regex("客服工作联系工作联系联系联系号码"),
            Regex("官方工作联系工作联系联系联系号码"),
            Regex("客服工作联系工作联系联系联系手机"),
            Regex("官方工作联系工作联系联系联系手机"),
            Regex("客服工作联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系热线"),
            Regex("官方工作联系工作联系联系热线"),
            Regex("客服工作联系工作联系联系QQ"),
            Regex("官方工作联系工作联系联系QQ"),
            Regex("客服工作联系工作联系联系微信"),
            Regex("官方工作联系工作联系联系微信"),
            Regex("客服工作联系工作联系联系钉钉"),
            Regex("官方工作联系工作联系联系钉钉"),
            Regex("客服工作联系工作联系联系企业微信"),
            Regex("官方工作联系工作联系联系企业微信"),
            Regex("客服工作联系工作联系联系工作号"),
            Regex("官方工作联系工作联系联系工作号"),
            Regex("客服工作联系工作联系联系工作微信"),
            Regex("官方工作联系工作联系联系工作微信"),
            Regex("客服工作联系工作联系联系工作QQ"),
            Regex("官方工作联系工作联系联系工作QQ"),
            Regex("客服工作联系工作联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系工作热线"),
            Regex("官方工作联系工作联系联系工作热线"),
            Regex("客服联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系座机"),
            Regex("官方工作联系工作联系联系座机"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系方式"),
            Regex("官方工作联系方式"),
            Regex("客服工作联系工作联系联系联系"),
            Regex("官方工作联系工作联系联系联系"),
            Regex("客服工作联系工作联系联系联系号码"),
            Regex("官方工作联系工作联系联系联系号码"),
            Regex("客服工作联系工作联系联系联系手机"),
            Regex("官方工作联系工作联系联系联系手机"),
            Regex("客服联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系热线"),
            Regex("官方工作联系工作联系联系热线"),
            Regex("客服工作联系工作联系联系QQ"),
            Regex("官方工作联系工作联系联系QQ"),
            Regex("客服工作联系工作联系联系微信"),
            Regex("官方工作联系工作联系联系微信"),
            Regex("客服工作联系工作联系联系钉钉"),
            Regex("官方工作联系工作联系联系钉钉"),
            Regex("客服工作联系工作联系联系企业微信"),
            Regex("官方工作联系工作联系联系企业微信"),
            Regex("客服工作联系工作联系联系工作号"),
            Regex("官方工作联系工作联系联系工作号"),
            Regex("客服工作联系工作联系联系工作微信"),
            Regex("官方工作联系工作联系联系工作微信"),
            Regex("客服工作联系工作联系联系工作QQ"),
            Regex("官方工作联系工作联系联系工作QQ"),
            Regex("客服工作联系工作联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系工作热线"),
            Regex("官方工作联系工作联系联系工作热线"),
            Regex("客服联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系座机"),
            Regex("官方工作联系工作联系联系座机"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系方式"),
            Regex("官方工作联系方式"),
            Regex("客服工作联系工作联系联系联系"),
            Regex("官方工作联系工作联系联系联系"),
            Regex("客服工作联系工作联系联系联系号码"),
            Regex("官方工作联系工作联系联系联系号码"),
            Regex("客服工作联系工作联系联系联系手机"),
            Regex("官方工作联系工作联系联系联系手机"),
            Regex("客服联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系热线"),
            Regex("官方工作联系工作联系联系热线"),
            Regex("客服工作联系工作联系联系QQ"),
            Regex("官方工作联系工作联系联系QQ"),
            Regex("客服工作联系工作联系联系微信"),
            Regex("官方工作联系工作联系联系微信"),
            Regex("客服工作联系工作联系联系钉钉"),
            Regex("官方工作联系工作联系联系钉钉"),
            Regex("客服工作联系工作联系联系企业微信"),
            Regex("官方工作联系工作联系联系企业微信"),
            Regex("客服工作联系工作联系联系工作号"),
            Regex("官方工作联系工作联系联系工作号"),
            Regex("客服工作联系工作联系联系工作微信"),
            Regex("官方工作联系工作联系联系工作微信"),
            Regex("客服工作联系工作联系联系工作QQ"),
            Regex("官方工作联系工作联系联系工作QQ"),
            Regex("客服工作联系工作联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系工作热线"),
            Regex("官方工作联系工作联系联系工作热线"),
            Regex("客服联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系手机"),
            Regex("官方工作联系工作联系联系手机"),
            Regex("客服工作联系工作联系联系座机"),
            Regex("官方工作联系工作联系联系座机"),
            Regex("客服工作联系工作联系联系号码"),
            Regex("官方工作联系工作联系联系号码"),
            Regex("客服工作联系方式"),
            Regex("官方工作联系方式"),
            Regex("客服工作联系工作联系联系联系"),
            Regex("官方工作联系工作联系联系联系"),
            Regex("客服工作联系工作联系联系联系号码"),
            Regex("官方工作联系工作联系联系联系号码"),
            Regex("客服工作联系工作联系联系联系手机"),
            Regex("官方工作联系工作联系联系联系手机"),
            Regex("客服联系电话"),
            Regex("官方联系电话"),
            Regex("客服工作联系工作联系联系热线"),
            Regex("官方工作联系工作联系联系热线"),
            Regex("客服工作联系工作联系联系QQ"),
            Regex("官方工作联系工作联系联系QQ"),
            Regex("客服工作联系工作联系联系微信"),
            Regex("官方工作联系工作联系联系微信"),
            Regex("客服工作联系工作联系联系钉钉"),
            Regex("官方工作联系工作联系联系钉钉"),
            Regex("客服工作联系工作联系联系企业微信"),
            Regex("官方工作联系工作联系联系企业微信"),
            Regex("客服工作联系工作联系联系工作号"),
            Regex("官方工作联系工作联系联系工作号"),
            Regex("客服工作联系工作联系联系工作微信"),
            Regex("官方工作联系工作联系联系工作微信"),
            Regex("客服工作联系工作联系联系工作QQ"),
            Regex("官方工作联系工作联系联系工作QQ"),
            Regex("客服工作联系工作联系联系工作钉钉"),
            Regex("官方工作联系工作联系联系工作钉钉"),
            Regex("客服工作联系工作联系联系工作企业微信"),
            Regex("官方工作联系工作联系联系工作企业微信"),
            Regex("客服工作联系工作联系联系工作热线"),
            Regex("官方工作联系工作联系联系工作热线")
        )
        
        return if (suspiciousPatterns.any { it.containsMatchIn(sender) }) {
            RiskLevel.MEDIUM
        } else {
            RiskLevel.NONE
        }
    }
    
    private fun analyzeTimePattern(timestamp: Long): RiskLevel {
        val hour = (timestamp / 3600000) % 24
        
        return if (hour in 0..5 || hour in 23..23) {
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
