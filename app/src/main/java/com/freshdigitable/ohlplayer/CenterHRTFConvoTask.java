package com.freshdigitable.ohlplayer;

import android.content.Context;

import com.freshdigitable.ohlplayer.ImpulseResponse.CHANNEL;
import com.freshdigitable.ohlplayer.ImpulseResponse.DIRECTION;
import com.freshdigitable.ohlplayer.ImpulseResponse.SAMPLING_FREQ;
import com.freshdigitable.ohlplayer.model.ComplexArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by akihit on 2017/05/14.
 */

public class CenterHRTFConvoTask implements ConvoTask {
  private short[] inBuf = new short[0];
  private ComplexArray inputFft = new ComplexArray(0);
  private ImpulseResponse hrirL, hrirR;
  private ExecutorService executor = Executors.newFixedThreadPool(2);

  public static CenterHRTFConvoTask create(Context context) throws IOException {
    final ImpulseResponse hrirL = ImpulseResponse.load(context, DIRECTION.C, CHANNEL.L, SAMPLING_FREQ.HZ_44100);
    final ImpulseResponse hrirR = ImpulseResponse.load(context, DIRECTION.C, CHANNEL.R, SAMPLING_FREQ.HZ_44100);
    return new CenterHRTFConvoTask(hrirL, hrirR);
  }

  private CenterHRTFConvoTask(ImpulseResponse hrirL, ImpulseResponse hrirR) {
    this.hrirL = hrirL;
    this.hrirR = hrirR;
  }

  @Override
  public AudioChannels convo(short[] input) {
    final int inputSize = input.length / 2;
    if (inBuf.length != inputSize) {
      inBuf = new short[inputSize];
    }
    for (int i = 0; i < inputSize; i++) { // to monaural
      inBuf[i] = (short) ((input[i * 2] + input[i * 2 + 1]) / 2);
    }
    final int outSize = inBuf.length + hrirL.getSize() - 1;
    final int fftSize = ComplexArray.calcFFTSize(outSize);
    if (inputFft.size() != fftSize) {
      inputFft = new ComplexArray(fftSize);
    }
    inputFft.fft(inBuf);
    final List<Future<int[]>> futures;
    try {
      futures = executor.invokeAll(Arrays.asList(
          hrirL.callableConvo(inputFft, outSize),
          hrirR.callableConvo(inputFft, outSize)));
      return new AudioChannels(futures.get(0).get(), futures.get(1).get());
    } catch (InterruptedException | ExecutionException e) {
      return new AudioChannels(0);
    }
  }

  @Override
  public void release() {
    executor.shutdown();
  }
}
