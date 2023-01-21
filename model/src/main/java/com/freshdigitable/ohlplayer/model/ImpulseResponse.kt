package com.freshdigitable.ohlplayer.model

import android.content.Context
import android.content.res.AssetFileDescriptor
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.Callable
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Created by akihit on 2015/04/18.
 */
class ImpulseResponse private constructor(private var impulseRes: DoubleArray) {
    private var hrtf = ComplexArray(0)
    private var cache = ComplexArray(0)
    private var res = IntArray(0)
    fun convo(sig: ComplexArray, outSize: Int): IntArray {
        if (hrtf.size() != sig.size()) {
            hrtf = ComplexArray.calcFFT(impulseRes, sig.size())
            cache = ComplexArray(sig.size())
        }
        if (res.size != outSize) {
            res = IntArray(outSize)
        }
        cache.product(hrtf, sig)
        cache.ifft()
        val resDouble = cache.real
        for (i in 0 until outSize) {
            res[i] = resDouble[i].toInt()
        }
        return res
    }

    fun callableConvo(sig: ComplexArray, outSize: Int): Callable<IntArray> {
        return Callable { convo(sig, outSize) }
    }

    val size: Int
        get() = impulseRes.size

    fun reform(start: Int, length: Int, amp: Double) {
        val newArr = DoubleArray(length)
        System.arraycopy(impulseRes, start, newArr, 0, length)
        impulseRes = newArr
        for (i in 0 until length) {
            impulseRes[i] *= amp
        }
    }

    fun findFirstEdge(): Int {
        val pow = power()
        var sum = 0.0
        for (i in impulseRes.indices) {
            sum += impulseRes[i] * impulseRes[i]
            if (sum / pow > 0.001) {
                return i
            }
        }
        throw IllegalStateException()
    }

    @JvmOverloads
    fun power(start: Int = 0, length: Int = impulseRes.size): Double {
        var pow = 0.0
        val end = start + length
        for (i in start until end) {
            val d = impulseRes[i]
            pow += d * d
        }
        return pow
    }

    fun maxAmp(): Double {
        var max = 0.0
        for (d in impulseRes) {
            max = max(max, d * d)
        }
        return sqrt(max)
    }

    override fun toString(): String {
        return ("len>" + impulseRes.size
            + ", edge> " + findFirstEdge()
            + ", pow>" + power()
            + ", max> " + maxAmp())
    }

    enum class DIRECTION {
        L30, R30, C
    }

    enum class CHANNEL {
        L, R
    }

    enum class SamplingFreq(internal val freq: Int) {
        HZ_44100(44100), HZ_48000(48000);

        companion object {
            fun isCapable(samplingFreq: Int): Boolean = values().any { it.freq == samplingFreq }

            fun valueOf(freq: Int): SamplingFreq = values().first { it.freq == freq }
        }
    }

    companion object {
        @Throws(IOException::class)
        fun load(
            context: Context,
            dir: DIRECTION,
            ch: CHANNEL,
            freq: SamplingFreq
        ): ImpulseResponse {
            val name = "imp" + dir.name + ch.name + "_" + freq.fileName + "_20k.DDB"
            return load(context, name)
        }

        private val SamplingFreq.fileName: String get() = "${this.freq}"

        @Throws(IOException::class)
        private fun load(context: Context, name: String): ImpulseResponse {
            return load(context.assets.openFd(name))
        }

        @Throws(IOException::class)
        private fun load(afd: AssetFileDescriptor): ImpulseResponse {
            val bb = ByteBuffer.allocate(afd.length.toInt()).order(ByteOrder.LITTLE_ENDIAN)
            var fc: FileChannel? = null
            val bufSize: Int
            try {
                fc = afd.createInputStream().channel
                bufSize = fc.read(bb)
            } finally {
                fc?.close()
            }
            bb.flip()
            val doubleBuf = DoubleArray(bufSize / 8)
            bb.asDoubleBuffer()[doubleBuf]
            return ImpulseResponse(doubleBuf)
        }
    }
}
