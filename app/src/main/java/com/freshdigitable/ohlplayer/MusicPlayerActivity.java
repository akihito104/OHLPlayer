package com.freshdigitable.ohlplayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
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
  private Switch ohlToggle;
  private DebugTextViewHelper debugTextViewHelper;
  private final PlayItemStore playItemStore = new PlayItemStore();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_player);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    final ConvoTask convoTask;
    try {
      convoTask = StereoHRTFConvoTask.create(getApplicationContext());
    } catch (IOException e) {
      Log.e(TAG, "onCreate: ", e);
      return;
    }
    final OHLAudioProcessor ohlAudioProcessor = new OHLAudioProcessor(convoTask);

    TrackSelector trackSelector = new DefaultTrackSelector();
    final DefaultLoadControl loadControl = new DefaultLoadControl();
    simpleExoPlayer = new SimpleExoPlayer(getApplicationContext(), trackSelector, loadControl,
        null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF,DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS) {
      @Override
      protected AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[]{ohlAudioProcessor};
      }
    };
    controller = (PlaybackControlView) findViewById(R.id.player_controller);
    controller.setPlayer(simpleExoPlayer);
    controller.show();
    ohlToggle = (Switch) findViewById(R.id.ohl_toggle);
    ohlToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ohlAudioProcessor.setEnabled(isChecked);
      }
    });
    final TextView debugText = (TextView) findViewById(R.id.player_debug);
    debugTextViewHelper = new DebugTextViewHelper(simpleExoPlayer, debugText);

    final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    final DefaultDataSourceFactory dataSourceFactory
        = new DefaultDataSourceFactory(getApplicationContext(), Util.getUserAgent(getApplicationContext(), "ohlplayer"), bandwidthMeter);
    final DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

    Uri uri = getIntent().getData();
    final ExtractorMediaSource extractorMediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
    simpleExoPlayer.prepare(extractorMediaSource);
  }

  @Override
  protected void onStart() {
    super.onStart();
    playItemStore.open();
    MusicItem item = playItemStore.findByPath(getIntent().getStringExtra("path"));
    ((TextView) findViewById(R.id.player_title)).setText(item.getTitle());
    ((TextView) findViewById(R.id.player_artist)).setText(item.getArtist());
    debugTextViewHelper.start();
  }

  @Override
  protected void onStop() {
    super.onStop();
    playItemStore.close();
    debugTextViewHelper.stop();
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
    if (ohlToggle != null) {
      ohlToggle.setOnCheckedChangeListener(null);
    }
  }

  static Intent createIntent(Context context, MusicItem item) {
    Intent intent = new Intent(context, MusicPlayerActivity.class);
    intent.setData(item.getUri());
    intent.putExtra("path", item.getPath());
    return intent;
  }

  static void start(Context context, MusicItem item) {
    Intent intent = createIntent(context, item);
    context.startActivity(intent);
  }
}
