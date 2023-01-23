package com.freshdigitable.ohlplayer.model

import androidx.annotation.VisibleForTesting
import java.util.*
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow

/**
 * Created by akihit on 2017/05/07.
 */
class ComplexArray(size: Int) {
    val real: DoubleArray
    val imag: DoubleArray
    private val size: Int
    private val cos: DoubleArray
    private val reverse: IntArray

    init {
        real = DoubleArray(size)
        imag = DoubleArray(size)
        this.size = size
        cos = DoubleArray(size)
        reverse = IntArray(size)
        for (i in 0 until size) {
            cos[i] = cos(-2 * Math.PI * i / size)
            reverse[i] = reverse(size, i)
        }
    }

    constructor(sig: ShortArray, size: Int) : this(size) {
        for (i in sig.indices) {
            real[i] = sig[i].toDouble()
        }
    }

    constructor(sig: IntArray, size: Int) : this(size) {
        for (i in sig.indices) {
            real[i] = sig[i].toDouble()
        }
    }

    constructor(sig: DoubleArray, size: Int) : this(size) {
        System.arraycopy(sig, 0, real, 0, sig.size)
    }

    fun fft(`in`: ShortArray) {
        for (i in `in`.indices) {
            real[i] = `in`[i].toDouble()
        }
        Arrays.fill(real, `in`.size, size, 0.0)
        Arrays.fill(imag, 0.0)
        fft()
    }

    fun fft() {
        val tmpIndex = IntArray(4)
        val tmpRe = DoubleArray(4)
        val tmpIm = DoubleArray(4)
        var wRe: Double
        var wIm: Double
        var re0: Double
        var im0: Double
        var P = size / FFT_RADIX
        while (P >= 1) {
            val PQ = P * FFT_RADIX
            var offset = 0
            while (offset < size) {
                for (p in offset until P + offset) {
                    tmpIndex[0] = p
                    tmpIndex[1] = P + p
                    tmpIndex[2] = 2 * P + p
                    tmpIndex[3] = 3 * P + p
                    tmpRe[0] = real[tmpIndex[0]]
                    tmpIm[0] = imag[tmpIndex[0]]
                    tmpRe[1] = real[tmpIndex[1]]
                    tmpIm[1] = imag[tmpIndex[1]]
                    tmpRe[2] = real[tmpIndex[2]]
                    tmpIm[2] = imag[tmpIndex[2]]
                    tmpRe[3] = real[tmpIndex[3]]
                    tmpIm[3] = imag[tmpIndex[3]]
                    real[tmpIndex[0]] = tmpRe[0] + tmpRe[1] + tmpRe[2] + tmpRe[3]
                    imag[tmpIndex[0]] = tmpIm[0] + tmpIm[1] + tmpIm[2] + tmpIm[3]
                    real[tmpIndex[1]] = tmpRe[0] + tmpIm[1] - tmpRe[2] - tmpIm[3]
                    imag[tmpIndex[1]] = tmpIm[0] - tmpRe[1] - tmpIm[2] + tmpRe[3]
                    real[tmpIndex[2]] = tmpRe[0] - tmpRe[1] + tmpRe[2] - tmpRe[3]
                    imag[tmpIndex[2]] = tmpIm[0] - tmpIm[1] + tmpIm[2] - tmpIm[3]
                    real[tmpIndex[3]] = tmpRe[0] - tmpIm[1] - tmpRe[2] + tmpIm[3]
                    imag[tmpIndex[3]] = tmpIm[0] + tmpRe[1] - tmpIm[2] - tmpRe[3]
                }
                for (r in 0 until FFT_RADIX) {
                    val rp = r * P + offset
                    for (p in 0 until P) {
                        val index = r * p * (size / PQ) % size
                        wRe = cos[index]
                        wIm = cos[(index + size / 4) % size]
                        re0 = real[rp + p]
                        im0 = imag[rp + p]
                        real[rp + p] = re0 * wRe - im0 * wIm
                        imag[rp + p] = re0 * wIm + im0 * wRe
                    }
                }
                offset += PQ
            }
            P /= FFT_RADIX
        }
        for (j in 1 until size) {
            val i = reverse[j]
            if (i < j) {
                re0 = real[i]
                im0 = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = re0
                imag[j] = im0
            }
        }
    }

