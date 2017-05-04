package com.freshdigitable.ohlplayer;

import com.freshdigitable.ohlplayer.CalcUtil.Complex;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class CalcUtilTest {
  @Test
  public void testFFT() throws Exception {
    final int[] input = new int[256];
    for (int i = 0; i < 100; i++) {
      input[i] = (int) (32768.0 * Math.sin(2 * Math.PI * i / 100.0));
    }
    long directCurrent = 0;
    for (int i : input) {
      directCurrent += i;
    }
    assertThat(directCurrent, is(0L));
    final Complex[] actual = CalcUtil.fft(input, input.length);
    assertThat(actual.length, is(input.length));
    assertThat(actual, is(new Complex[0]));
  }
}