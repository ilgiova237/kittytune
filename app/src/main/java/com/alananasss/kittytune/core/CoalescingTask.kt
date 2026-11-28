package com.alananasss.kittytune.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Share work already running, while allowing later refreshes and retries. */
internal class CoalescingTask(private val scope: CoroutineScope) {
    private var running: Job? = null

    @Synchronized
    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        running?.takeIf { it.isActive }?.let { return it }
        return scope.launch(block = block).also { running = it }
    }
}
