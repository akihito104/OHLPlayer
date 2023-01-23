package com.freshdigitable.ohlplayer.model

import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by akihit on 2017/05/07.
 */
class Complex @JvmOverloads internal constructor(
    var real: Double = 0.0,
    var imag: Double = 0.0,
) {
    fun prod(other: Complex): Complex {
        val re = real * other.real - imag * other.imag
        val im = imag * other.real + real * other.imag
        return Complex(re, im)
    }

    fun productedBy(other: Complex): Complex {
        val re = real * other.real - imag * other.imag
        val im = imag * other.real + real * other.imag
        real = re
        imag = im
        return this
    }

    fun add(adder: Complex) {
        real += adder.real
        imag += adder.imag
    }

    fun divideScalar(d: Double) {
        real /= d
        imag /= d
    }

    fun conjugate() {
        imag = -imag
    }

    override fun toString(): String {
        return "$real + $imag i"
    }

    fun clear() {
        real = 0.0
        imag = 0.0
    }

    companion object {
        @JvmStatic
        fun exp(radix: Double): Complex {
            val re = cos(radix)
            val im = sin(radix)
            return Complex(re, im)
        }
    }
}
