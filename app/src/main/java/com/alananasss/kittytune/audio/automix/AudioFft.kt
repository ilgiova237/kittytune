package com.alananasss.kittytune.audio.automix

import kotlin.math.cos
import kotlin.math.sin

/** Shared FFT helpers for the automix analyzers. */
internal object AudioFft {
    fun hannWindow(size: Int): FloatArray =
        FloatArray(size) { 0.5f - 0.5f * cos(2.0 * Math.PI * it / size).toFloat() }

    // Bound retention even if callers introduce new transform sizes. Plans are immutable;
    // concurrent analyzers only mutate their own input arrays.
    private val plans = object : LinkedHashMap<Int, Plan>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Plan>?): Boolean = size > 4
    }

    private class Plan(val n: Int) {
        val reversed = IntArray(n)
        val real = FloatArray(n - 1)
        val imaginary = FloatArray(n - 1)
        init {
            val shift = 32 - Integer.numberOfTrailingZeros(n)
            for (i in 1 until n) reversed[i] = Integer.reverse(i) ushr shift
            var len = 2
            while (len <= n) {
                val angle = -2.0 * Math.PI / len
                val wr = cos(angle).toFloat()
                val wi = sin(angle).toFloat()
                var cr = 1f
                var ci = 0f
                val offset = len / 2 - 1
                for (k in 0 until len / 2) {
                    real[offset + k] = cr
                    imaginary[offset + k] = ci
                    // Preserve the original Float recurrence and rounding exactly.
                    val next = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = next
                }
                len = len shl 1
            }
        }
    }

    /** In-place radix-2 FFT with reusable coefficients; no per-transform arrays. */
    fun transform(re: FloatArray, im: FloatArray) {
        val n = re.size
        require(n > 0 && n and (n - 1) == 0 && im.size == n)
        if (n == 1) return
        val plan = synchronized(plans) { plans.getOrPut(n) { Plan(n) } }
        for (i in 1 until n) {
            val j = plan.reversed[i]
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val half = len / 2
            val offset = half - 1
            for (k in 0 until half) {
                val cr = plan.real[offset + k]
                val ci = plan.imaginary[offset + k]
                var i = 0
                while (i < n) {
                    val a = i + k
                    val b = a + half
                    val ur = re[a]
                    val ui = im[a]
                    val vr = re[b] * cr - im[b] * ci
                    val vi = re[b] * ci + im[b] * cr
                    re[a] = ur + vr
                    im[a] = ui + vi
                    re[b] = ur - vr
                    im[b] = ui - vi
                    i += len
                }
            }
            len = len shl 1
        }
    }
}
