package com.freshdigitable.ohlplayer.benchmark

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.ohlplayer.model.ConvoTask
import com.freshdigitable.ohlplayer.model.StereoHRTFConvoTask
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class StereoHRTFConvoTaskBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private companion object {
        private const val MAX_SIZE = 4096
    }

    private val input = ShortArray(MAX_SIZE)
    private lateinit var sut: ConvoTask

    @Before
    fun setup() {
        for (i in 0 until MAX_SIZE) {
            input[i] = (32768.0 * sin(2.0 * Math.PI * i.toDouble() / 256)).toShort()
        }
        sut = StereoHRTFConvoTask.create(
            ApplicationProvider.getApplicationContext<Context>(), 48000
        )
    }

    @Test
    fun convo() = benchmarkRule.measureRepeated {
        sut.convo(input)
    }
}