package com.freshdigitable.ohlplayer;

import android.support.annotation.NonNull;

/**
 * Created by akihit on 2017/05/02.
 */

public class CalcUtil {
  private static final String TAG = CalcUtil.class.getSimpleName();
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
    final int radix = (int) (Math.log10(FFT_RADIX) / Math.log10(2));
    final int shift = (int) (Math.log10(resSize) / Math.log10(FFT_RADIX) + 1);
    return (int) Math.pow(2, radix * shift);
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
      new Complex(0, -1),
      new Complex(-1, 0),
      new Complex(0, 1)
  };

  // TODO
  private static Complex[] fft(final Complex[] input) {
    final Complex[] out = new Complex[input.length];
    System.arraycopy(input, 0, out, 0, input.length);
    final Complex[] mid = new Complex[out.length];

    for (int P = out.length / FFT_RADIX; P >= 1; P /= FFT_RADIX) {
      final int PQ = P * FFT_RADIX;
      for (int offset = 0; offset < out.length; offset += PQ) {
        for (int p = 0; p < P; p++) {
          for (int r = 0; r < FFT_RADIX; r++) {
            Complex w = Complex.exp(-2 * Math.PI * p * r / PQ);
            Complex s = new Complex();
            for (int q = 0; q < FFT_RADIX; q++) {
              s.add(out[offset + q * P + p].prod(wq[(q * r) % FFT_RADIX]));
            }
            mid[r * P + p] = w.prod(s);
          }
        }
        System.arraycopy(mid, 0, out, offset, PQ);
      }
    }

    for (int j = 1; j < out.length; j++) {
      int i = reverse(out.length, j);
      if (j < i) {
        final Complex temp = out[j];
        out[j] = out[i];
        out[i] = temp;
      }
    }
    return out;
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
    private float re;
    private float im;

    Complex() {
      this(0, 0);
    }

    Complex(float re, float im) {
      this.re = re;
      this.im = im;
    }

    Complex prod(Complex other) {
      final float re = this.re * other.re - this.im * other.im;
      final float im = this.im * other.re + this.re * other.im;
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
      final float re = (float) Math.cos(radix);
      final float im = (float) Math.sin(radix);
      return new Complex(re, im);
    }

    public float getReal() {
      return re;
    }

    public float getImag() {
      return im;
    }
  }

  private CalcUtil() {}
}
