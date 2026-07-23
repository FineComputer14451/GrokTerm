package com.finecomputer.grokterm.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Manages the Grok Build static binary inside the app's private storage.
 * Handles download (when URL known), DNS path patching, and execution readiness.
 *
 * Note: Official binary download URL is obtained via the install script.
 * For production, prefer shipping a known-good binary or using the
 * Termux Phase 1 path + inter-process launch, or implement a robust
 * fetcher that follows the official install.sh logic.
 */
class GrokBinaryManager(private val context: Context) {

    private val tag = "GrokBinaryManager"
    private val filesDir: File = context.filesDir
    private val binaryDir = File(filesDir, "grok/bin").also { it.mkdirs() }
    private val binaryFile = File(binaryDir, "grok")
    private val dnsFile = File(context.getExternalFilesDir(null), ".grokdns")

    val isReady: Boolean
        get() = binaryFile.exists() && binaryFile.canExecute()

    val binaryPath: String
        get() = binaryFile.absolutePath

    suspend fun ensureDnsFile() = withContext(Dispatchers.IO) {
        if (!dnsFile.exists()) {
            dnsFile.parentFile?.mkdirs()
            dnsFile.writeText(
                """
                nameserver 8.8.8.8
                nameserver 8.8.4.4
                nameserver 1.1.1.1
                """.trimIndent()
            )
        }
    }

    /**
     * Apply the 16-byte DNS path patch used by the Termux native approach.
     * /etc/resolv.conf  →  a writable path of exactly the same length.
     */
    suspend fun applyDnsPatch(): Boolean = withContext(Dispatchers.IO) {
        if (!binaryFile.exists()) return@withContext false

        val original = "/etc/resolv.conf".toByteArray(Charsets.US_ASCII)
        // Must be exactly 16 bytes. Adjust path if needed to keep length.
        val replacement = "/sdcard/.grokdns".toByteArray(Charsets.US_ASCII)

        if (original.size != 16 || replacement.size != 16) {
            Log.e(tag, "Patch strings must be exactly 16 bytes")
            return@withContext false
        }

        val data = binaryFile.readBytes()
        val idx = data.indexOf(original)
        if (idx < 0) {
            // Already patched or binary changed
            val already = data.indexOf(replacement) >= 0
            Log.i(tag, if (already) "Already patched" else "String not found — upstream binary may have changed")
            return@withContext already
        }

        // Backup
        val bak = File(binaryFile.absolutePath + ".orig")
        if (!bak.exists()) binaryFile.copyTo(bak, overwrite = false)

        System.arraycopy(replacement, 0, data, idx, 16)
        binaryFile.writeBytes(data)
        binaryFile.setExecutable(true, false)
        Log.i(tag, "DNS patch applied at offset $idx")
        true
    }

    /**
     * Placeholder for future official binary download.
     * Currently expects the user to place a binary or use Phase 1 Termux install
     * and share via storage, or implement full fetch logic mirroring install.sh.
     */
    suspend fun downloadOrUpdate(): Result<Unit> = withContext(Dispatchers.IO) {
        // TODO: Implement robust download following https://x.ai/cli/install.sh logic
        // For now we only ensure the directory and DNS file exist.
        ensureDnsFile()
        if (isReady) {
            applyDnsPatch()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(
                "Grok binary not present. Use Phase 1 Termux installer or place aarch64 musl binary at ${binaryFile.absolutePath}"
            ))
        }
    }

    fun getLaunchCommand(extraArgs: List<String> = emptyList()): List<String> {
        return listOf(binaryPath) + extraArgs
    }
}

private fun ByteArray.indexOf(sequence: ByteArray): Int {
    if (sequence.isEmpty()) return 0
    outer@ for (i in 0..size - sequence.size) {
        for (j in sequence.indices) {
            if (this[i + j] != sequence[j]) continue@outer
        }
        return i
    }
    return -1
}
