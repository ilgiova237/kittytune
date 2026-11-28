package com.alananasss.kittytune.core

/** Process-lifetime setup: concurrent callers wait, failures remain retryable. */
internal class ProcessInitializer {
    @Volatile var isInitialized: Boolean = false
        private set

    fun initialize(block: () -> Unit) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            block()
            isInitialized = true
        }
    }
}
