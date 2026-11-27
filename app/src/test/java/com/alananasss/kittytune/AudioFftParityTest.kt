package com.alananasss.kittytune

import com.alananasss.kittytune.audio.automix.AudioFft
import kotlin.math.*
import kotlin.random.Random
import org.junit.Test
import org.junit.Assert.*

class AudioFftParityTest {
    private fun compare(re: FloatArray, im: FloatArray) {
        val expectedRe = re.copyOf()
        val expectedIm = im.copyOf()
        LegacyFft.fft(expectedRe, expectedIm)
        AudioFft.transform(re, im)
        assertTrue("real n=${re.size}", expectedRe.contentEquals(re))
        assertTrue("imaginary n=${im.size}", expectedIm.contentEquals(im))
    }

    @Test fun complexInputsAndCacheEvictionMatchLegacyExactly() {
        val random = Random(829)
        repeat(3) {
            for (n in listOf(1, 2, 4, 8, 16, 64, 256, 1024, 2048, 4096, 8192, 16384)) {
                repeat(5) { compare(FloatArray(n) { random.nextFloat() * 2 - 1 }, FloatArray(n) { random.nextFloat() * 2 - 1 }) }
            }
        }
    }

    @Test fun silenceImpulseConstantAndSineMatchExactly() {
        for (n in listOf(2, 64, 1024, 4096)) {
            compare(FloatArray(n), FloatArray(n))
            compare(FloatArray(n) { if (it == 0) 1f else 0f }, FloatArray(n))
            compare(FloatArray(n) { 1f }, FloatArray(n))
            compare(FloatArray(n) { sin(2 * PI * it * 7 / n).toFloat() }, FloatArray(n))
        }
    }

    @Test fun concurrentTransformsHaveIndependentInputs() {
        val failures = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
        val threads = List(4) { seed -> Thread {
            try {
                val random = Random(seed)
                repeat(30) { compare(FloatArray(1024) { random.nextFloat() }, FloatArray(1024)) }
            } catch (failure: Throwable) { failures.add(failure) }
        } }
        threads.forEach { it.start() }; threads.forEach { it.join() }
        assertTrue(failures.toString(), failures.isEmpty())
    }
}

// Frozen pre-optimization implementation: checks exact Float operation results,
// including twiddle recurrence rounding, rather than a loose FFT tolerance.
private object LegacyFft {
    fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * Math.PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f
                var curIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val vIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}