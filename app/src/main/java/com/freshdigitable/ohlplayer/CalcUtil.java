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

  // TODO
  public static Complex[] fft(final short[] sig, int pow) {
    final short[] fftSig = new short[1 << pow];
    System.arraycopy(sig, 0, fftSig, 0, sig.length);

    return new Complex[0];
  }

  // TODO
  public static Complex[] fft(final int[] sig, int pow) {
    final int[] fftSig = new int[1 << pow];
    System.arraycopy(sig, 0, fftSig, 0, sig.length);

    return new Complex[0];
  }

  // TODO
  private static Complex[] ifft(Complex[] convoFft) {
    return new Complex[0];
  }

  private static class Complex {
    private double re;
    private double im;

    public Complex(double re, double im) {
      this.re = re;
      this.im = im;
    }

    public Complex prod(Complex other) {
      final double re = this.re * other.re - im * other.im;
      final double im = this.im * other.re + this.re * other.im;
      return new Complex(re, im);
    }
  }

  private CalcUtil() {}
}
