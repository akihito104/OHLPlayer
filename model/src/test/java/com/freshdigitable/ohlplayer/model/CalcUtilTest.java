package com.freshdigitable.ohlplayer.model;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class CalcUtilTest {
  private static final int MAX_SIZE = 256;
  private final int[] input = new int[MAX_SIZE];
  private Complex[] expected;
  private long elapse;

  @Before
  public void setup() {
    for (int i = 0; i < MAX_SIZE; i++) {
      input[i] = (int) (32768.0 * Math.sin(2 * Math.PI * i / 256));
    }
    long directCurrent = 0;
    for (int i : input) {
      directCurrent += i;
    }
    assertThat(directCurrent).isEqualTo(0L);
    final long start = System.nanoTime();
    expected = dft(input);
    final long end = System.nanoTime();
    elapse = end - start;
  }

  @Test
  public void testFFT() {
    final long start = System.nanoTime();
    final ComplexArray actual = ComplexArray.calcFFT(input, input.length);
    final long end = System.nanoTime();

    assertThat(actual.size()).isEqualTo(expected.length);
    final String message = actual.toString();
    for (int i = 0; i < expected.length; i++) {
      final double diffReal = Math.abs(actual.getReal()[i] - expected[i].getReal());
      assertWithMessage(message).that(diffReal).isLessThan(10e-5);
      final double diffImag = Math.abs(actual.getImag()[i] - expected[i].getImag());
      assertWithMessage(message).that(diffImag).isLessThan(10e-5);
    }
    final long actualElapse = end - start;
    assertWithMessage("fft: " + actualElapse + ", dft: " + elapse)
        .that(actualElapse).isLessThan(elapse);
  }

  private Complex[] dft(int[] input) {
    Complex[] in = new Complex[input.length];
    for (int i = 0; i < in.length; i++) {
      in[i] = new Complex(input[i], 0);
    }
    Complex[] out = new Complex[input.length];
    for (int k = 0; k < input.length; k++) {
      out[k] = new Complex();
      final double omega = -2.0 * Math.PI * k / input.length;
      for (int n = 0; n < input.length; n++) {
        final Complex prod = in[n].prod(Complex.exp(omega * n));
        out[k].add(prod);
      }
    }
    return out;
  }

}