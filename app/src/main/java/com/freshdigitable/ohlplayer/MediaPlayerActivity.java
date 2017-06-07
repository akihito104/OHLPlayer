package com.freshdigitable.ohlplayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.freshdigitable.ohlplayer.store.PlayableItem;
import com.freshdigitable.ohlplayer.store.PlayableItemStore;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
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

public class MediaPlayerActivity extends AppCompatActivity {
  @SuppressWarnings("unused")
  private static final String TAG = MediaPlayerActivity.class.getSimpleName();
  private SimpleExoPlayer simpleExoPlayer;
  private PlaybackControlView controller;
  private Switch ohlToggle;
  private final PlayableItemStore playableItemStore = new PlayableItemStore();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_player);
    controller = (PlaybackControlView) findViewById(R.id.player_controller);
    ohlToggle = (Switch) findViewById(R.id.ohl_toggle);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    final OHLAudioProcessor ohlAudioProcessor = createProcessor(getApplicationContext());
    simpleExoPlayer = createPlayer(getApplicationContext(), ohlAudioProcessor);
    simpleExoPlayer.setVideoSurfaceView((SurfaceView) findViewById(R.id.player_surface_view));
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

  @Override
  protected void onStart() {
    super.onStart();
    playableItemStore.open();
    final PlayableItem item = playableItemStore.findByPath(getPath());
    if (item != null) {
      ((TextView) findViewById(R.id.player_title)).setText(item.getTitle());
      ((TextView) findViewById(R.id.player_artist)).setText(item.getArtist());
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    playableItemStore.close();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    simpleExoPlayer.stop();
    simpleExoPlayer.release();
    controller.setPlayer(null);
    ohlToggle.setOnCheckedChangeListener(null);
  }

  private static final String EXTRA_PATH = "path";

  public static Intent createIntent(@NonNull Context context, @NonNull PlayableItem item) {
    final Intent intent = new Intent(context, MediaPlayerActivity.class);
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

  public static void start(@NonNull Context context, @NonNull PlayableItem item) {
    final Intent intent = createIntent(context, item);
    context.startActivity(intent);
  }

  @NonNull
  private static OHLAudioProcessor createProcessor(@NonNull Context context) {
    return new OHLAudioProcessor(context);
  }

  @NonNull
  private static SimpleExoPlayer createPlayer(@NonNull Context context,
                                              @NonNull final OHLAudioProcessor ohlAudioProcessor) {
    final TrackSelector trackSelector = new DefaultTrackSelector();
    final DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context) {
      @Override
      protected AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[]{ohlAudioProcessor};
      }
    };
    return ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
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
