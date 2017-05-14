package com.freshdigitable.ohlplayer;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by akihit on 2017/04/27.
 */

public class OHLAudioProcessor implements AudioProcessor {
  private static final String TAG = OHLAudioProcessor.class.getSimpleName();
  private static final int VOLUME = 9000; // XXX
  private int channelCount = 0;

  private final ConvoTask convoTask;

  OHLAudioProcessor(ConvoTask convoTask) {
    this.convoTask = convoTask;
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

  private ByteBuffer buf = EMPTY_BUFFER;
  private ByteBuffer outBuf = EMPTY_BUFFER;
  private AudioChannels tail = new AudioChannels();

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
    if (enabled) {
      convo(shortBuffer);
    } else {
      thru(shortBuffer);
    }
    inputBuf.position(inputBuf.position() + inputBuf.remaining());
    buf.flip();
    outBuf = buf;
  }

  private void thru(ShortBuffer shortBuffer) {
    final int size = Math.min(buf.remaining() / 4, tail.size());
    for (int i = 0; i < size; i++) {
      buf.putShort((short) (tail.getL(i) / VOLUME + shortBuffer.get(i * 2)));
      buf.putShort((short) (tail.getR(i) / VOLUME + shortBuffer.get(i * 2 + 1)));
    }
    for (int i = size * 2; i < shortBuffer.remaining(); i++) {
      buf.putShort(shortBuffer.get(i));
    }
    if (size < tail.size()) {
      AudioChannels newChannels = new AudioChannels(tail.size() - size);
      newChannels.copyFrom(tail, size, 0, newChannels.size());
      tail = newChannels;
    } else {
      tail = new AudioChannels();
    }
  }

  private short[] inBuf = new short[0];

  private void convo(ShortBuffer shortBuffer) {
    final int remaining = shortBuffer.remaining();
    final int inputSize = remaining / 2;
    if (inBuf.length != remaining) {
      inBuf = new short[remaining];
    }
    shortBuffer.get(inBuf);
    final AudioChannels audioChannels = convoTask.convo(inBuf);
    audioChannels.add(tail);
    for (int i = 0; i < inputSize; i++) {
      buf.putShort((short) (audioChannels.getL(i) / VOLUME));
      buf.putShort((short) (audioChannels.getR(i) / VOLUME));
    }
    if (tail.size() != audioChannels.size() - inputSize) {
      tail = new AudioChannels(audioChannels.size() - inputSize);
    }
    tail.copyFrom(audioChannels, inputSize, 0, tail.size());
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
  public void release() {
    inputEnded = false;
  }

  private boolean enabled = false;

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
