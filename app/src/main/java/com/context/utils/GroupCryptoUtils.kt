package com.context.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object GroupCryptoUtils {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    /**
     * Generates a random 256-bit AES key and returns it as a Base64 string.
     */
    fun generateSecretKey(): String {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(256)
        val secretKey = keyGen.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Encrypts data using AES-GCM.
     * Returns: "IV:EncryptedData" encoded in Base64
     */
    fun encrypt(data: String, secretKeyStr: String): String {
        // Sanitize the key string (handle URL decoding issues where + became space)
        val sanitizedKey = secretKeyStr.replace(" ", "+")
        val secretKey = SecretKeySpec(Base64.decode(sanitizedKey, Base64.NO_WRAP), ALGORITHM)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        
        val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        
        return "$ivString:$encryptedString"
    }

    /**
     * Decrypts data using AES-GCM.
     */
    fun decrypt(encryptedPayload: String, secretKeyStr: String): String {
        val parts = encryptedPayload.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Invalid encrypted payload")
        
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encryptedData = Base64.decode(parts[1], Base64.NO_WRAP)
        
        // Sanitize the key string
        val sanitizedKey = secretKeyStr.replace(" ", "+")
        val secretKey = SecretKeySpec(Base64.decode(sanitizedKey, Base64.NO_WRAP), ALGORITHM)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        val decryptedBytes = cipher.doFinal(encryptedData)

        return String(decryptedBytes)
    }
}
