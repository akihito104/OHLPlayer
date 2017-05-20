package com.freshdigitable.ohlplayer;

/**
 * Created by akihit on 2017/05/14.
 */

public interface ConvoTask {
  AudioChannels convo(short[] input);

  void release();
}
