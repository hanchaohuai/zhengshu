package com.zhengshu.services.evidence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zhengshu.R
import com.zhengshu.data.model.ChatMessage
import com.zhengshu.data.model.EvidencePackage
import com.zhengshu.data.model.EnvironmentData
import com.zhengshu.data.model.RiskDetectionResult
import com.zhengshu.utils.EncryptionManager
import com.zhengshu.utils.HashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EvidenceCollectionService : Service() {

    companion object {
        private const val CHANNEL_ID = "evidence_collection_channel"
        private const val NOTIFICATION_ID = 3001
        
        const val ACTION_START_COLLECTION = "com.zhengshu.START_COLLECTION"
        const val ACTION_STOP_COLLECTION = "com.zhengshu.STOP_COLLECTION"
        const val EXTRA_RISK_RESULT = "risk_result"
        
        private const val COLLECTION_DURATION = 30000L
        private const val SCREENSHOT_INTERVAL = 5000L
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private var isCollecting = false
    private var collectionStartTime = 0L
    
    private val _collectionProgressFlow = MutableSharedFlow<CollectionProgress>()
    val collectionProgressFlow = _collectionProgressFlow.asSharedFlow()
    
    private val _evidencePackageFlow = MutableSharedFlow<EvidencePackage>()
    val evidencePackageFlow = _evidencePackageFlow.asSharedFlow()
    
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var screenRecordService: ScreenRecordService
    private lateinit var chatMessageExtractor: ChatMessageExtractor
    
    private val screenshotPaths = mutableListOf<String>()
    private val chatMessages = mutableListOf<ChatMessage>()
    private var screenRecordPath: String? = null
    private var riskResult: RiskDetectionResult? = null

    override fun onCreate() {
        super.onCreate()
        encryptionManager = EncryptionManager(this)
        chatMessageExtractor = ChatMessageExtractor(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_COLLECTION -> {
                riskResult = intent.getSerializableExtra(EXTRA_RISK_RESULT) as? RiskDetectionResult
                startCollection()
            }
            ACTION_STOP_COLLECTION -> stopCollection()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCollection() {
        if (isCollecting) return
        
        isCollecting = true
        collectionStartTime = System.currentTimeMillis()
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            _collectionProgressFlow.emit(CollectionProgress.Started)
            
            collectEvidence()
        }
    }

    private fun stopCollection() {
        if (!isCollecting) return
        
        isCollecting = false
        
        serviceScope.launch {
            finalizeEvidencePackage()
            _collectionProgressFlow.emit(CollectionProgress.Completed)
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun collectEvidence() {
        val elapsed = System.currentTimeMillis() - collectionStartTime
        
        while (isCollecting && elapsed < COLLECTION_DURATION) {
            collectScreenshot()
            collectChatMessages()
            
            _collectionProgressFlow.emit(
                CollectionProgress.Progress(
                    elapsedTime = System.currentTimeMillis() - collectionStartTime,
                    screenshotCount = screenshotPaths.size,
                    messageCount = chatMessages.size
                )
            )
            
            delay(SCREENSHOT_INTERVAL)
        }
        
        if (isCollecting) {
            stopCollection()
        }
    }

    private suspend fun collectScreenshot() {
        
    }

    private suspend fun collectChatMessages() {
        val recentMessages = chatMessageExtractor.extractSmsMessages(50)
        chatMessages.addAll(recentMessages)
    }

    private suspend fun finalizeEvidencePackage() {
        val environmentData = collectEnvironmentData()
        
        val evidencePackage = EvidencePackage(
            id = generateEvidenceId(),
            title = generateTitle(),
            description = riskResult?.riskReason ?: "自动存证",
            screenRecordPath = screenRecordPath,
            screenshotPaths = screenshotPaths.toList(),
            chatMessages = chatMessages.toList(),
            environmentData = environmentData,
            timestamp = System.currentTimeMillis(),
            hashValue = "",
            blockchainAddress = null
        )
        
        val hashValue = HashUtils.calculateHash(evidencePackage)
        val encryptedPackage = encryptionManager.encryptEvidence(evidencePackage)
        
        val finalPackage = evidencePackage.copy(
            hashValue = hashValue
        )
        
        val filePath = saveEvidencePackage(finalPackage)
        
        _evidencePackageFlow.emit(finalPackage)
    }

    private fun collectEnvironmentData(): EnvironmentData {
        val packageManager = packageManager
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        
        return EnvironmentData(
            deviceModel = android.os.Build.MODEL,
            osVersion = "Android ${android.os.Build.VERSION.RELEASE}",
            appVersion = packageInfo.versionName,
            networkType = getNetworkType(),
            ipAddress = null,
            location = null
        )
    }

    private fun getNetworkType(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        
        return when (networkInfo?.type) {
            android.net.ConnectivityManager.TYPE_WIFI -> "WiFi"
            android.net.ConnectivityManager.TYPE_MOBILE -> "Mobile"
            else -> "Unknown"
        }
    }

    private fun generateEvidenceId(): String {
        return "evidence_${System.currentTimeMillis()}_${(0..9999).random()}"
    }

    private fun generateTitle(): String {
        val riskLevel = riskResult?.riskLevel?.name ?: "UNKNOWN"
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return "证据包 - $riskLevel - $date"
    }

    private suspend fun saveEvidencePackage(evidencePackage: EvidencePackage): String {
        val fileName = "${evidencePackage.id}.proof"
        val directory = File(getExternalFilesDir(null), "evidence")
        
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        val file = File(directory, fileName)
        
        return file.absolutePath
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, com.zhengshu.ui.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = Intent(this, EvidenceCollectionService::class.java).apply {
            action = ACTION_STOP_COLLECTION
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("存证采集中")
            .setContentText("正在收集证据...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "存证采集",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "存证采集通知"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (isCollecting) {
            stopCollection()
        }
    }
}

sealed class CollectionProgress {
    object Started : CollectionProgress()
    data class Progress(
        val elapsedTime: Long,
        val screenshotCount: Int,
        val messageCount: Int
    ) : CollectionProgress()
    object Completed : CollectionProgress()
}
