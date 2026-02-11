package com.zhengshu.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhengshu.data.model.Evidence
import com.zhengshu.data.model.RiskDetectionResult
import com.zhengshu.data.repository.EvidenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _riskAlertState = MutableStateFlow<RiskAlertState?>(null)
    val riskAlertState: StateFlow<RiskAlertState?> = _riskAlertState.asStateFlow()

    init {
        loadEvidenceList()
    }

    fun loadEvidenceList() {
        viewModelScope.launch {
            
        }
    }

    fun showRiskAlert(result: RiskDetectionResult) {
        _riskAlertState.value = RiskAlertState(
            riskLevel = result.riskLevel,
            riskReason = result.riskReason,
            detectedKeywords = result.detectedKeywords,
            detectedBehaviors = result.detectedBehaviors,
            confidence = result.confidence
        )
    }

    fun dismissRiskAlert() {
        _riskAlertState.value = null
    }

    fun markAsFalsePositive() {
        _riskAlertState.value = null
        
    }

    fun startEvidenceCollection() {
        _uiState.value = _uiState.value.copy(
            isCollectingEvidence = true
        )
    }

    fun stopEvidenceCollection() {
        _uiState.value = _uiState.value.copy(
            isCollectingEvidence = false
        )
    }

    fun selectTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab
        )
    }
}

data class MainUiState(
    val selectedTab: MainTab = MainTab.Home,
    val isCollectingEvidence: Boolean = false,
    val evidenceList: List<Evidence> = emptyList()
)

data class RiskAlertState(
    val riskLevel: com.zhengshu.data.model.RiskLevel,
    val riskReason: String,
    val detectedKeywords: List<String>,
    val detectedBehaviors: List<String>,
    val confidence: Float
)

enum class MainTab(val displayName: String) {
    Home("首页"),
    Evidence("存证"),
    Legal("文书"),
    Judiciary("司法"),
    Hardware("硬件"),
    Settings("设置")
}
