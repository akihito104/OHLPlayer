package com.freshdigitable.ohlplayer;

import android.support.annotation.NonNull;

/**
 * Created by akihit on 2017/05/02.
 */

public class CalcUtil {
  @SuppressWarnings("unused")
  private static final String TAG = CalcUtil.class.getSimpleName();
  private static final int FFT_RADIX = 4;

  public static int[] convoFFT(@NonNull final short[] signal, @NonNull final ImpulseResponse ir) {
    final int resSize = signal.length + ir.getSize() - 1;
    final int fftSize = calcFFTSize(resSize);
    final ComplexArray sigFft = fft(signal, fftSize);
    final ComplexArray irFft = fft(ir.getImpulseResponce(), fftSize);
    return convoFFT(sigFft, irFft, resSize);
  }

  static int[] convoFFT(@NonNull ComplexArray signal, @NonNull ComplexArray hrtf, int outSize) {
    final ComplexArray convoFft = ComplexArray.productAll(signal, hrtf);
    final ComplexArray resComp = ifft(convoFft);
    final double[] resDouble = resComp.getReal();
    final int[] res = new int[outSize];
    for (int i = 0; i < outSize; i++) {
      res[i] = (int) resDouble[i];
    }
    return res;
  }

  private static int calcFFTSize(int resSize) {
    final int radix = (int) (Math.log10(FFT_RADIX) / Math.log10(2));
    final int shift = (int) (Math.log10(resSize) / Math.log10(FFT_RADIX) + 1);
    return (int) Math.pow(2, radix * shift);
  }

  public static ComplexArray fft(final short[] sig, int fftSize) {
    final ComplexArray fftSig = new ComplexArray(sig, fftSize);
    return fft(fftSig);
  }

  public static ComplexArray fft(final int[] sig, int fftSize) {
    final ComplexArray fftSig = new ComplexArray(sig, fftSize);
    return fft(fftSig);
  }

  private static ComplexArray fft(final ComplexArray out) {
    final int size = out.size();
    final ComplexArray mid = new ComplexArray(size);

    for (int P = size / FFT_RADIX; P >= 1; P /= FFT_RADIX) {
      final int PQ = P * FFT_RADIX;
      for (int offset = 0; offset < size; offset += PQ) {
        for (int p = 0; p < P; p++) {
          final int p1 = p + offset;
          mid.setReAt(        p1, out.reAt(p1) + out.reAt(P + p1) + out.reAt(2 * P + p1) + out.reAt(3 * P + p1));
          mid.setImAt(        p1, out.imAt(p1) + out.imAt(P + p1) + out.imAt(2 * P + p1) + out.imAt(3 * P + p1));
          mid.setReAt(    P + p1, out.reAt(p1) + out.imAt(P + p1) - out.reAt(2 * P + p1) - out.imAt(3 * P + p1));
          mid.setImAt(    P + p1, out.imAt(p1) - out.reAt(P + p1) - out.imAt(2 * P + p1) + out.reAt(3 * P + p1));
          mid.setReAt(2 * P + p1, out.reAt(p1) - out.reAt(P + p1) + out.reAt(2 * P + p1) - out.reAt(3 * P + p1));
          mid.setImAt(2 * P + p1, out.imAt(p1) - out.imAt(P + p1) + out.imAt(2 * P + p1) - out.imAt(3 * P + p1));
          mid.setReAt(3 * P + p1, out.reAt(p1) - out.imAt(P + p1) - out.reAt(2 * P + p1) + out.imAt(3 * P + p1));
          mid.setImAt(3 * P + p1, out.imAt(p1) + out.reAt(P + p1) - out.imAt(2 * P + p1) - out.reAt(3 * P + p1));
          for (int r = 0; r < FFT_RADIX; r++) {
            mid.prodExp(r * P + p1, -2 * Math.PI * p * r / PQ);
          }
        }
      }
      out.copyFrom(mid, 0, 0, size);
      mid.clear();
    }

    for (int j = 1; j < size; j++) {
      int i = reverse(size, j);
      if (j < i) {
        out.swap(i, j);
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

  private static ComplexArray ifft(ComplexArray input) {
    input.conjugate();
    final ComplexArray output = fft(input);
    output.divideAllWithScalar(output.size());
    return output;
  }

  private CalcUtil() {}
}
