package com.hamid.clothing.admin

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * One file encrypted with AES-256-GCM. The key lives in the Android Keystore and never leaves it.
 * File layout: 12-byte IV followed by the ciphertext. Writes go through a temp file so a crash
 * cannot leave a half-written data file.
 */
class SecureFile(private val file: File, private val alias: String = "admin_data_key") {

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getKey(alias, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Returns null when the file does not exist yet. Throws when it cannot be decrypted. */
    fun read(): String? {
        if (!file.exists()) return null
        val all = file.readBytes()
        require(all.size > IV_SIZE) { "data file is too short" }
        val iv = all.copyOfRange(0, IV_SIZE)
        val data = all.copyOfRange(IV_SIZE, all.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }

    fun write(text: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        val out = cipher.iv + encrypted
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(out)
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "could not replace data file" }
        }
    }

    /** Moves an unreadable file aside instead of overwriting it, so nothing is lost silently. */
    fun quarantine() {
        if (file.exists()) {
            file.renameTo(File(file.parentFile, file.name + ".corrupt-" + System.currentTimeMillis()))
        }
    }

    private companion object {
        const val IV_SIZE = 12
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
