package com.zhengshu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.zhengshu.data.local.AppDatabase
import com.zhengshu.data.repository.EvidenceRepository
import com.zhengshu.services.ai.RiskDetectionService
import com.zhengshu.services.evidence.ScreenRecordService
import com.zhengshu.utils.EncryptionManager

class ZhengShuApplication : Application() {

    companion object {
        private const val CHANNEL_ID_RISK = "risk_detection_channel"
        private const val CHANNEL_ID_EVIDENCE = "evidence_collection_channel"
        private const val CHANNEL_ID_SCREEN_RECORD = "screen_record_channel"
        
        lateinit var instance: ZhengShuApplication
            private set
        
        lateinit var database: AppDatabase
            private set
        
        lateinit var evidenceRepository: EvidenceRepository
            private set
        
        lateinit var encryptionManager: EncryptionManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        initDatabase()
        initRepositories()
        initServices()
        createNotificationChannels()
    }

    private fun initDatabase() {
        database = AppDatabase.getDatabase(this)
    }

    private fun initRepositories() {
        evidenceRepository = EvidenceRepository(database.evidenceDao())
    }

    private fun initServices() {
        encryptionManager = EncryptionManager(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            val riskChannel = NotificationChannel(
                CHANNEL_ID_RISK,
                getString(R.string.risk_detection),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "风险预警通知"
            }
            
            val evidenceChannel = NotificationChannel(
                CHANNEL_ID_EVIDENCE,
                getString(R.string.evidence_management),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "存证管理通知"
            }
            
            val screenRecordChannel = NotificationChannel(
                CHANNEL_ID_SCREEN_RECORD,
                getString(R.string.screen_recording_started),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "录屏服务通知"
            }
            
            notificationManager.createNotificationChannel(riskChannel)
            notificationManager.createNotificationChannel(evidenceChannel)
            notificationManager.createNotificationChannel(screenRecordChannel)
        }
    }
}
