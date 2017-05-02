package com.freshdigitable.ohlplayer;

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
  private final int[] impulseRes;

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
    int[] ir = new int[1400];
    for (int i = 0; i < ir.length; i++) {
      ir[i] = (int) (doubleBuf[i + 190] * 32768.0);
    }
    return new ImpulseResponse(ir);
  }

  private int[] res = new int[0];

  public int[] convo(final short[] sig) {
    int siglen = sig.length;
    int implen = impulseRes.length;
    if (res.length != siglen + implen - 1) {
      res = new int[siglen + implen - 1];
    } else {
      for (int i = 0; i < res.length; i++) {
        res[i] = 0;
      }
    }
    for (int i = 0; i < implen; i++) {
      int ir = impulseRes[i];
      for (int j = 0; j < siglen; j++) {
        res[i + j] += sig[j] * ir;
      }
    }
    return res;
  }

  public Callable<int[]> callableConvo(final short[] sig) {
    return new Callable<int[]>() {
      @Override
      public int[] call() throws Exception {
        return convo(sig);
      }
    };
  }

  private ImpulseResponse(int[] ir) {
    this.impulseRes = ir;
  }

  public int getSize() {
    return this.impulseRes.length;
  }

  public int[] getImpulseResponce() {
    return impulseRes;
  }
}
