package com.freshdigitable.ohlplayer;

import android.content.Context;

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
    final ImpulseResponse hrirL = ImpulseResponse.loadImpulseResponse(context.getAssets().openFd("impCL_44100.DDB"));
    final ImpulseResponse hrirR = ImpulseResponse.loadImpulseResponse(context.getAssets().openFd("impCR_44100.DDB"));
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
      inputFft = new ComplexArray(inBuf, fftSize);
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
}