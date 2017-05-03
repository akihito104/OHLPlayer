package com.freshdigitable.ohlplayer;

import android.support.annotation.NonNull;

/**
 * Created by akihit on 2017/05/02.
 */

public class CalcUtil {
  public static int[] convoFFT(@NonNull final short[] signal, @NonNull final ImpulseResponse ir) {
    final int resSize = signal.length + ir.getSize() - 1;
    final int fftSize = (int) (Math.log10(resSize) / Math.log10(2) + 1);
    final Complex[] sigFft = fft(signal, fftSize);
    final Complex[] irFft = fft(ir.getImpulseResponce(), fftSize);
    Complex[] convoFft = new Complex[1 << fftSize];
    for (int i = 0; i < convoFft.length; i++) {
      convoFft[i] = sigFft[i].prod(irFft[i]);
    }
    Complex[] resComp = ifft(convoFft);
    int[] res = new int[resSize];
    for (int i = 0; i < resSize; i++) {
      res[i] = (int) resComp[i].re;
    }
    return res;
  }

  public static Complex[] fft(final short[] sig, int pow) {
    final Complex[] fftSig = new Complex[1 << pow];
    for (int i = 0; i < sig.length; i++) {
      fftSig[i].re = sig[i];
      fftSig[i].im = 0;
    }
    return fft(fftSig);
  }

  public static Complex[] fft(final int[] sig, int pow) {
    final Complex[] fftSig = new Complex[1 << pow];
    for (int i = 0; i < sig.length; i++) {
      fftSig[i].re = sig[i];
      fftSig[i].im = 0;
    }
    return fft(fftSig);
  }

  // TODO
  private static Complex[] fft(final Complex[] input) {
    return new Complex[0];
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

  private static class Complex {
    private double re;
    private double im;

    Complex(double re, double im) {
      this.re = re;
      this.im = im;
    }

    Complex prod(Complex other) {
      final double re = this.re * other.re - im * other.im;
      final double im = this.im * other.re + this.re * other.im;
      return new Complex(re, im);
    }

    void conjugate() {
      im = -im;
    }
  }

  private CalcUtil() {}
}
