package com.zhengshu.services.evidence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
        private const val NOTIFICATION_ID = 1002
        
        const val ACTION_START_COLLECTION = "com.zhengshu.START_COLLECTION"
        const val ACTION_STOP_COLLECTION = "com.zhengshu.STOP_COLLECTION"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false
    
    private lateinit var encryptionManager: EncryptionManager
    
    override fun onCreate() {
        super.onCreate()
        encryptionManager = EncryptionManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_COLLECTION -> startCollection()
            ACTION_STOP_COLLECTION -> stopCollection()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCollection() {
        if (isRunning) return
        
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            while (isRunning) {
                delay(5000)
            }
        }
    }

    private fun stopCollection() {
        if (!isRunning) return
        
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("证据收集服务运行中")
            .setContentText("正在收集相关证据")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        
        return notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "证据收集",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "证据收集通知"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (isRunning) {
            stopCollection()
        }
    }
}