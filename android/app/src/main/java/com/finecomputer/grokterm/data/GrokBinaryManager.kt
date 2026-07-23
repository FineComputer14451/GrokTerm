package com.finecomputer.grokterm.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages the Grok Build static binary inside the app's private storage.
 * Handles download via GrokBinaryDownloader, DNS path patching, and readiness.
 */
class GrokBinaryManager(private val context: Context) {

    private val tag = "GrokBinaryManager"
    private val filesDir: File = context.filesDir
    private val binaryDir = File(filesDir, "grok/bin").also { it.mkdirs() }
    private val binaryFile = File(binaryDir, "grok")
    private val dnsFile = File(context.getExternalFilesDir(null), ".grokdns")

    private val downloader = GrokBinaryDownloader(context)

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
     * Apply the 16-byte DNS path patch.
     * /etc/resolv.conf → /sdcard/.grokdns  (exactly 16 bytes)
     */
    suspend fun applyDnsPatch(): Boolean = withContext(Dispatchers.IO) {
        if (!binaryFile.exists()) return@withContext false

        val original = "/etc/resolv.conf".toByteArray(Charsets.US_ASCII)
        val replacement = "/sdcard/.grokdns".toByteArray(Charsets.US_ASCII)

        if (original.size != 16 || replacement.size != 16) {
            Log.e(tag, "Patch strings must be exactly 16 bytes")
            return@withContext false
        }

        val data = binaryFile.readBytes()
        val idx = data.indexOf(original)
        if (idx < 0) {
            val already = data.indexOf(replacement) >= 0
            Log.i(tag, if (already) "Already patched" else "String not found — upstream may have changed")
            return@withContext already
        }

        val bak = File(binaryFile.absolutePath + ".orig")
        if (!bak.exists()) binaryFile.copyTo(bak, overwrite = false)

        System.arraycopy(replacement, 0, data, idx, 16)
        binaryFile.writeBytes(data)
        binaryFile.setExecutable(true, false)
        Log.i(tag, "DNS patch applied at offset $idx")
        true
    }

    /**
     * Download (or re-download) the latest stable aarch64 binary and patch it.
     */
    suspend fun downloadOrUpdate(
        onProgress: (GrokBinaryDownloader.Progress) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        when (val result = downloader.downloadLatest(onProgress)) {
            is GrokBinaryDownloader.Result.Success -> {
                Result.success("Grok ${result.version} ready at ${result.path}")
            }
            is GrokBinaryDownloader.Result.Failure -> {
                Result.failure(Exception(result.message, result.cause))
            }
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
