package com.freshdigitable.ohlplayer.model

import android.content.Context
import com.freshdigitable.ohlplayer.model.ImpulseResponse.*
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Created by akihit on 2017/05/14.
 */
class CenterHRTFConvoTask private constructor(
    private val hrirL: ImpulseResponse,
    private val hrirR: ImpulseResponse,
) : ConvoTask {
    private var inBuf = ShortArray(0)
    private var inputFft = ComplexArray(0)
    private val executor = Executors.newFixedThreadPool(2)
    override fun convo(input: ShortArray): AudioChannels {
        val inputSize = input.size / 2
        if (inBuf.size != inputSize) {
            inBuf = ShortArray(inputSize)
        }
        for (i in 0 until inputSize) { // to monaural
            inBuf[i] = ((input[i * 2] + input[i * 2 + 1]) / 2).toShort()
        }
        val outSize = inBuf.size + hrirL.size - 1
        val fftSize: Int = ComplexArray.calcFFTSize(outSize)
        if (inputFft.size() != fftSize) {
            inputFft = ComplexArray(fftSize)
        }
        inputFft.fft(inBuf)
        val futures: List<Future<IntArray>>
        return try {
            futures = executor.invokeAll(
                listOf(
                    hrirL.callableConvo(inputFft, outSize),
                    hrirR.callableConvo(inputFft, outSize)
                )
            )
            AudioChannels(futures[0].get(), futures[1].get())
        } catch (e: InterruptedException) {
            AudioChannels(0)
        } catch (e: ExecutionException) {
            AudioChannels(0)
        }
    }

    override fun release() {
        executor.shutdown()
    }

    companion object {
        @Throws(IOException::class)
        fun create(context: Context): CenterHRTFConvoTask {
            val hrirL: ImpulseResponse = ImpulseResponse.load(
                context,
                DIRECTION.C,
                CHANNEL.L,
                SamplingFreq.HZ_44100
            )
            val hrirR: ImpulseResponse = ImpulseResponse.load(
                context,
                DIRECTION.C,
                CHANNEL.R,
                SamplingFreq.HZ_44100
            )
            return CenterHRTFConvoTask(hrirL, hrirR)
        }
    }
}
