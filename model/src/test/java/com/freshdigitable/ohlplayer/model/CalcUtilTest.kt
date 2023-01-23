package com.freshdigitable.ohlplayer.model

import com.freshdigitable.ohlplayer.model.Complex.Companion.exp
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class CalcUtilTest {
    private val input = IntArray(MAX_SIZE)
    private var expected: Array<Complex?> = arrayOf()
    private var elapse: Long = 0

    @Before
    fun setup() {
        for (i in 0 until MAX_SIZE) {
            input[i] = (32768.0 * sin(2 * Math.PI * i / 256)).toInt()
        }
        var directCurrent: Long = 0
        for (i in input) {
            directCurrent += i.toLong()
        }
        Truth.assertThat(directCurrent).isEqualTo(0L)
        val start = System.nanoTime()
        expected = dft(input)
        val end = System.nanoTime()
        elapse = end - start
    }

    @Test
    fun testFFT() {
        val start = System.nanoTime()
        val actual = ComplexArray.calcFFT(input, input.size)
        val end = System.nanoTime()
        Truth.assertThat(actual.size()).isEqualTo(expected.size)
        val message = actual.toString()
        for (i in expected.indices) {
            val diffReal = abs(actual.real[i] - expected[i]!!.real)
            Truth.assertWithMessage(message).that(diffReal).isLessThan(10e-5)
            val diffImag = abs(actual.imag[i] - expected[i]!!.imag)
            Truth.assertWithMessage(message).that(diffImag).isLessThan(10e-5)
        }
        val actualElapse = end - start
        Truth.assertWithMessage("fft: $actualElapse, dft: $elapse")
            .that(actualElapse).isLessThan(elapse)
    }

    private fun dft(input: IntArray): Array<Complex?> {
        val `in` = arrayOfNulls<Complex>(input.size)
        for (i in `in`.indices) {
            `in`[i] = Complex(input[i].toDouble(), 0.0)
        }
        val out = arrayOfNulls<Complex>(input.size)
        for (k in input.indices) {
            out[k] = Complex()
            val omega = -2.0 * Math.PI * k / input.size
            for (n in input.indices) {
                val prod = `in`[n]!!.prod(exp(omega * n))
                out[k]!!.add(prod)
            }
        }
        return out
    }

    companion object {
        private const val MAX_SIZE = 256
    }
}
