package com.freshdigitable.ohlplayer;

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

  int size() {
    return size;
  }

  public static ComplexArray productAll(ComplexArray a, ComplexArray b) {
    final ComplexArray res = new ComplexArray(a.size());
    final int size = res.size();
    for (int i = 0; i < size; i++) {
      res.real[i] = a.real[i] * b.real[i] - a.imag[i] * b.imag[i];
      res.imag[i] = a.imag[i] * b.real[i] + a.real[i] * b.imag[i];
    }
    return res;
  }

  public double[] getReal() {
    return real;
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
    for (int i = 0; i < imag.length; i++) {
      imag[i] = -imag[i];
    }
  }

  public void divideAllWithScalar(int scalar) {
    for (int i = 0; i < size; i++) {
      real[i] /= scalar;
      imag[i] /= scalar;
    }
  }

  public double prodReal(int i, Complex c) {
    return prodReal(i, c.getReal(), c.getImag());
  }

  double prodReal(int i, double re, double im) {
    return this.real[i] * re - this.imag[i] * im;
  }

  double prodImag(int i, Complex c) {
    return prodImag(i, c.getReal(), c.getImag());
  }

  double prodImag(int i, double re, double im) {
    return this.real[i] * im + this.imag[i] * re;
  }

  public void add(int i, double re, double im) {
    this.real[i] += re;
    this.imag[i] += im;
  }

  public void prodExp(int i, double radix) {
    final double wRe = Math.cos(radix);
    final double wIm = Math.sin(radix);
    final double re = prodReal(i, wRe, wIm);
    final double im = prodImag(i, wRe, wIm);
    this.real[i] = re;
    this.imag[i] = im;
  }

  public void copyFrom(ComplexArray src, int srcFrom, int distFrom, int length) {
    System.arraycopy(src.getReal(), srcFrom, real, distFrom, length);
    System.arraycopy(src.getImag(), srcFrom, imag, distFrom, length);
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

  public void clear() {
    for (int i = 0; i < size; i++) {
      this.real[i] = 0;
      this.imag[i] = 0;
    }
  }
}
