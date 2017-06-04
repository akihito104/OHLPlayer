package com.freshdigitable.ohlplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;

/**
 * Created by akihit on 2015/04/18.
 */
public class ImpulseResponse {
  private double[] impulseRes;

  public static ImpulseResponse loadImpulseResponse(Context context, String name) throws IOException {
    return loadImpulseResponse(context.getAssets().openFd(name));
  }

  public static ImpulseResponse loadImpulseResponse(AssetFileDescriptor afd) throws IOException {
    ByteBuffer bb = ByteBuffer.allocate((int) afd.getLength()).order(ByteOrder.LITTLE_ENDIAN);
    FileChannel fc = null;
    int bufSize;
    try {
      fc = afd.createInputStream().getChannel();
      bufSize = fc.read(bb);
    } finally {
      if (fc !=null){
        fc.close();
      }
    }
    bb.flip();
    double[] doubleBuf = new double[bufSize / 8];
    bb.asDoubleBuffer().get(doubleBuf);
    return new ImpulseResponse(doubleBuf);
  }

  private ComplexArray hrtf = new ComplexArray(0);
  private ComplexArray cache = new ComplexArray(0);
  private int[] res = new int[0];

  public int[] convo(final ComplexArray sig, int outSize) {
    if (hrtf.size() != sig.size()) {
      hrtf = ComplexArray.calcFFT(impulseRes, sig.size());
      cache = new ComplexArray(sig.size());
    }
    if (res.length != outSize) {
      res = new int[outSize];
    }
    cache.product(hrtf, sig);
    cache.ifft();
    final double[] resDouble = cache.getReal();
    for (int i = 0; i < outSize; i++) {
      res[i] = (int) resDouble[i];
    }
    return res;
  }

  public Callable<int[]> callableConvo(final ComplexArray sig, final int outSize) {
    return new Callable<int[]>() {
      @Override
      public int[] call() throws Exception {
        return convo(sig, outSize);
      }
    };
  }

  private ImpulseResponse(double[] ir) {
    this.impulseRes = ir;
  }

  public int getSize() {
    return this.impulseRes.length;
  }

  public void reform(int start, int length, double amp) {
    final double[] newArr = new double[length];
    System.arraycopy(this.impulseRes, start, newArr, 0, length);
    this.impulseRes = newArr;
    for (int i = 0; i < length; i++) {
      impulseRes[i] *= amp;
    }
  }

  public int findFirstEdge() {
    final double pow = power();
    double sum = 0;
    for (int i = 0; i < impulseRes.length; i++) {
      sum += impulseRes[i] * impulseRes[i];
      if (sum / pow > 0.001) {
        return i;
      }
    }
    throw new IllegalStateException();
  }

  public double power() {
    return power(0, impulseRes.length);
  }

  public double power(int start, int length) {
    double pow = 0;
    int end = start + length;
    for (int i = start; i < end; i++) {
      double d = impulseRes[i];
      pow += d * d;
    }
    return pow;
  }

  public double maxAmp() {
    double max = 0;
    for (double d : impulseRes) {
      max = Math.max(max, d * d);
    }
    return Math.sqrt(max);
  }

  @Override
  public String toString() {
    return "len>" + impulseRes.length
        + ", edge> " + findFirstEdge()
        + ", pow>" + power()
        + ", max> " + maxAmp();
  }
}
