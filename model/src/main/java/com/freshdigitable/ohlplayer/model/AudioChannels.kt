package com.freshdigitable.ohlplayer.model

import android.util.Log
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Created by akihit on 2017/05/14.
 */
class AudioChannels internal constructor(
    private val chL: IntArray,
    private val chR: IntArray,
) {
    @JvmOverloads
    constructor(size: Int = 0) : this(IntArray(size), IntArray(size))

    private fun add(adderL: IntArray?, adderR: IntArray?) {
        val size = min(chL.size, adderL!!.size)
        for (i in 0 until size) {
            chL[i] += adderL[i]
            chR[i] += adderR!![i]
        }
    }

    fun add(adder: AudioChannels) {
        add(adder.chL, adder.chR)
    }

    fun size(): Int {
        return chL.size
    }

    fun copyFrom(src: AudioChannels, srcFrom: Int, distFrom: Int, distLen: Int) {
        System.arraycopy(src.chL, srcFrom, chL, distFrom, distLen)
        System.arraycopy(src.chR, srcFrom, chR, distFrom, distLen)
    }

    fun getL(i: Int): Int {
        return chL[i]
    }

    fun getR(i: Int): Int {
        return chR[i]
    }

    fun productFactor(factor: Double) {
        val size = size()
        for (i in 0 until size) {
            chL[i] = (chL[i] * factor).toInt()
            chR[i] = (chR[i] * factor).toInt()
        }
    }

    fun checkClipping(limit: Int): Double {
        var max = 0.0
        var maxIndex = 0
        for (i in 0 until size()) {
            var pow = (chL[i] * chL[i]).toDouble()
            if (max < pow) {
                max = pow
                maxIndex = i
            }
            pow = (chR[i] * chR[i]).toDouble()
            if (max < pow) {
                max = pow
            }
        }
        max = sqrt(max)
        if (max > limit) {
            val endFactor = limit / max
            Log.d(TAG, "checkClipping: max>$endFactor")
            productCosSlope(endFactor, maxIndex)
            return endFactor
        }
        return 1.0
    }

    private fun productCosSlope(endFactor: Double, size: Int) {
        val amp = -(1 - endFactor) / 2
        for (i in 0 until size) {
            val a = 1 + amp * (1 - cos(Math.PI * i / size))
            chL[i] = (chL[i] * a).toInt()
            chR[i] = (chR[i] * a).toInt()
        }
        val full = size()
        for (i in size until full) {
            chL[i] = (chL[i] * endFactor).toInt()
            chR[i] = (chR[i] * endFactor).toInt()
        }
    }

    companion object {
        private val TAG = AudioChannels::class.java.simpleName
    }
}
