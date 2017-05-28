package com.freshdigitable.ohlplayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import static com.google.android.exoplayer2.SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF;

public class MusicPlayerActivity extends AppCompatActivity {
  private static final String TAG = MusicPlayerActivity.class.getSimpleName();
  private SimpleExoPlayer simpleExoPlayer;
  private PlaybackControlView controller;
  private Switch ohlToggle;
  private final PlayItemStore playItemStore = new PlayItemStore();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_player);
    controller = (PlaybackControlView) findViewById(R.id.player_controller);
    ohlToggle = (Switch) findViewById(R.id.ohl_toggle);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    final OHLAudioProcessor ohlAudioProcessor = createProcessor(getApplicationContext());
    if (ohlAudioProcessor == null) return;

    simpleExoPlayer = createPlayer(getApplicationContext(), ohlAudioProcessor);
    controller.setPlayer(simpleExoPlayer);
    controller.show();
    ohlToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ohlAudioProcessor.setEnabled(isChecked);
      }
    });

    final ExtractorMediaSource extractorMediaSource
        = createExtractorMediaSource(getApplicationContext(), getUri());
    simpleExoPlayer.prepare(extractorMediaSource);
  }

  private DebugTextViewHelper debugTextViewHelper;

  @Override
  protected void onStart() {
    super.onStart();
    playItemStore.open();
    final MusicItem item = playItemStore.findByPath(getPath());
    ((TextView) findViewById(R.id.player_title)).setText(item.getTitle());
    ((TextView) findViewById(R.id.player_artist)).setText(item.getArtist());

    final TextView debugText = (TextView) findViewById(R.id.player_debug);
    debugTextViewHelper = new DebugTextViewHelper(simpleExoPlayer, debugText);
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
    controller.setPlayer(null);
    ohlToggle.setOnCheckedChangeListener(null);
  }

  private static final String EXTRA_PATH = "path";

  public static Intent createIntent(@NonNull Context context, @NonNull MusicItem item) {
    final Intent intent = new Intent(context, MusicPlayerActivity.class);
    intent.setData(item.getUri());
    intent.putExtra(EXTRA_PATH, item.getPath());
    return intent;
  }

  private Uri getUri() {
    return getIntent().getData();
  }

  private String getPath() {
    return getIntent().getStringExtra(EXTRA_PATH);
  }

  public static void start(@NonNull Context context, @NonNull MusicItem item) {
    final Intent intent = createIntent(context, item);
    context.startActivity(intent);
  }

  @Nullable
  private static OHLAudioProcessor createProcessor(@NonNull Context context) {
    final ConvoTask convoTask;
    try {
      convoTask = StereoHRTFConvoTask.create(context);
    } catch (IOException e) {
      Log.e(TAG, "createProcessor: ", e);
      return null;
    }
    return new OHLAudioProcessor(convoTask);
  }

  @NonNull
  private static SimpleExoPlayer createPlayer(@NonNull Context context,
                                              @NonNull final OHLAudioProcessor ohlAudioProcessor) {
    final TrackSelector trackSelector = new DefaultTrackSelector();
    final DefaultLoadControl loadControl = new DefaultLoadControl();
    return new SimpleExoPlayer(context, trackSelector, loadControl, null,
        EXTENSION_RENDERER_MODE_OFF, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS) {
      @Override
      protected AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[]{ohlAudioProcessor};
      }
    };
  }

  private static final String USER_AGENT_NAME = "ohlplayer";

  @NonNull
  private static ExtractorMediaSource createExtractorMediaSource(@NonNull Context context,
                                                                 @NonNull Uri dataSource) {
    final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    final String userAgent = Util.getUserAgent(context, USER_AGENT_NAME);
    final DefaultDataSourceFactory dataSourceFactory
        = new DefaultDataSourceFactory(context, userAgent, bandwidthMeter);
    final DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    return new ExtractorMediaSource(dataSource, dataSourceFactory, extractorsFactory, null, null);
  }
}
