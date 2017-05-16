package com.freshdigitable.ohlplayer;

/**
 * Created by akihit on 2017/05/14.
 */

public class AudioChannels {
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
    for (int i = 0; i < adderL.length; i++) {
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
}
