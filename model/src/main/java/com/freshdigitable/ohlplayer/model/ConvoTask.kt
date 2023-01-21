package com.freshdigitable.ohlplayer.model;

/**
 * Created by akihit on 2017/05/14.
 */

public interface ConvoTask {
  AudioChannels convo(short[] input);

  void release();
}
