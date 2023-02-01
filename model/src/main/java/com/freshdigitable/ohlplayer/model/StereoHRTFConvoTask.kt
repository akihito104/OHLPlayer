package com.freshdigitable.ohlplayer.model

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.sqrt

/**
 * Created by akihit on 2017/05/15.
 */
class StereoHRTFConvoTask internal constructor(
    private val hrirL30L: ImpulseResponse, private val hrirL30R: ImpulseResponse,
    private val hrirR30L: ImpulseResponse, private val hrirR30R: ImpulseResponse
) : ConvoTask {
    private val executor = Executors.newFixedThreadPool(4)
    private var chL = ShortArray(0)
    private var chR = ShortArray(0)
    private var fftChL = ComplexArray(0)
    private var fftChR = ComplexArray(0)
    override fun convo(input: ShortArray): AudioChannels {
        val size = input.size / 2
        if (size != chL.size) {
            chL = ShortArray(size)
            chR = ShortArray(size)
        }
        for (i in 0 until size) {
            chL[i] = input[i * 2]
            chR[i] = input[i * 2 + 1]
        }
        val outSize = size + hrirL30L.size - 1
        val fftSize: Int = ComplexArray.calcFFTSize(outSize)
        if (fftChL.size() != fftSize) {
            fftChL = ComplexArray(fftSize)
            fftChR = ComplexArray(fftSize)
        }
        return try {
            val fftFuture = executor.invokeAll(
                listOf(
                    callableFFT(fftChL, chL),
                    callableFFT(fftChR, chR)
                )
            )
            fftChL = fftFuture[0].get()
            fftChR = fftFuture[1].get()
            val convoFuture = executor.invokeAll(
                listOf(
                    hrirL30L.callableConvo(fftChL, fftSize),
                    hrirL30R.callableConvo(fftChL, fftSize),
                    hrirR30L.callableConvo(fftChR, fftSize),
                    hrirR30R.callableConvo(fftChR, fftSize)
                )
            )
            val outL = add(convoFuture[0], convoFuture[2])
            val outR = add(convoFuture[1], convoFuture[3])
            AudioChannels(outL, outR)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            AudioChannels()
        } catch (e: ExecutionException) {
            e.printStackTrace()
            AudioChannels()
        }
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun add(a: Future<IntArray>, b: Future<IntArray>): IntArray {
        val aa = a.get()
        val bb = b.get()
        val res = IntArray(aa.size)
        for (i in res.indices) {
            res[i] = (aa[i] + bb[i]) / 2
        }
        return res
    }

    private fun callableFFT(fft: ComplexArray, input: ShortArray): Callable<ComplexArray> {
        return Callable {
            fft.fft(input)
            fft
        }
    }

    override fun release() {
        executor.shutdown()
    }

    init {
        val startL = hrirL30L.findFirstEdge()
        val startR = hrirR30R.findFirstEdge()
        val powL = hrirL30L.power(
            startL,
            RESPONSE_LENGTH
        ) // + this.hrirR30L.power(startR, RESPONSE_LENGTH);
        val powR = hrirR30R.power(
            startR,
            RESPONSE_LENGTH
        ) // + this.hrirL30R.power(startL, RESPONSE_LENGTH);
        hrirL30L.reform(startL, RESPONSE_LENGTH, RESPONSE_AMP)
        hrirL30R.reform(startL, RESPONSE_LENGTH, RESPONSE_AMP)
        hrirR30L.reform(startR, RESPONSE_LENGTH, RESPONSE_AMP * sqrt(powL / powR))
        hrirR30R.reform(startR, RESPONSE_LENGTH, RESPONSE_AMP * sqrt(powL / powR))
    }

    companion object {
        private const val RESPONSE_LENGTH = 1800
        private const val RESPONSE_AMP = 3.0
    }
}
