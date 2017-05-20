package com.freshdigitable.ohlplayer;

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

  AudioChannels(int size) {
    this(new int[size], new int[size]);
  }

  AudioChannels() {
    this(0);
  }

  private void add(int[] adderL, int[] adderR) {
    int size = Math.min(chL.length, adderL.length);
    for (int i = 0; i < size; i++) {
      this.chL[i] += adderL[i];
      this.chR[i] += adderR[i];
    }
  }

  void add(AudioChannels adder) {
    add(adder.chL, adder.chR);
  }

  int size() {
    return chL.length;
  }

  void copyFrom(AudioChannels src, int srcFrom, int distFrom, int distLen) {
    System.arraycopy(src.chL, srcFrom, chL, distFrom, distLen);
    System.arraycopy(src.chR, srcFrom, chR, distFrom, distLen);
  }

  int getL(int i) {
    return chL[i];
  }

  int getR(int i) {
    return chR[i];
  }

  void printMax() {
    int l = findMax(chL);
    int r = findMax(chR);
    Log.d(TAG, "printMax: L>" + l + ", R>" + r);
  }

  private static int findMax(int[] sig) {
    int l = 0;
    for (int c : sig) {
      l = Math.max(l, c);
    }
    return l;
  }
}
