package com.zhengshu.services.evidence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.zhengshu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    companion object {
        private const val CHANNEL_ID = "screen_record_channel"
        private const val NOTIFICATION_ID = 2001
        
        const val ACTION_START_RECORDING = "com.zhengshu.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.zhengshu.STOP_RECORDING"
        const val ACTION_TAKE_SCREENSHOT = "com.zhengshu.TAKE_SCREENSHOT"
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA_INTENT = "data_intent"
        
        private const val SCREEN_WIDTH = 1080
        private const val SCREEN_HEIGHT = 1920
        private const val SCREEN_DPI = 320
        private const val BITMAP_QUALITY = 100
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var mediaRecorder: MediaRecorder? = null
    
    private var isRecording = false
    private var isTakingScreenshot = false
    
    private val _screenshotFlow = MutableSharedFlow<String>()
    val screenshotFlow = _screenshotFlow.asSharedFlow()
    
    private val _recordingStatusFlow = MutableSharedFlow<RecordingStatus>()
    val recordingStatusFlow = _recordingStatusFlow.asSharedFlow()
    
    private var currentVideoPath: String? = null
    private var screenshotCount = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA_INTENT)
                if (resultCode != -1 && data != null) {
                    startRecording(resultCode, data)
                }
            }
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_TAKE_SCREENSHOT -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA_INTENT)
                if (resultCode != -1 && data != null) {
                    takeScreenshot(resultCode, data)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording(resultCode: Int, data: Intent) {
        if (isRecording) return
        
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        initMediaRecorder()
        
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecord",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
        
        try {
            mediaRecorder?.start()
            isRecording = true
            currentVideoPath = getVideoFilePath()
            
            startForeground(NOTIFICATION_ID, createRecordingNotification())
            
            serviceScope.launch {
                _recordingStatusFlow.emit(RecordingStatus.Recording)
            }
            
            captureVideoFrames()
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        isRecording = false
        
        serviceScope.launch {
            _recordingStatusFlow.emit(RecordingStatus.Stopped(currentVideoPath))
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun takeScreenshot(resultCode: Int, data: Intent) {
        if (isTakingScreenshot) return
        
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi
        
        val tempImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        val tempVirtualDisplay = projection.createVirtualDisplay(
            "Screenshot",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            tempImageReader.surface,
            null,
            null
        )
        
        isTakingScreenshot = true
        
        serviceScope.launch {
            delay(100)
            
            try {
                val image = tempImageReader.acquireLatestImage()
                image?.let {
                    val bitmap = imageToBitmap(it)
                    val path = saveBitmap(bitmap)
                    _screenshotFlow.emit(path)
                    it.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                tempVirtualDisplay.release()
                tempImageReader.close()
                projection.stop()
                isTakingScreenshot = false
            }
        }
    }

    private fun captureVideoFrames() {
        serviceScope.launch {
            while (isRecording) {
                try {
                    val image = imageReader?.acquireLatestImage()
                    image?.let {
                        it.close()
                    }
                    delay(33)
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun initMediaRecorder() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
        
        val path = getVideoFilePath()
        
        mediaRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(path)
            setVideoSize(SCREEN_WIDTH, SCREEN_HEIGHT)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(8 * 1024 * 1024)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            
            try {
                prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        
        bitmap.copyPixelsFromBuffer(buffer)
        
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun saveBitmap(bitmap: Bitmap): String {
        val path = getScreenshotFilePath()
        val file = File(path)
        val fos = FileOutputStream(file)
        
        bitmap.compress(Bitmap.CompressFormat.PNG, BITMAP_QUALITY, fos)
        fos.flush()
        fos.close()
        
        return path
    }

    private fun getVideoFilePath(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "VIDEO_$timeStamp.mp4"
        
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "evidence")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        return File(directory, fileName).absolutePath
    }

    private fun getScreenshotFilePath(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SCREENSHOT_$timeStamp.png"
        
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "evidence")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        return File(directory, fileName).absolutePath
    }

    private fun createRecordingNotification(): Notification {
        val intent = Intent(this, com.zhengshu.ui.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("录屏进行中")
            .setContentText("正在录制屏幕...")
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
                "录屏服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "录屏服务通知"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (isRecording) {
            stopRecording()
        }
    }
}

sealed class RecordingStatus {
    object Idle : RecordingStatus()
    object Recording : RecordingStatus()
    data class Stopped(val videoPath: String?) : RecordingStatus()
}
