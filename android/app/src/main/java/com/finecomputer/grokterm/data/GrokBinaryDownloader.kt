package com.finecomputer.grokterm.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Robust downloader for the official Grok Build aarch64 musl static binary.
 *
 * Follows the same resolution pattern used by the official install.sh and the
 * community Termux native installer:
 *
 * 1. GET https://x.ai/cli/stable          → version string
 * 2. GET https://x.ai/cli/grok-<ver>-linux-aarch64
 *    (fallback: storage.googleapis.com/grok-build-public-artifacts/cli/...)
 * 3. Write to app private storage, set executable, apply DNS patch.
 */
class GrokBinaryDownloader(private val context: Context) {

    private val tag = "GrokBinaryDownloader"

    private val primaryBase = "https://x.ai/cli"
    private val fallbackBase = "https://storage.googleapis.com/grok-build-public-artifacts/cli"
    private val platform = "linux-aarch64"
    private val channel = "stable"

    data class Progress(
        val stage: String,
        val bytesDownloaded: Long = 0,
        val totalBytes: Long = -1,
        val version: String? = null
    )

    sealed class Result {
        data class Success(val version: String, val path: String) : Result()
        data class Failure(val message: String, val cause: Throwable? = null) : Result()
    }

    /**
     * Download the latest stable aarch64 binary, write it, apply DNS patch.
     * @param onProgress optional callback for UI updates (called on Main? — caller decides)
     */
    suspend fun downloadLatest(
        onProgress: (Progress) -> Unit = {}
    ): Result = withContext(Dispatchers.IO) {
        try {
            onProgress(Progress("Resolving latest version…"))

            val version = resolveVersion()
                ?: return@withContext Result.Failure("Could not resolve latest version from $primaryBase/$channel")

            Log.i(tag, "Resolved version: $version")
            onProgress(Progress("Downloading grok $version…", version = version))

            val binaryManager = GrokBinaryManager(context)
            val target = File(binaryManager.binaryPath)
            target.parentFile?.mkdirs()

            val tmp = File(target.absolutePath + ".tmp")

            val downloaded = downloadBinary(version, tmp, onProgress)
            if (!downloaded) {
                tmp.delete()
                return@withContext Result.Failure("Download failed for version $version")
            }

            // Atomic replace
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }

            target.setExecutable(true, false)

            onProgress(Progress("Applying DNS patch…", version = version))
            binaryManager.ensureDnsFile()
            val patched = binaryManager.applyDnsPatch()

            if (!patched) {
                Log.w(tag, "DNS patch could not be applied (string not found or already patched)")
            }

            onProgress(Progress("Ready", version = version))
            Result.Success(version, target.absolutePath)

        } catch (e: Exception) {
            Log.e(tag, "Download failed", e)
            Result.Failure(e.message ?: "Unknown error", e)
        }
    }

    private fun resolveVersion(): String? {
        // Try primary then fallback
        listOf(primaryBase, fallbackBase).forEach { base ->
            try {
                val conn = (URL("$base/$channel").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().readText().trim()
                    // Expected plain version string e.g. "0.2.109"
                    if (body.matches(Regex("\\d+\\.\\d+\\.\\d+.*"))) {
                        return body.lines().first().trim()
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Version resolve failed for $base: ${e.message}")
            }
        }
        return null
    }

    private fun downloadBinary(
        version: String,
        dest: File,
        onProgress: (Progress) -> Unit
    ): Boolean {
        val candidates = listOf(
            "$primaryBase/grok-$version-$platform",
            "$fallbackBase/grok-$version-$platform"
        )

        for (urlStr in candidates) {
            try {
                Log.i(tag, "Trying $urlStr")
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                }

                if (conn.responseCode !in 200..299) {
                    Log.w(tag, "HTTP ${conn.responseCode} for $urlStr")
                    continue
                }

                val total = conn.contentLengthLong
                var downloaded = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(dest).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n == -1) break
                            output.write(buffer, 0, n)
                            downloaded += n
                            onProgress(
                                Progress(
                                    stage = "Downloading…",
                                    bytesDownloaded = downloaded,
                                    totalBytes = total,
                                    version = version
                                )
                            )
                        }
                        output.flush()
                    }
                }

                if (dest.length() > 1_000_000) { // sanity: real binary is several MB
                    Log.i(tag, "Downloaded ${dest.length()} bytes from $urlStr")
                    return true
                } else {
                    Log.w(tag, "Downloaded file too small: ${dest.length()} bytes")
                    dest.delete()
                }
            } catch (e: Exception) {
                Log.w(tag, "Download attempt failed for $urlStr: ${e.message}")
                dest.delete()
            }
        }
        return false
    }
}
