package com.zhengshu.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionManager(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "zhengshu_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SHARED_PREFS_NAME = "zhengshu_encrypted_prefs"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            .build()
    }

    private val encryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SHARED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun encryptString(plaintext: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val combined = iv + encryptedBytes
            return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt string", e)
        }
    }

    fun decryptString(ciphertext: String): String {
        try {
            val combined = android.util.Base64.decode(ciphertext, android.util.Base64.NO_WRAP)

            val iv = combined.sliceArray(0..11)
            val encryptedBytes = combined.sliceArray(12 until combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw EncryptionException("Failed to decrypt string", e)
        }
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        try {
            val encryptedFile = EncryptedFile.Builder(
                outputFile,
                context,
                KEY_ALIAS,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val inputStream = inputFile.inputStream()
            val outputStream = encryptedFile.openFileOutput()

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt file", e)
        }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        try {
            val encryptedFile = EncryptedFile.Builder(
                inputFile,
                context,
                KEY_ALIAS,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val inputStream = encryptedFile.openFileInput()
            val outputStream = outputFile.outputStream()

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            throw EncryptionException("Failed to decrypt file", e)
        }
    }

    fun saveEncrypted(key: String, value: String) {
        encryptedSharedPreferences.edit().putString(key, value).apply()
    }

    fun getDecrypted(key: String): String? {
        return encryptedSharedPreferences.getString(key, null)
    }

    fun removeEncrypted(key: String) {
        encryptedSharedPreferences.edit().remove(key).apply()
    }

    fun clearAllEncrypted() {
        encryptedSharedPreferences.edit().clear().apply()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (key != null) {
            return key
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return keyGenerator.generateKey()
    }

    class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
}