package com.finecomputer.grokterm.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Bridge to the Phase 1 Termux environment.
 *
 * Strategies (in order):
 * 1. com.termux.RUN_COMMAND — run a command inside Termux (requires permission)
 * 2. Launch Termux main activity
 * 3. Open F-Droid / Play listing if Termux is not installed
 */
object TermuxBridge {

    const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    TERMUX_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Try to run a command inside Termux via RUN_COMMAND.
     * Falls back to simply launching Termux if the service intent fails.
     *
     * @param command full shell command, e.g. "grok" or "cd /path && grok"
     * @param workDir optional working directory path (filesystem)
     */
    fun runInTermux(
        context: Context,
        command: String = "grok",
        workDir: String? = null
    ): Boolean {
        if (!isTermuxInstalled(context)) return false

        // Prefer RUN_COMMAND service
        try {
            val intent = Intent().apply {
                setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
                action = RUN_COMMAND_ACTION
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra(
                    "com.termux.RUN_COMMAND_ARGUMENTS",
                    arrayOf("-lc", command)
                )
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir ?: "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0") // open in new session
            }
            context.startService(intent)
            return true
        } catch (_: Exception) {
            // Fall through to activity launch
        }

        return launchTermux(context)
    }

    /** Just open the Termux app. */
    fun launchTermux(context: Context): Boolean {
        if (!isTermuxInstalled(context)) return false
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    /** Open F-Droid page for Termux (preferred) or a web fallback. */
    fun openTermuxInstallPage(context: Context) {
        val fdroid = Uri.parse("https://f-droid.org/packages/com.termux/")
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, fdroid).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            // ignore
        }
    }

    /**
     * Build a sensible default command for the current project.
     */
    fun defaultGrokCommand(workDir: String?): String {
        return if (workDir != null) {
            "cd ${shellEscape(workDir)} && grok"
        } else {
            "grok"
        }
    }

    private fun shellEscape(path: String): String {
        // Minimal single-quote escape for paths
        return "'" + path.replace("'", "'\\''") + "'"
    }
}
