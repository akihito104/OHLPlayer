package com.freshdigitable.ohlplayer;

/**
 * Created by akihit on 2017/05/07.
 */
public class Complex {
  private double re;
  private double im;

  Complex() {
    this(0, 0);
  }

  Complex(double re, double im) {
    this.re = re;
    this.im = im;
  }

  Complex prod(Complex other) {
    final double re = this.re * other.re - this.im * other.im;
    final double im = this.im * other.re + this.re * other.im;
    return new Complex(re, im);
  }

  Complex productedBy(Complex other) {
    final double re = this.re * other.re - this.im * other.im;
    final double im = this.im * other.re + this.re * other.im;
    this.re = re;
    this.im = im;
    return this;
  }

  void add(Complex adder) {
    this.re += adder.re;
    this.im += adder.im;
  }

  void devideScaler(double d) {
    this.re /= d;
    this.im /= d;
  }

  void conjugate() {
    im = -im;
  }

  @Override
  public String toString() {
    return re + " + " + im + " i";
  }

  public static Complex exp(double radix) {
    final double re = Math.cos(radix);
    final double im = Math.sin(radix);
    return new Complex(re, im);
  }

  public double getReal() {
    return re;
  }

  public double getImag() {
    return im;
  }

  public void clear() {
    re = 0;
    im = 0;
  }
}
