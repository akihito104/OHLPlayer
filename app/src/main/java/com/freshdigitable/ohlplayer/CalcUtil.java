package com.freshdigitable.ohlplayer;

import android.support.annotation.NonNull;

/**
 * Created by akihit on 2017/05/02.
 */

public class CalcUtil {
  private static final int FFT_RADIX = 4;

  public static int[] convoFFT(@NonNull final short[] signal, @NonNull final ImpulseResponse ir) {
    final int resSize = signal.length + ir.getSize() - 1;
    final int fftSize = calcFFTSize(resSize);
    final Complex[] sigFft = fft(signal, fftSize);
    final Complex[] irFft = fft(ir.getImpulseResponce(), fftSize);
    Complex[] convoFft = new Complex[fftSize];
    for (int i = 0; i < fftSize; i++) {
      convoFft[i] = sigFft[i].prod(irFft[i]);
    }
    Complex[] resComp = ifft(convoFft);
    int[] res = new int[resSize];
    for (int i = 0; i < resSize; i++) {
      res[i] = (int) resComp[i].re;
    }
    return res;
  }

  private static int calcFFTSize(int resSize) {
    return (1 << (int) (Math.log10(FFT_RADIX) / Math.log10(2)))
        << (int) (Math.log10(resSize) / Math.log10(FFT_RADIX) + 1);
  }

  public static Complex[] fft(final short[] sig, int fftSize) {
    final Complex[] fftSig = new Complex[fftSize];
    for (int i = 0; i < fftSize; i++) {
      fftSig[i] = new Complex();
    }
    for (int i = 0; i < sig.length; i++) {
      fftSig[i].re = sig[i];
      fftSig[i].im = 0;
    }
    return fft(fftSig);
  }

  public static Complex[] fft(final int[] sig, int fftSize) {
    final Complex[] fftSig = new Complex[fftSize];
    for (int i = 0; i < fftSize; i++) {
      fftSig[i] = new Complex();
    }
    for (int i = 0; i < sig.length; i++) {
      fftSig[i].re = sig[i];
      fftSig[i].im = 0;
    }
    return fft(fftSig);
  }

  private static final Complex[] wq = new Complex[]{
      new Complex(1, 0),
      new Complex(0, 1),
      new Complex(-1, 0),
      new Complex(0, -1)
  };

  // TODO
  private static Complex[] fft(final Complex[] input) {
    Complex[] out = new Complex[input.length];
    System.arraycopy(input, 0, out, 0, out.length);
    for (int n = out.length / FFT_RADIX; n >= 1; n /= FFT_RADIX) {
      for (int m = 0; m < out.length; m += (n * FFT_RADIX)) {

        for (int p = 0; p < n; p++) {
          for (int q = 0; q < FFT_RADIX; q++) {
            Complex w = Complex.exp(-2 * Math.PI * p * q / (n * FFT_RADIX));
            Complex s = new Complex();
            for (int r = 0; r < FFT_RADIX; r++) {
              s.add(out[m + r * n + p].prod(wq[(r * q) % FFT_RADIX]));
            }
            out[m + q * n + p].add(w.prod(s));
          }
        }

      }
    }

    return out;
  }

  private static Complex[] ifft(Complex[] input) {
    for (Complex c : input) {
      c.conjugate();
    }
    final Complex[] output = fft(input);
    for (int i = 0; i < input.length; i++) {
      output[i].re /= input.length;
      output[i].im /= input.length;
    }
    return output;
  }

  public static class Complex {
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
      final double re = this.re * other.re - im * other.im;
      final double im = this.im * other.re + this.re * other.im;
      return new Complex(re, im);
    }

    void add(Complex adder) {
      this.re += adder.re;
      this.im += adder.im;
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
  }

  private CalcUtil() {}
}
