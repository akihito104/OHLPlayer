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
  private final double[] impulseRes;

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
    double[] doubleBuf = new double[bufSize / 8], res = new double[1400];
    bb.asDoubleBuffer().get(doubleBuf);
    System.arraycopy(doubleBuf, 190, res, 0, res.length);
    for (int i=0;i<res.length;i++) {
      res[i] *= 5;
    }
    return new ImpulseResponse(res);
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
}
