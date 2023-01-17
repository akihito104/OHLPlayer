package com.freshdigitable.ohlplayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.freshdigitable.ohlplayer.store.PlayableItem;
import com.freshdigitable.ohlplayer.store.PlayableItemStore;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.DefaultAudioSink;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.video.VideoSize;

public class MediaPlayerActivity extends AppCompatActivity {
  @SuppressWarnings("unused")
  private static final String TAG = MediaPlayerActivity.class.getSimpleName();
  private ExoPlayer simpleExoPlayer;
  private PlayerControlView controller;
  private SwitchCompat ohlToggle;
  private View overlayView;
  private final PlayableItemStore playableItemStore = new PlayableItemStore();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_player);
    controller = findViewById(R.id.player_controller);
    ohlToggle = findViewById(R.id.ohl_toggle);
    overlayView = findViewById(R.id.player_overlay);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setSupportActionBar(findViewById(R.id.player_toolbar));
    showSystemUI();

    final OHLAudioProcessor ohlAudioProcessor = createProcessor(getApplicationContext());
    simpleExoPlayer = createPlayer(getApplicationContext(), ohlAudioProcessor);
    final AspectRatioFrameLayout surfaceContainer = findViewById(R.id.player_surface_view_container);
    simpleExoPlayer.addListener(new Player.Listener() {
      @Override
      public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
        final int width = videoSize.width;
        final int height = videoSize.height;
        final float pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio;
        final float aspectRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;
        surfaceContainer.setAspectRatio(aspectRatio);
      }

      @Override
      public void onRenderedFirstFrame() {
      }
    });
    simpleExoPlayer.setVideoSurfaceView((SurfaceView) findViewById(R.id.player_surface_view));
    controller.setPlayer(simpleExoPlayer);
    controller.show();
    ohlToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ohlAudioProcessor.setEnabled(isChecked);
      }
    });

    final MediaSource extractorMediaSource
        = createExtractorMediaSource(getApplicationContext(), getUri());
    simpleExoPlayer.setMediaSource(extractorMediaSource);
    simpleExoPlayer.prepare();
  }

  @Override
  protected void onStart() {
    super.onStart();
    playableItemStore.open();
    final PlayableItem item = playableItemStore.findByPath(getPath());
    final ActionBar supportActionBar = getSupportActionBar();
    if (item != null && supportActionBar != null) {
      supportActionBar.setTitle(item.getTitle());
      supportActionBar.setSubtitle(item.getArtist());
    }

    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
            visibility -> {
              if (isSystemUIVisible(visibility)) {
                showOverlayUI(supportActionBar);
                controller.show();
              } else {
                hideOverlayUI(supportActionBar);
                controller.hide();
              }
            });
    overlayView.setOnClickListener(v -> {
      if (isSystemUIVisible()) {
        hideSystemUI();
        controller.hide();
      } else {
        showSystemUI();
        controller.show();
      }
    });
  }

  @Override
  protected void onStop() {
    super.onStop();
    playableItemStore.close();
    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
    overlayView.setOnClickListener(null);
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
  private static ExoPlayer createPlayer(@NonNull Context context,
                                              @NonNull final OHLAudioProcessor ohlAudioProcessor) {
    final DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context) {
      @Override
      protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams, boolean enableOffload) {
        return new DefaultAudioSink.Builder()
                .setAudioProcessorChain(new DefaultAudioSink.DefaultAudioProcessorChain(ohlAudioProcessor))
                .build();
      }
    };
    return new ExoPlayer.Builder(context, renderersFactory).build();
  }

  private static final String USER_AGENT_NAME = "ohlplayer";

  @NonNull
  private static MediaSource createExtractorMediaSource(@NonNull Context context,
                                                        @NonNull Uri dataSource) {
    final DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);
    return new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(dataSource));
  }

  private boolean isSystemUIVisible() {
    return isSystemUIVisible(getWindow().getDecorView().getSystemUiVisibility());
  }

  private static boolean isSystemUIVisible(int visibility) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      return (View.SYSTEM_UI_FLAG_FULLSCREEN & visibility) == 0;
    } else {
      return (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION & visibility) == 0;
    }
  }

  private void showSystemUI() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      );
    }
    if (isInMultiWindowModeCompat()) {
      showOverlayUI(getSupportActionBar());
    }
  }

  private static void showOverlayUI(ActionBar actionBar) {
    if (actionBar != null) {
      actionBar.show();
    }
  }

  private void hideSystemUI() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
              | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
              | View.SYSTEM_UI_FLAG_IMMERSIVE
      );
    } else {
      setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
      );
    }
    if (isInMultiWindowModeCompat()) {
      hideOverlayUI(getSupportActionBar());
    }
  }

  private static void hideOverlayUI(ActionBar actionBar) {
    if (actionBar != null) {
      actionBar.hide();
    }
  }

  private void setSystemUiVisibility(int visibility) {
    getWindow().getDecorView().setSystemUiVisibility(visibility);
  }

  private boolean isInMultiWindowModeCompat() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        && isInMultiWindowMode();
  }
}
