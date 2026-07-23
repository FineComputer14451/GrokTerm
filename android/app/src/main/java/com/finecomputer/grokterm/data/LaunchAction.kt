package com.finecomputer.grokterm.data

/**
 * One-shot launch intents from the Home quick-action tiles.
 * Consumed by TerminalScreen on start.
 */
sealed class LaunchAction {
    data object Interactive : LaunchAction()
    data object Plan : LaunchAction()
    data class Headless(val prompt: String) : LaunchAction()
    data object Resume : LaunchAction()
}

/**
 * Simple process-wide holder for the next launch action.
 * Adequate for the current single-activity scaffold.
 */
object PendingLaunch {
    @Volatile
    var action: LaunchAction? = null

    fun take(): LaunchAction? {
        val a = action
        action = null
        return a
    }

    fun set(action: LaunchAction) {
        this.action = action
    }
}
