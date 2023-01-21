package com.freshdigitable.ohlplayer.model;

import android.util.Log;

/**
 * Created by akihit on 2017/05/14.
 */

public class AudioChannels {
  private static final String TAG = AudioChannels.class.getSimpleName();
  private final int[] chL;
  private final int[] chR;

  AudioChannels(int[] chL, int[] chR) {
    this.chL = chL;
    this.chR = chR;
  }

  public AudioChannels(int size) {
    this(new int[size], new int[size]);
  }

  public AudioChannels() {
    this(0);
  }

  private void add(int[] adderL, int[] adderR) {
    int size = Math.min(chL.length, adderL.length);
    for (int i = 0; i < size; i++) {
      this.chL[i] += adderL[i];
      this.chR[i] += adderR[i];
    }
  }

  public void add(AudioChannels adder) {
    add(adder.chL, adder.chR);
  }

  public int size() {
    return chL.length;
  }

  public void copyFrom(AudioChannels src, int srcFrom, int distFrom, int distLen) {
    System.arraycopy(src.chL, srcFrom, chL, distFrom, distLen);
    System.arraycopy(src.chR, srcFrom, chR, distFrom, distLen);
  }

  public int getL(int i) {
    return chL[i];
  }

  public int getR(int i) {
    return chR[i];
  }

  public void productFactor(double factor) {
    final int size = size();
    for (int i = 0; i < size; i++) {
      chL[i] *= factor;
      chR[i] *= factor;
    }
  }

  public double checkClipping(int limit) {
    double max = 0;
    int maxIndex = 0;
    for (int i = 0; i < size(); i++) {
      double pow = chL[i] * chL[i];
      if (max < pow) {
        max = pow;
        maxIndex = i;
      }
      pow = chR[i] * chR[i];
      if (max < pow) {
        max = pow;
      }
    }
    max = Math.sqrt(max);
    if (max > limit) {
      final double endFactor = limit / max;
      Log.d(TAG, "checkClipping: max>" + endFactor);
      productCosSlope(endFactor, maxIndex);
      return endFactor;
    }
    return 1;
  }

  private void productCosSlope(double endFactor, int size) {
    final double amp = -(1 - endFactor) / 2;

    for (int i = 0; i < size; i++) {
      final double a = 1 + amp * (1 - Math.cos(Math.PI * i / size));
      chL[i] *= a;
      chR[i] *= a;
    }
    int full = size();
    for (int i = size; i < full; i++) {
      chL[i] *= endFactor;
      chR[i] *= endFactor;
    }
  }
}
