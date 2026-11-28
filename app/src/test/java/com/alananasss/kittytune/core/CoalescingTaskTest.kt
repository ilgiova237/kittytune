package com.alananasss.kittytune.core

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CoalescingTaskTest {
    @Test fun overlappingEntriesShareWorkButLaterEntryRefreshes() = runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        try {
            val work=CoalescingTask(scope)
            val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
            val count=AtomicInteger()
            val first=work.launch { count.incrementAndGet(); entered.complete(Unit); release.await() }
            withTimeout(5000) { entered.await() }
            repeat(20) { assertSame(first, work.launch { count.incrementAndGet() }) }
            release.complete(Unit);withTimeout(5000) { first.join() }
            val next=work.launch { count.incrementAndGet() };withTimeout(5000) { next.join() }
            assertNotSame(first,next);assertEquals(2,count.get())
        } finally { scope.cancel() }
    }
    @Test fun failedMaintenanceCanBeRetried() = runBlocking {
        val caught=CompletableDeferred<Unit>()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default+CoroutineExceptionHandler { _,_ -> caught.complete(Unit) })
        try {
            val work=CoalescingTask(scope)
            val failed=work.launch { error("scan failed") }
            withTimeout(5000) { caught.await(); failed.join() }
            val retried=CompletableDeferred<Unit>()
            val next=work.launch { retried.complete(Unit) }
            withTimeout(5000) { retried.await(); next.join() }
            assertNotSame(failed,next)
        } finally { scope.cancel() }
    }
}
