package com.zhengshu.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhengshu.data.model.ChatMessage
import com.zhengshu.data.model.RiskDetectionResult
import com.zhengshu.data.model.RiskLevel
import com.zhengshu.services.ai.RiskDetectionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatRiskUiState(
    val messages: List<ChatMessage> = emptyList(),
    val riskLevels: Map<String, RiskLevel> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMessage: ChatMessage? = null
)

class ChatRiskViewModel(application: Application) : AndroidViewModel(application) {
    
    private val riskDetectionEngine = RiskDetectionEngine(application)
    
    private val _uiState = MutableStateFlow(ChatRiskUiState())
    val uiState: StateFlow<ChatRiskUiState> = _uiState.asStateFlow()
    
    private val _showDetailDialog = MutableStateFlow<ChatMessage?>(null)
    val showDetailDialog: StateFlow<ChatMessage?> = _showDetailDialog.asStateFlow()
    
    fun addMessage(message: ChatMessage) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = riskDetectionEngine.analyzeChatMessage(message)
                
                val updatedMessages = _uiState.value.messages + message
                val updatedRiskLevels = _uiState.value.riskLevels.toMutableMap().apply {
                    this[message.id] = result.riskLevel
                }
                
                _uiState.value = ChatRiskUiState(
                    messages = updatedMessages,
                    riskLevels = updatedRiskLevels,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun addMessages(messages: List<ChatMessage>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val updatedMessages = _uiState.value.messages + messages
                val updatedRiskLevels = _uiState.value.riskLevels.toMutableMap()
                
                messages.forEach { message ->
                    val result = riskDetectionEngine.analyzeChatMessage(message)
                    updatedRiskLevels[message.id] = result.riskLevel
                }
                
                _uiState.value = ChatRiskUiState(
                    messages = updatedMessages,
                    riskLevels = updatedRiskLevels,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun selectMessage(message: ChatMessage) {
        _uiState.value = _uiState.value.copy(selectedMessage = message)
        _showDetailDialog.value = message
    }
    
    fun dismissDetailDialog() {
        _showDetailDialog.value = null
        _uiState.value = _uiState.value.copy(selectedMessage = null)
    }
    
    fun clearMessages() {
        _uiState.value = ChatRiskUiState()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun getRiskLevel(messageId: String): RiskLevel {
        return _uiState.value.riskLevels[messageId] ?: RiskLevel.NONE
    }
    
    fun getHighRiskMessages(): List<ChatMessage> {
        return _uiState.value.messages.filter { 
            _uiState.value.riskLevels[it.id] == RiskLevel.HIGH 
        }
    }
    
    fun getMediumRiskMessages(): List<ChatMessage> {
        return _uiState.value.messages.filter { 
            _uiState.value.riskLevels[it.id] == RiskLevel.MEDIUM 
        }
    }
    
    fun getLowRiskMessages(): List<ChatMessage> {
        return _uiState.value.messages.filter { 
            _uiState.value.riskLevels[it.id] == RiskLevel.LOW 
        }
    }
    
    fun getSafeMessages(): List<ChatMessage> {
        return _uiState.value.messages.filter { 
            _uiState.value.riskLevels[it.id] == RiskLevel.NONE 
        }
    }
    
    suspend fun analyzeMessage(message: ChatMessage): RiskDetectionResult {
        return riskDetectionEngine.analyzeChatMessage(message)
    }
}
