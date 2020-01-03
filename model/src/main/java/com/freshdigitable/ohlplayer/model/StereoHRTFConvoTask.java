package com.freshdigitable.ohlplayer.model;

import android.content.Context;
import androidx.annotation.NonNull;

import com.freshdigitable.ohlplayer.model.ImpulseResponse.CHANNEL;
import com.freshdigitable.ohlplayer.model.ImpulseResponse.DIRECTION;
import com.freshdigitable.ohlplayer.model.ImpulseResponse.SAMPLING_FREQ;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by akihit on 2017/05/15.
 */

public class StereoHRTFConvoTask implements ConvoTask {
//  private static final String TAG = StereoHRTFConvoTask.class.getSimpleName();
  private final ExecutorService executor = Executors.newFixedThreadPool(4);
  private final ImpulseResponse hrirL30L, hrirL30R;
  private final ImpulseResponse hrirR30L, hrirR30R;
  private short[] chL = new short[0];
  private short[] chR = new short[0];
  private ComplexArray fftChL = new ComplexArray(0);
  private ComplexArray fftChR = new ComplexArray(0);

  @Override
  public AudioChannels convo(short[] input) {
    int size = input.length / 2;
    if (size != chL.length) {
      chL = new short[size];
      chR = new short[size];
    }
    for (int i = 0; i < size; i++) {
      chL[i] = input[i * 2];
      chR[i] = input[i * 2 + 1];
    }
    final int outSize = size + hrirL30L.getSize() - 1;
    final int fftSize = ComplexArray.calcFFTSize(outSize);
    if (fftChL.size() != fftSize) {
      fftChL = new ComplexArray(fftSize);
      fftChR = new ComplexArray(fftSize);
    }
    try {
      final List<Future<ComplexArray>> fftFuture = executor.invokeAll(Arrays.asList(
          callableFFT(fftChL, chL),
          callableFFT(fftChR, chR)));
      fftChL = fftFuture.get(0).get();
      fftChR = fftFuture.get(1).get();
      final List<Future<int[]>> convoFuture = executor.invokeAll(Arrays.asList(
          hrirL30L.callableConvo(fftChL, fftSize),
          hrirL30R.callableConvo(fftChL, fftSize),
          hrirR30L.callableConvo(fftChR, fftSize),
          hrirR30R.callableConvo(fftChR, fftSize)));
      int[] outL = add(convoFuture.get(0), convoFuture.get(2));
      int[] outR = add(convoFuture.get(1), convoFuture.get(3));
      return new AudioChannels(outL, outR);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      return new AudioChannels();
    }
  }

  private int[] add(Future<int[]> a, Future<int[]> b) throws ExecutionException, InterruptedException {
    int[] aa = a.get();
    int[] bb = b.get();
    int[] res = new int[aa.length];
    for (int i = 0; i < res.length; i++) {
      res[i] = (aa[i] + bb[i]) / 2;
    }
    return res;
  }

  private Callable<ComplexArray> callableFFT(final ComplexArray fft, final short[] input) {
    return new Callable<ComplexArray>() {
      @Override
      public ComplexArray call() throws Exception {
        fft.fft(input);
        return fft;
      }
    };
  }

  @Override
  public void release() {
    executor.shutdown();
  }

  public static StereoHRTFConvoTask create(@NonNull Context context, int samplingFreq) throws IOException {
    return create(context, SAMPLING_FREQ.valueOf(samplingFreq));
  }

  private static StereoHRTFConvoTask create(@NonNull Context context,
                                    @NonNull SAMPLING_FREQ freq) throws IOException {
    final ImpulseResponse hrirL30L = ImpulseResponse.load(context, DIRECTION.L30, CHANNEL.L, freq);
    final ImpulseResponse hrirL30R = ImpulseResponse.load(context, DIRECTION.L30, CHANNEL.R, freq);
    final ImpulseResponse hrirR30L = ImpulseResponse.load(context, DIRECTION.R30, CHANNEL.L, freq);
    final ImpulseResponse hrirR30R = ImpulseResponse.load(context, DIRECTION.R30, CHANNEL.R, freq);
    return new StereoHRTFConvoTask(hrirL30L, hrirL30R, hrirR30L, hrirR30R);
  }

  private static final int RESPONSE_LENGTH = 1800;
  private static final double RESPONSE_AMP = 3;
  private StereoHRTFConvoTask(ImpulseResponse hrirL30L, ImpulseResponse hrirL30R,
                              ImpulseResponse hrirR30L, ImpulseResponse hrirR30R) {
    this.hrirL30L = hrirL30L;
    this.hrirL30R = hrirL30R;
    this.hrirR30L = hrirR30L;
    this.hrirR30R = hrirR30R;

    int startL = this.hrirL30L.findFirstEdge();
    int startR = this.hrirR30R.findFirstEdge();
    double powL = this.hrirL30L.power(startL, RESPONSE_LENGTH);// + this.hrirR30L.power(startR, RESPONSE_LENGTH);
    double powR = this.hrirR30R.power(startR, RESPONSE_LENGTH);// + this.hrirL30R.power(startL, RESPONSE_LENGTH);
    this.hrirL30L.reform(startL, RESPONSE_LENGTH, RESPONSE_AMP);
    this.hrirL30R.reform(startL, RESPONSE_LENGTH, RESPONSE_AMP);
    this.hrirR30L.reform(startR, RESPONSE_LENGTH, RESPONSE_AMP * Math.sqrt(powL / powR));
    this.hrirR30R.reform(startR, RESPONSE_LENGTH, RESPONSE_AMP * Math.sqrt(powL / powR));
  }
}
