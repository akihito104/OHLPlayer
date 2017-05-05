package com.freshdigitable.ohlplayer;

import com.freshdigitable.ohlplayer.CalcUtil.Complex;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class CalcUtilTest {
  private final int[] input = new int[256];
  private Complex[] expected;
  private long elapse;

  @Before
  public void setup() {
    for (int i = 0; i < 256; i++) {
      input[i] = (int) (32768.0 * Math.sin(2 * Math.PI * i / 256));
    }
    long directCurrent = 0;
    for (int i : input) {
      directCurrent += i;
    }
    assertThat(directCurrent, is(0L));
    final long start = System.nanoTime();
    expected = dft(input);
    final long end = System.nanoTime();
    elapse = end - start;
  }

  @Test
  public void testFFT() throws Exception {
    final long start = System.nanoTime();
    final Complex[] actual = CalcUtil.fft(input, input.length);
    final long end = System.nanoTime();

    assertThat(actual.length, is(expected.length));
    final String message = Arrays.toString(actual);
    for (int i = 0; i < expected.length; i++) {
      final double diffReal = Math.abs(actual[i].getReal() - expected[i].getReal());
      assertTrue(message, diffReal < 10e-7);
      final double diffImag = Math.abs(actual[i].getImag() - expected[i].getImag());
      assertTrue(message,diffImag < 10e-7);
    }
    final long actualElapse = end - start;
    assertTrue("fft: " + actualElapse + ", dft: " + elapse, actualElapse < elapse);
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