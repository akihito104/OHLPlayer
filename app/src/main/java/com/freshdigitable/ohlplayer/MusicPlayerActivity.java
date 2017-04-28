package com.freshdigitable.ohlplayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;

import static com.google.android.exoplayer2.ExoPlayerFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS;

public class MusicPlayerActivity extends AppCompatActivity {
  private static final String TAG = MusicPlayerActivity.class.getSimpleName();
  private SimpleExoPlayer simpleExoPlayer;
  private PlaybackControlView controller;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_player);

    final HRTF hrtfL;
    final HRTF hrtfR;
    try {
      hrtfL = HRTF.loadImpulseResponse(getApplicationContext().getAssets().openFd("impCL_44100.DDB"));
      hrtfR = HRTF.loadImpulseResponse(getApplicationContext().getAssets().openFd("impCR_44100.DDB"));
    } catch (IOException e) {
      Log.e(TAG, "onCreate: ", e);
      return;
    }

    TrackSelector trackSelector = new DefaultTrackSelector();
    final DefaultLoadControl loadControl = new DefaultLoadControl();
    simpleExoPlayer = new SimpleExoPlayer(getApplicationContext(), trackSelector, loadControl,
        null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF,DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS) {
      @Override
      protected AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[]{
            new SingleOHLAudioProcessor(hrtfL, hrtfR)
        };
      }
    };
    controller = (PlaybackControlView) findViewById(R.id.playback_confroller);
    controller.setPlayer(simpleExoPlayer);
    controller.show();

    final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    final DefaultDataSourceFactory dataSourceFactory
        = new DefaultDataSourceFactory(getApplicationContext(), Util.getUserAgent(getApplicationContext(), "ohlplayer"), bandwidthMeter);
    final DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

    Uri uri = getIntent().getData();
    final ExtractorMediaSource extractorMediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
    simpleExoPlayer.prepare(extractorMediaSource);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (simpleExoPlayer != null) {
      simpleExoPlayer.stop();
      simpleExoPlayer.release();
    }
    if (controller != null) {
      controller.setPlayer(null);
    }
  }

  static Intent createIntent(Context context, Uri music, String title) {
    Intent intent = new Intent(context, MusicPlayerActivity.class);
    intent.setData(music);
    intent.putExtra("title", title);
    return intent;
  }

  static void start(Context context, MusicItem item) {
    final Intent intent = createIntent(context, item.getUri(), item.getTitle());
    context.startActivity(intent);
  }
}
