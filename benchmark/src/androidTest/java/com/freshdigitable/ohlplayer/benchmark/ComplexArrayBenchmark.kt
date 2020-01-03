package com.freshdigitable.ohlplayer.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.ohlplayer.model.ComplexArray
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class ComplexArrayBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private companion object {
        private const val MAX_SIZE = 256
    }

    private val input = IntArray(MAX_SIZE)

    @Before
    fun setup() {
        for (i in 0 until MAX_SIZE) {
            input[i] = (32768.0 * sin(2.0 * Math.PI * i.toDouble() / 256)).toInt()
        }
    }

    @Test
    fun calcFFT() = benchmarkRule.measureRepeated {
        ComplexArray.calcFFT(input, input.size)
    }

    @Test
    fun fft() = benchmarkRule.measureRepeated {
        val res = runWithTimingDisabled { ComplexArray(input, input.size) }
        res.fft()
    }
}