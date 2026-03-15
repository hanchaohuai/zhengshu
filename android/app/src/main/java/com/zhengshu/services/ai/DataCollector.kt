package com.zhengshu.services.ai

import android.content.Context
import com.zhengshu.data.model.BehaviorData
import com.zhengshu.data.model.ChatMessage
import com.zhengshu.data.model.RiskDetectionResult
import com.zhengshu.data.model.RiskLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DataCollector(private val context: Context) {
    
    private val _textContentFlow = MutableSharedFlow<String>()
    val textContentFlow = _textContentFlow.asSharedFlow()
    
    private val _chatMessageFlow = MutableSharedFlow<ChatMessage>()
    val chatMessageFlow = _chatMessageFlow.asSharedFlow()
    
    private val _behaviorDataFlow = MutableSharedFlow<BehaviorData>()
    val behaviorDataFlow = _behaviorDataFlow.asSharedFlow()
    
    private var isCollecting = false
    
    fun startCollection() {
        if (isCollecting) return
        isCollecting = true
    }
    
    fun stopCollection() {
        isCollecting = false
    }
    
    suspend fun collectText(text: String) {
        if (isCollecting) {
            _textContentFlow.emit(text)
        }
    }
    
    suspend fun collectChatMessage(message: ChatMessage) {
        if (isCollecting) {
            _chatMessageFlow.emit(message)
        }
    }
    
    suspend fun collectBehavior(behavior: BehaviorData) {
        if (isCollecting) {
            _behaviorDataFlow.emit(behavior)
        }
    }
}
