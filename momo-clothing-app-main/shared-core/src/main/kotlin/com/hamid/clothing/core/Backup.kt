package com.hamid.clothing.core

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongPasswordException : Exception("wrong password or damaged backup")

/**
 * Password-protected streaming encryption for backup files.
 * Key: PBKDF2 from the password. Data: AES-256-GCM in 64 KB chunks, each with its own nonce;
 * a "last chunk" flag is authenticated so cutting the file short is detected.
 *
 * File layout: "CSBK1", kdf id (1 byte), iterations (int), salt (16), nonce base (8),
 * then chunks of [flag byte][length int][ciphertext + tag].
 */
object BackupCrypto {
    private const val MAGIC = "CSBK1"
    private const val CHUNK = 64 * 1024
    private const val SALT_SIZE = 16
    private const val NONCE_BASE_SIZE = 8
    private const val TAG_BITS = 128
    private const val KDF_SHA256 = 1
    private const val KDF_SHA1 = 2
    const val DEFAULT_ITERATIONS = 200_000

    private fun kdfName(id: Int): String = when (id) {
        KDF_SHA256 -> "PBKDF2WithHmacSHA256"
        KDF_SHA1 -> "PBKDF2WithHmacSHA1"
        else -> throw IOException("unknown key derivation")
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int, kdfId: Int): SecretKey {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance(kdfName(kdfId)).generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun pickKdf(): Int =
        try {
            SecretKeyFactory.getInstance(kdfName(KDF_SHA256))
            KDF_SHA256
        } catch (e: GeneralSecurityException) {
            KDF_SHA1
        }

    private fun iv(nonceBase: ByteArray, counter: Int): ByteArray {
        val iv = ByteArray(12)
        System.arraycopy(nonceBase, 0, iv, 0, NONCE_BASE_SIZE)
        iv[8] = (counter ushr 24).toByte()
        iv[9] = (counter ushr 16).toByte()
        iv[10] = (counter ushr 8).toByte()
        iv[11] = counter.toByte()
        return iv
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = input.read(buffer, total, buffer.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    fun encrypt(input: InputStream, output: OutputStream, password: CharArray, iterations: Int = DEFAULT_ITERATIONS) {
        require(password.isNotEmpty()) { "password must not be empty" }
        require(iterations in 1_000..2_000_000) { "bad iteration count" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also { random.nextBytes(it) }
        val nonceBase = ByteArray(NONCE_BASE_SIZE).also { random.nextBytes(it) }
        val kdfId = pickKdf()
        val key = deriveKey(password, salt, iterations, kdfId)

        val out = DataOutputStream(output)
        out.write(MAGIC.toByteArray(Charsets.US_ASCII))
        out.writeByte(kdfId)
        out.writeInt(iterations)
        out.write(salt)
        out.write(nonceBase)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        var current = ByteArray(CHUNK)
        var next = ByteArray(CHUNK)
        var currentLen = readFully(input, current)
        var counter = 0
        while (true) {
            val nextLen = if (currentLen == CHUNK) readFully(input, next) else 0
            val last = nextLen == 0
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv(nonceBase, counter)))
            val flag: Byte = if (last) 1 else 0
            cipher.updateAAD(byteArrayOf(flag))
            val encrypted = cipher.doFinal(current, 0, currentLen)
            out.writeByte(flag.toInt())
            out.writeInt(encrypted.size)
            out.write(encrypted)
            if (last) break
            counter++
            val tmp = current
            current = next
            next = tmp
            currentLen = nextLen
        }
        out.flush()
    }

    /** @throws WrongPasswordException when the password is wrong or the file was changed. */
    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val din = DataInputStream(input)
        try {
            val magic = ByteArray(MAGIC.length)
            din.readFully(magic)
            if (String(magic, Charsets.US_ASCII) != MAGIC) throw IOException("not a backup file")
            val kdfId = din.readUnsignedByte()
            val iterations = din.readInt()
            if (iterations !in 1_000..2_000_000) throw IOException("bad backup header")
            val salt = ByteArray(SALT_SIZE)
            din.readFully(salt)
            val nonceBase = ByteArray(NONCE_BASE_SIZE)
            din.readFully(nonceBase)
            val key = deriveKey(password, salt, iterations, kdfId)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            var counter = 0
            while (true) {
                val flag = din.readUnsignedByte()
                val length = din.readInt()
                if (flag > 1 || length < 16 || length > CHUNK + 16) throw IOException("damaged backup")
                val encrypted = ByteArray(length)
                din.readFully(encrypted)
                val plain = try {
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv(nonceBase, counter)))
                    cipher.updateAAD(byteArrayOf(flag.toByte()))
                    cipher.doFinal(encrypted)
                } catch (e: GeneralSecurityException) {
                    throw WrongPasswordException()
                }
                output.write(plain)
                if (flag == 1) break
                counter++
            }
            if (din.read() != -1) throw IOException("damaged backup")
            output.flush()
        } catch (e: EOFException) {
            throw IOException("backup file is incomplete")
        }
    }
}

data class BackupData(val snapshot: StoreSnapshot, val createdAt: Long)

/** Backup contents: data.json, backup.json and images/<name> inside a zip (which is then encrypted). */
object BackupPackage {
    const val DATA_ENTRY = "data.json"
    const val META_ENTRY = "backup.json"
    const val IMAGE_DIR = "images/"

    /** Returns how many images were stored. Images are loaded one by one. */
    fun write(out: OutputStream, snapshot: StoreSnapshot, createdAt: Long, loadImage: (String) -> ByteArray?): Int {
        val zip = ZipOutputStream(out)
        zip.putNextEntry(ZipEntry(META_ENTRY))
        zip.write("""{"app":"clothing-store-admin","createdAt":$createdAt}""".toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.putNextEntry(ZipEntry(DATA_ENTRY))
        zip.write(DataCodec.encode(snapshot).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        var count = 0
        for (name in snapshot.products.flatMap { it.imageUris }.distinct()) {
            if (!CatalogPackage.isSafeImageName(name)) continue
            val bytes = loadImage(name) ?: continue
            zip.putNextEntry(ZipEntry(IMAGE_DIR + name))
            zip.write(bytes)
            zip.closeEntry()
            count++
        }
        zip.finish()
        return count
    }

    fun read(input: InputStream, onImage: (String, ByteArray) -> Unit): BackupData {
        val zip = ZipInputStream(input)
        var snapshot: StoreSnapshot? = null
        var createdAt = 0L
        var total = 0L
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) continue
            val bytes = ZipLimits.readLimited(zip)
            total += bytes.size
            require(total <= CatalogPackage.MAX_TOTAL_BYTES) { "backup is too large" }
            val name = entry.name
            if (name == DATA_ENTRY) {
                snapshot = DataCodec.decode(String(bytes, Charsets.UTF_8))
            } else if (name == META_ENTRY) {
                val text = String(bytes, Charsets.UTF_8)
                val m = Regex("\"createdAt\"\\s*:\\s*(\\d+)").find(text)
                createdAt = m?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            } else if (name.startsWith(IMAGE_DIR)) {
                val file = name.substring(IMAGE_DIR.length)
                if (CatalogPackage.isSafeImageName(file)) onImage(file, bytes)
            }
        }
        val data = snapshot ?: throw IllegalArgumentException("data.json not found")
        return BackupData(data, createdAt)
    }

    /** Convenience for tests and small data: whole round trip through encryption. */
    fun encryptBytes(plain: ByteArray, password: CharArray, iterations: Int = BackupCrypto.DEFAULT_ITERATIONS): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(plain), out, password, iterations)
        return out.toByteArray()
    }
}
