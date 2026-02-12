package com.zhengshu.services.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zhengshu.R
import com.zhengshu.services.evidence.EvidenceCollectionService
import com.zhengshu.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RiskDetectionService : Service() {

    companion object {
        private const val CHANNEL_ID = "risk_detection_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_DETECTION = "com.zhengshu.START_DETECTION"
        const val ACTION_STOP_DETECTION = "com.zhengshu.STOP_DETECTION"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false
    
    private lateinit var riskDetectionEngine: RiskDetectionEngine
    private lateinit var dataCollector: DataCollector
    
    override fun onCreate() {
        super.onCreate()
        riskDetectionEngine = RiskDetectionEngine(this)
        dataCollector = DataCollector(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DETECTION -> startDetection()
            ACTION_STOP_DETECTION -> stopDetection()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startDetection() {
        if (isRunning) return
        
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            dataCollector.startCollection()
            
            dataCollector.textContentFlow.collectLatest { text ->
                val result = riskDetectionEngine.analyzeText(text)
                if (result.riskLevel != com.zhengshu.data.model.RiskLevel.NONE) {
                    handleRiskDetection(result)
                }
            }
            
            dataCollector.chatMessageFlow.collectLatest { message ->
                val result = riskDetectionEngine.analyzeChatMessage(message)
                if (result.riskLevel != com.zhengshu.data.model.RiskLevel.NONE) {
                    handleRiskDetection(result)
                }
            }
            
            dataCollector.behaviorDataFlow.collectLatest { behavior ->
                val result = riskDetectionEngine.analyzeBehavior(behavior)
                if (result.riskLevel != com.zhengshu.data.model.RiskLevel.NONE) {
                    handleRiskDetection(result)
                }
            }
        }
    }

    private fun stopDetection() {
        if (!isRunning) return
        
        isRunning = false
        dataCollector.stopCollection()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleRiskDetection(result: com.zhengshu.data.model.RiskDetectionResult) {
        when (result.riskLevel) {
            com.zhengshu.data.model.RiskLevel.HIGH -> {
                showHighRiskAlert(result)
                startEvidenceCollection(result)
            }
            com.zhengshu.data.model.RiskLevel.MEDIUM -> {
                showMediumRiskAlert(result)
            }
            com.zhengshu.data.model.RiskLevel.LOW -> {
                logLowRisk(result)
            }
            else -> {}
        }
    }

    private fun showHighRiskAlert(result: com.zhengshu.data.model.RiskDetectionResult) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("risk_level", "HIGH")
            putExtra("risk_reason", result.riskReason)
        }
        startActivity(intent)
    }

    private fun showMediumRiskAlert(result: com.zhengshu.data.model.RiskDetectionResult) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.risk_warning_title))
            .setContentText(result.riskReason)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1002, notification)
    }

    private fun logLowRisk(result: com.zhengshu.data.model.RiskDetectionResult) {
        
    }

    private fun startEvidenceCollection(result: com.zhengshu.data.model.RiskDetectionResult) {
        val intent = Intent(this, com.zhengshu.services.evidence.EvidenceCollectionService::class.java).apply {
            action = com.zhengshu.services.evidence.EvidenceCollectionService.ACTION_START_COLLECTION
            putExtra("risk_result", result)
        }
        startService(intent)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("风险识别服务运行中")
            .setContentText("正在监控可能的诈骗风险")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "风险识别",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "风险预警通知"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (isRunning) {
            stopDetection()
        }
    }
}
