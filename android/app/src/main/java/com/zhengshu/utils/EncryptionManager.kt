package com.zhengshu.utils

import android.content.Context
import android.os.IBinder
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zhengshu.data.model.EvidencePackage
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EncryptionManager(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "zhengshu_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SHARED_PREFS_NAME = "zhengshu_encrypted_prefs"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val masterKey: MasterKey by lazy {