    fun ifft() {
        conjugate()
        fft()
        for (i in 0 until size) {
            real[i] /= size.toDouble()
            //      imag[i] /= size; // whole of imag values are nearly 0
        }
    }

    fun product(a: ComplexArray, b: ComplexArray) {
        productAll(this, a, b)
    }

    private fun conjugate() {
        var tmp: Double
        imag[0] *= -1.0
        for (i in 1 until size / 2) {
            tmp = imag[i]
            imag[i] = imag[size - i]
            imag[size - i] = tmp
        }
        imag[size / 2] *= -1.0
    }

    fun size(): Int = size

    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0 until size) {
            sb.append(real[i]).append(" + ").append(imag[i]).append(", ")
        }
        return sb.toString()
    } // removed to improve performance

    //  void swap(int i, int j) {
    //    double re = real[i];
    //    double im = imag[i];
    //    real[i] = real[j];
    //    imag[i] = imag[j];
    //    real[j] = re;
    //    imag[j] = im;
    //  }
    // removed to improve performance
    //  public void divideAllWithScalar(int scalar) {
    //    for (int i = 0; i < size; i++) {
    //      real[i] /= scalar;
    //      imag[i] /= scalar;
    //    }
    //  }
    // removed to improve performance
    //  void prodExp(int i, double radix) {
    //    final double wRe = Math.cos(radix);
    //    final double wIm = Math.sin(radix);
    //    final double re = this.real[i] * wRe - this.imag[i] * wIm;
    //    final double im = this.real[i] * wIm + this.imag[i] * wRe;
    //    this.real[i] = re;
    //    this.imag[i] = im;
    //  }
    companion object {
        private const val FFT_RADIX = 4

        @VisibleForTesting
        fun calcFFT(sig: IntArray, fftSize: Int): ComplexArray {
            val res = ComplexArray(sig, fftSize)
            res.fft()
            return res
        }

        fun calcFFT(sig: DoubleArray, fftSize: Int): ComplexArray {
            val res = ComplexArray(sig, fftSize)
            res.fft()
            return res
        }

        private fun reverse(size: Int, i: Int): Int {
            var reverse = 0
            var k = i
            var j = size / FFT_RADIX
            while (j > 0) {
                reverse *= FFT_RADIX
                reverse += k % FFT_RADIX
                k /= FFT_RADIX
                j /= FFT_RADIX
            }
            return reverse
        }

        fun productAll(p: ComplexArray, a: ComplexArray, b: ComplexArray): ComplexArray {
            val size = p.size()
            p.real[0] = a.real[0] * b.real[0] - a.imag[0] * b.imag[0]
            p.imag[0] = a.imag[0] * b.real[0] + a.real[0] * b.imag[0]
            val half = size / 2
            for (i in 1 until half) {
                p.real[i] = a.real[i] * b.real[i] - a.imag[i] * b.imag[i]
                p.imag[i] = a.imag[i] * b.real[i] + a.real[i] * b.imag[i]
                p.real[size - i] = p.real[i]
                p.imag[size - i] = -p.imag[i]
            }
            p.real[half] = a.real[half] * b.real[half] - a.imag[half] * b.imag[half]
            p.imag[half] = a.imag[half] * b.real[half] + a.real[half] * b.imag[half]
            return p
        }

        fun calcFFTSize(resSize: Int): Int {
            val radix = (log10(FFT_RADIX.toDouble()) / log10(2.0)).toInt()
            val shift =
                (log10(resSize.toDouble()) / log10(FFT_RADIX.toDouble()) + 1).toInt()
            return 2.0.pow((radix * shift).toDouble()).toInt()
        }
    }
}
