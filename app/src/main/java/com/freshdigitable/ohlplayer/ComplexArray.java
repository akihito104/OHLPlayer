package com.freshdigitable.ohlplayer;

import java.util.Arrays;

/**
 * Created by akihit on 2017/05/07.
 */

public class ComplexArray {
  private final double[] real;
  private final double[] imag;
  private final int size;

  ComplexArray(int size) {
    this.real = new double[size];
    this.imag = new double[size];
    this.size = size;
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

  private static final int FFT_RADIX = 4;

  static ComplexArray calcFFT(short[] sig, int fftSize) {
    final ComplexArray res = new ComplexArray(sig, fftSize);
    res.fft();
    return res;
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
    int p1;
    double o0Re, o0Im, o1Re, o1Im, o2Re, o2Im, o3Re, o3Im;
    double wRe, wIm, re0, im0, re, im, omega;
    for (int P = size / FFT_RADIX; P >= 1; P /= FFT_RADIX) {
      final int PQ = P * FFT_RADIX;
      for (int offset = 0; offset < size; offset += PQ) {
        for (int p = 0; p < P; p++) {
          p1 = p + offset;
          o0Re = real[p1];
          o0Im = imag[p1];
          o1Re = real[P + p1];
          o1Im = imag[P + p1];
          o2Re = real[2 * P + p1];
          o2Im = imag[2 * P + p1];
          o3Re = real[3 * P + p1];
          o3Im = imag[3 * P + p1];
          real[p1] = o0Re + o1Re + o2Re + o3Re;
          imag[p1] = o0Im + o1Im + o2Im + o3Im;
          real[P + p1] = o0Re + o1Im - o2Re - o3Im;
          imag[P + p1] = o0Im - o1Re - o2Im + o3Re;
          real[2 * P + p1] = o0Re - o1Re + o2Re - o3Re;
          imag[2 * P + p1] = o0Im - o1Im + o2Im - o3Im;
          real[3 * P + p1] = o0Re - o1Im - o2Re + o3Im;
          imag[3 * P + p1] = o0Im + o1Re - o2Im - o3Re;
          omega = -2 * Math.PI * p / PQ;
          for (int r = 0; r < FFT_RADIX; r++) {
            wRe = Math.cos(omega * r);
            wIm = Math.sin(omega * r);
            re0 = real[r * P + p1];
            im0 = imag[r * P + p1];
            re = re0 * wRe - im0 * wIm;
            im = re0 * wIm + im0 * wRe;
            real[r * P + p1] = re;
            imag[r * P + p1] = im;
          }
        }
      }
    }

    for (int j = 1; j < size; j++) {
      int i = reverse(size, j);
      if (j < i) {
        swap(i, j);
      }
    }
  }

  void prodExp(int i, double radix) {
    final double wRe = Math.cos(radix);
    final double wIm = Math.sin(radix);
    final double re = this.real[i] * wRe - this.imag[i] * wIm;
    final double im = this.real[i] * wIm + this.imag[i] * wRe;
    this.real[i] = re;
    this.imag[i] = im;
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

  int size() {
    return size;
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

  public static ComplexArray productAll(ComplexArray a, ComplexArray b) {
    return productAll(new ComplexArray(a.size()), a, b);
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

  public double[] getReal() {
    return real;
  }

  void setReAt(int index, double value) {
    real[index] = value;
  }

  double reAt(int index) {
    return real[index];
  }

  void setImAt(int index, double value) {
    imag[index] = value;
  }

  double imAt(int index) {
    return imag[index];
  }

  public void swap(int i, int j) {
    double re = real[i];
    double im = imag[i];
    real[i] = real[j];
    imag[i] = imag[j];
    real[j] = re;
    imag[j] = im;
  }

  public void conjugate() {
    double tmp;
    imag[0] *= -1;
    for (int i = 1; i < size / 2; i++) {
      tmp = imag[i];
      imag[i] = imag[size - i];
      imag[size - i] = tmp;
    }
    imag[size / 2] *= -1;
  }

  public void divideAllWithScalar(int scalar) {
    for (int i = 0; i < size; i++) {
      real[i] /= scalar;
      imag[i] /= scalar;
    }
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
}
