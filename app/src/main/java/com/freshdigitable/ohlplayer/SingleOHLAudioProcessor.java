package com.freshdigitable.ohlplayer;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by akihit on 2017/04/27.
 */

public class SingleOHLAudioProcessor implements AudioProcessor {
  private static final String TAG = SingleOHLAudioProcessor.class.getSimpleName();
  private static final int VOLUME = 9000;
  private int channelCount = 0;
  private ImpulseResponse hrirL, hrirR;
  private ExecutorService executor = Executors.newFixedThreadPool(2);

  SingleOHLAudioProcessor(ImpulseResponse hrirL, ImpulseResponse hrirR) {
    this.hrirL = hrirL;
    this.hrirR = hrirR;
  }

  @Override
  public boolean configure(int sampleRateHz, int channelCount, int encoding) throws UnhandledFormatException {
    if (encoding != C.ENCODING_PCM_16BIT) {
      throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
    }
    if (sampleRateHz != 44100 || channelCount > 2) {
      throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
    }
    if (this.channelCount == channelCount) {
      return false;
    }
    this.channelCount = channelCount;
    return true;
  }

  @Override
  public boolean isActive() {
    return channelCount != 0;
  }

  @Override
  public int getOutputChannelCount() {
    return 2;
  }

  @Override
  public int getOutputEncoding() {
    return C.ENCODING_PCM_16BIT;
  }

  private short[] buf = new short[0];
  private int[] tailL = new int[0];
  private int[] tailR = new int[0];

  @Override
  public void queueInput(ByteBuffer inputBuf) {
//    Log.d(TAG, "queueInput: ");
    if (inputBuf.remaining() <= 0) {
      return;
    }
    final ShortBuffer shortBuffer = inputBuf.asShortBuffer();
    if (enabled) {
      convo(shortBuffer);
    } else {
      thru(shortBuffer);
    }

    inputBuf.position(inputBuf.position() + inputBuf.remaining());
  }

  private void thru(ShortBuffer shortBuffer) {
    if (buf.length != shortBuffer.remaining()) {
      buf = new short[shortBuffer.remaining()];
    }
    shortBuffer.get(buf);
    final int size = Math.min(buf.length / 2, tailL.length);
    for (int i = 0; i < size; i++) {
      buf[i * 2] += (short) (tailL[i] / VOLUME);
      buf[i * 2 + 1] += (short) (tailR[i] / VOLUME);
    }
    if (size < tailL.length) {
      int[] newL = new int[tailL.length - size];
      int[] newR = new int[tailR.length - size];
      System.arraycopy(tailL, size, newL, 0, newL.length);
      System.arraycopy(tailR, size, newR, 0, newR.length);
      tailL = newL;
      tailR = newR;
    } else {
      tailL = new int[0];
      tailR = new int[0];
    }
  }

  private void convo(ShortBuffer shortBuffer) {
    final int remaining = shortBuffer.remaining();
    final int inputSize = remaining / 2;
    final short[] inBuf = new short[remaining];
    shortBuffer.get(inBuf);
    final short[] input = new short[inputSize];
    for (int i = 0; i < inputSize; i++) { // to monaural
      input[i] = (short) ((inBuf[i * 2] + inBuf[i * 2 + 1]) / 2);
    }
    try {
      final List<Future<int[]>> futures = executor.invokeAll(Arrays.asList(
          hrirL.callableConvo(input),
          hrirR.callableConvo(input)));
      final int[] convoL = futures.get(0).get();
      final int[] convoR = futures.get(1).get();
      for (int i = 0; i < tailR.length; i++) {
        convoL[i] += tailL[i];
        convoR[i] += tailR[i];
      }

      if (buf.length != remaining) {
        buf = new short[remaining];
      }
      for (int i = 0; i < inputSize; i++) {
        buf[i * 2] = (short) (convoL[i] / VOLUME);
        buf[i * 2 + 1] = (short) (convoR[i] / VOLUME);
      }
      if (tailL.length != convoL.length - inputSize) {
        tailL = new int[convoL.length - inputSize];
        tailR = new int[convoR.length - inputSize];
      }
      System.arraycopy(convoL, inputSize, tailL, 0, tailL.length);
      System.arraycopy(convoR, inputSize, tailR, 0, tailR.length);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  private boolean inputEnded = false;

  @Override
  public void queueEndOfStream() {
    Log.d(TAG, "queueEndOfStream: ");
    this.inputEnded = true;
  }

  @Override
  public ByteBuffer getOutput() {
//    Log.d(TAG, "getOutput: ");
    final ByteBuffer buffer = ByteBuffer.allocate(buf.length * 2).order(ByteOrder.nativeOrder());
    for (short s: buf) {
//      max = Math.max(max, s * s);
      buffer.putShort(s);
    }
    buffer.flip();
    return buffer;
  }

  @Override
  public boolean isEnded() {
    return inputEnded;
  }

  @Override
  public void flush() {
    this.inputEnded = false;
  }

  @Override
  public void release() {
    inputEnded = false;
  }

  private boolean enabled = false;

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
