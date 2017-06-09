package com.freshdigitable.ohlplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AudioProcessor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by akihit on 2017/04/27.
 */

public class OHLAudioProcessor implements AudioProcessor {
  private static final String TAG = OHLAudioProcessor.class.getSimpleName();
  private final Context context;

  public OHLAudioProcessor(@NonNull Context context) {
    this.context = context.getApplicationContext();
  }

  private int channelCount = 0;
  private int samplingFreq = 0;
  private ConvoTask convoTask;

  @Override
  public boolean configure(int sampleRateHz, int channelCount, int encoding)
      throws UnhandledFormatException {
    if (encoding != C.ENCODING_PCM_16BIT) {
      this.channelCount = 0;
      throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
    }
    if (!ImpulseResponse.SAMPLING_FREQ.isCapable(sampleRateHz) || channelCount > 2) {
      this.channelCount = 0;
      throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
    }

    boolean isUpdated = false;
    if (this.samplingFreq != sampleRateHz) {
      try {
        convoTask = StereoHRTFConvoTask.create(context, sampleRateHz);
        this.samplingFreq = sampleRateHz;
        isUpdated = true;
      } catch (IOException e) {
        Log.e(TAG, "creating ConvoTask is failed...", e);
        convoTask = null;
        return false;
      }
    }
    if (this.channelCount != channelCount) {
      isUpdated = true;
    }
    this.channelCount = channelCount;
    return isUpdated;
  }

  @Override
  public boolean isActive() {
    return channelCount != 0
        && convoTask != null;
  }

  @Override
  public int getOutputChannelCount() {
    return 2;
  }

  @Override
  public int getOutputEncoding() {
    return C.ENCODING_PCM_16BIT;
  }

  private ByteBuffer buf = EMPTY_BUFFER;
  private ByteBuffer outBuf = EMPTY_BUFFER;
  private AudioChannels tail = new AudioChannels();
  private short[] inBuf = new short[0];

  @Override
  public void queueInput(ByteBuffer inputBuf) {
//    Log.d(TAG, "queueInput: ");
    if (inputBuf.remaining() <= 0) {
      return;
    }
    if (buf.remaining() != inputBuf.remaining()) {
      buf = ByteBuffer.allocate(inputBuf.remaining()).order(ByteOrder.nativeOrder());
    } else {
      buf.clear();
    }
    final ShortBuffer shortBuffer = inputBuf.asShortBuffer();
    setupBuffer(shortBuffer);
    if (enabled) {
      convo(inBuf);
    } else {
      thru(inBuf);
    }
    inputBuf.position(inputBuf.position() + inputBuf.remaining());
    buf.flip();
    outBuf = buf;
  }

  private void setupBuffer(ShortBuffer shortBuffer) {
    final int remaining = shortBuffer.remaining();
    int bufLength = remaining;
    if (channelCount == 1) {
      bufLength *= 2;
    }
    if (inBuf.length != bufLength) {
      inBuf = new short[bufLength];
    }
    if (channelCount == 1) {
      for (int i = 0; i < remaining; i++) {
        final short s = shortBuffer.get(i);
        inBuf[2 * i] = s;
        inBuf[2 * i + 1] = s;
      }
    } else {
      shortBuffer.get(inBuf);
    }
  }

  private static final double THROUGH_FACTOR = 0.6; // XXX

  private void thru(short[] inBuf) {
    final int size = Math.min(buf.remaining() / 4, tail.size());
    for (int i = 0; i < size; i++) {
      buf.putShort((short) (tail.getL(i) + inBuf[i * 2] * THROUGH_FACTOR));
      buf.putShort((short) (tail.getR(i) + inBuf[i * 2 + 1] * THROUGH_FACTOR));
    }
    for (int i = size * 2; i < inBuf.length; i++) {
      buf.putShort((short) (inBuf[i] * THROUGH_FACTOR));
    }
    if (size < tail.size()) {
      AudioChannels newChannels = new AudioChannels(tail.size() - size);
      newChannels.copyFrom(tail, size, 0, newChannels.size());
      tail = newChannels;
    } else {
      tail = new AudioChannels();
    }
  }

  private static final int LIMIT_VALUE = 30000; // ~92% of max PCM 16bit value
  private double effectedFactor = 1;

  private void convo(short[] inBuf) {
    final int windowSize = inBuf.length / 2;
    final AudioChannels audioChannels = convoTask.convo(inBuf);
    audioChannels.productFactor(effectedFactor);
    audioChannels.add(tail);
    effectedFactor *= audioChannels.checkClipping(LIMIT_VALUE);
    for (int i = 0; i < windowSize; i++) {
      buf.putShort((short) audioChannels.getL(i));
      buf.putShort((short) audioChannels.getR(i));
    }
    if (tail.size() != audioChannels.size() - windowSize) {
      tail = new AudioChannels(audioChannels.size() - windowSize);
    }
    tail.copyFrom(audioChannels, windowSize, 0, tail.size());
  }

  private boolean inputEnded = false;

  @Override
  public void queueEndOfStream() {
    Log.d(TAG, "queueEndOfStream: ");
    this.inputEnded = true;
  }

  @Override
  public ByteBuffer getOutput() {
    ByteBuffer buffer = outBuf;
    outBuf = EMPTY_BUFFER;
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
  public void reset() {
    inputEnded = false;
    convoTask.release();
  }

  private boolean enabled = false;

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
