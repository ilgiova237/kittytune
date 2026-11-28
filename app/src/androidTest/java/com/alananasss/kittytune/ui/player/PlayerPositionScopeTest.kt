package com.alananasss.kittytune.ui.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alananasss.kittytune.ui.player.automix.AutomixDebugOverlay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PlayerPositionScopeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun positionTicksDoNotRecomposeTheSurroundingPlayer() {
        val position = mutableLongStateOf(0)
        val parentCompositions = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent {
            MaterialTheme {
                SideEffect { parentCompositions.incrementAndGet() }
                AutomixDebugOverlay(currentPosition = { reads.incrementAndGet(); position.longValue })
            }
        }
        compose.waitForIdle()
        val parentsBefore = parentCompositions.get()
        val readsBefore = reads.get()
        repeat(5) { tick ->
            compose.runOnIdle { position.longValue = (tick+1)*500L }
            compose.waitForIdle()
        }
        assertEquals(parentsBefore,parentCompositions.get())
        assertTrue(reads.get() >= readsBefore+5)
    }
}
