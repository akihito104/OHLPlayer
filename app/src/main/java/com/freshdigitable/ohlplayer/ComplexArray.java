package com.freshdigitable.ohlplayer;

import java.util.Arrays;

/**
 * Created by akihit on 2017/05/07.
 */

public class ComplexArray {
  private static final int FFT_RADIX = 4;
  private final double[] real;
  private final double[] imag;
  private final int size;
  private final double[] cos;
  private final int[] reverse;

  ComplexArray(int size) {
    this.real = new double[size];
    this.imag = new double[size];
    this.size = size;
    this.cos = new double[size];
    this.reverse = new int[size];
    for (int i = 0; i < size; i++) {
      cos[i] = Math.cos(-2 * Math.PI * i / size);
      reverse[i] = reverse(size, i);
    }
  }

  public ComplexArray(short[] sig, int size) {
    this(size);
    for (int i = 0; i < sig.length; i++) {
      this.real[i] = sig[i];
    }
  }

  public ComplexArray(int[] sig, int size) {
    this(size);
    for (int i = 0; i < sig.length; i++) {
      this.real[i] = sig[i];
    }
  }

  static ComplexArray calcFFT(int[] sig, int fftSize) {
    final ComplexArray res = new ComplexArray(sig, fftSize);
    res.fft();
    return res;
  }

  void fft(short[] in) {
    for (int i = 0; i < in.length; i++) {
      real[i] = in[i];
    }
    Arrays.fill(real, in.length, size, 0);
    Arrays.fill(imag, 0);
    fft();
  }

  void fft() {
    int[] tmpIndex = new int[4];
    double[] tmpRe = new double[4], tmpIm = new double[4];
    double wRe, wIm, re0, im0;
    for (int P = size / FFT_RADIX; P >= 1; P /= FFT_RADIX) {
      final int PQ = P * FFT_RADIX;
      for (int offset = 0; offset < size; offset += PQ) {
        for (int p = offset; p < P + offset; p++) {
          tmpIndex[0] = p;
          tmpIndex[1] = P + p;
          tmpIndex[2] = 2 * P + p;
          tmpIndex[3] = 3 * P + p;
          tmpRe[0] = real[tmpIndex[0]];
          tmpIm[0] = imag[tmpIndex[0]];
          tmpRe[1] = real[tmpIndex[1]];
          tmpIm[1] = imag[tmpIndex[1]];
          tmpRe[2] = real[tmpIndex[2]];
          tmpIm[2] = imag[tmpIndex[2]];
          tmpRe[3] = real[tmpIndex[3]];
          tmpIm[3] = imag[tmpIndex[3]];
          real[tmpIndex[0]] = tmpRe[0] + tmpRe[1] + tmpRe[2] + tmpRe[3];
          imag[tmpIndex[0]] = tmpIm[0] + tmpIm[1] + tmpIm[2] + tmpIm[3];
          real[tmpIndex[1]] = tmpRe[0] + tmpIm[1] - tmpRe[2] - tmpIm[3];
          imag[tmpIndex[1]] = tmpIm[0] - tmpRe[1] - tmpIm[2] + tmpRe[3];
          real[tmpIndex[2]] = tmpRe[0] - tmpRe[1] + tmpRe[2] - tmpRe[3];
          imag[tmpIndex[2]] = tmpIm[0] - tmpIm[1] + tmpIm[2] - tmpIm[3];
          real[tmpIndex[3]] = tmpRe[0] - tmpIm[1] - tmpRe[2] + tmpIm[3];
          imag[tmpIndex[3]] = tmpIm[0] + tmpRe[1] - tmpIm[2] - tmpRe[3];
        }
        for (int r = 0; r < FFT_RADIX; r++) {
          int rp = r * P + offset;
          for (int p = 0; p < P; p++) {
            int index = r * p * (size / PQ) % size;
            wRe = cos[index];
            wIm = cos[(index + size / 4) % size];
            re0 = real[rp + p];
            im0 = imag[rp + p];
            real[rp + p] = re0 * wRe - im0 * wIm;
            imag[rp + p] = re0 * wIm + im0 * wRe;
          }
        }
      }
    }

    for (int j = 1; j < size; j++) {
      int i = reverse[j];
      if (i < j) {
        re0 = real[i];
        im0 = imag[i];
        real[i] = real[j];
        imag[i] = imag[j];
        real[j] = re0;
        imag[j] = im0;
      }
    }
  }

  private static int reverse(int size, int i) {
    int reverse = 0;
    int k = i;
    int j = size / FFT_RADIX;
    while (j > 0) {
      reverse *= FFT_RADIX;
      reverse += k % FFT_RADIX;
      k /= FFT_RADIX;
      j /= FFT_RADIX;
    }
    return reverse;
  }

  void ifft() {
    conjugate();
    fft();
    for (int i = 0; i < size; i++) {
      real[i] /= size;
//      imag[i] /= size; // whole of imag values are nearly 0
    }
  }

  void product(ComplexArray a, ComplexArray b) {
    productAll(this, a, b);
  }

  public static ComplexArray productAll(ComplexArray p, ComplexArray a, ComplexArray b) {
    final int size = p.size();
    p.real[0] = a.real[0] * b.real[0] - a.imag[0] * b.imag[0];
    p.imag[0] = a.imag[0] * b.real[0] + a.real[0] * b.imag[0];
    int half = size / 2;
    for (int i = 1; i < half; i++) {
      p.real[i] = a.real[i] * b.real[i] - a.imag[i] * b.imag[i];
      p.imag[i] = a.imag[i] * b.real[i] + a.real[i] * b.imag[i];
      p.real[size - i] = p.real[i];
      p.imag[size - i] = -p.imag[i];
    }
    p.real[half] = a.real[half] * b.real[half] - a.imag[half] * b.imag[half];
    p.imag[half] = a.imag[half] * b.real[half] + a.real[half] * b.imag[half];
    return p;
  }

  private void conjugate() {
    double tmp;
    imag[0] *= -1;
    for (int i = 1; i < size / 2; i++) {
      tmp = imag[i];
      imag[i] = imag[size - i];
      imag[size - i] = tmp;
    }
    imag[size / 2] *= -1;
  }

  static int calcFFTSize(int resSize) {
    final int radix = (int) (Math.log10(FFT_RADIX) / Math.log10(2));
    final int shift = (int) (Math.log10(resSize) / Math.log10(FFT_RADIX) + 1);
    return (int) Math.pow(2, radix * shift);
  }

  int size() {
    return size;
  }

  public double[] getReal() {
    return real;
  }

  public double[] getImag() {
    return imag;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      sb.append(real[i]).append(" + ").append(imag[i]).append(", ");
    }
    return sb.toString();
  }

  // removed to improve performance
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
}
