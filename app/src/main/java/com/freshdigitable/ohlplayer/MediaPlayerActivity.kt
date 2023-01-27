package com.freshdigitable.ohlplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.freshdigitable.ohlplayer.databinding.ActivityMediaPlayerBinding
import com.freshdigitable.ohlplayer.store.PlayableItem
import com.freshdigitable.ohlplayer.store.PlayableItemStore
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink.DefaultAudioProcessorChain
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.video.VideoSize

class MediaPlayerActivity : AppCompatActivity() {
    private var simpleExoPlayer: ExoPlayer? = null
    private var ohlToggle: SwitchCompat? = null
    private var binding: ActivityMediaPlayerBinding? = null
    private val playableItemStore: PlayableItemStore
        get() = (application as MainApplication).playableItemStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMediaPlayerBinding.inflate(LayoutInflater.from(this))
        this.binding = binding
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(binding.playerToolbar)
        showSystemUI()

        val audioProcessor = createProcessor(applicationContext)
        ohlToggle = findViewById<SwitchCompat?>(R.id.ohl_toggle)?.also {
            it.setOnCheckedChangeListener { _, isChecked -> audioProcessor.setEnabled(isChecked) }
        }

        val simpleExoPlayer = createPlayer(applicationContext, audioProcessor)
        this.simpleExoPlayer = simpleExoPlayer
        val surfaceContainer = binding.playerSurfaceViewContainer
        simpleExoPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val width = videoSize.width
                val height = videoSize.height
                val pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
                val aspectRatio: Float =
                    if (height == 0) 1.0f else width * pixelWidthHeightRatio / height
                surfaceContainer.setAspectRatio(aspectRatio)
            }

            override fun onRenderedFirstFrame() {}
        })
        simpleExoPlayer.setVideoSurfaceView(binding.playerSurfaceView)
        binding.playerController.apply {
            player = simpleExoPlayer
            show()
        }
        val extractorMediaSource = createExtractorMediaSource(applicationContext, uri!!)
        simpleExoPlayer.setMediaSource(extractorMediaSource)
        simpleExoPlayer.prepare()
    }

    override fun onStart() {
        super.onStart()
        playableItemStore.open()
        setupTitle()

        val controller = binding?.playerController ?: return
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
            if (isSystemUIVisible(visibility)) {
                showOverlayUI(supportActionBar)
                controller.show()
            } else {
                hideOverlayUI(supportActionBar)
                controller.hide()
            }
        }
        (controller.parent as? View)?.setOnClickListener { _: View? ->
            if (isSystemUIVisible) {
                hideSystemUI()
                controller.hide()
            } else {
                showSystemUI()
                controller.show()
            }
        }
    }

    private fun setupTitle() {
        val supportActionBar = supportActionBar ?: return
        val path = this.path ?: return
        val item = playableItemStore.findByPath(path) ?: return
        supportActionBar.title = item.title
        supportActionBar.subtitle = item.artist
    }

    override fun onStop() {
        super.onStop()
        playableItemStore.close()
        window.decorView.setOnSystemUiVisibilityChangeListener(null)
        (binding?.playerController?.parent as? View)?.setOnClickListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.apply {
            stop()
            release()
        }
        binding?.playerController?.player = null
        ohlToggle?.setOnCheckedChangeListener(null)
        binding = null
    }

    private val uri: Uri?
        get() = intent.data
    private val path: String?
        get() = intent.getStringExtra(EXTRA_PATH)
    private val isSystemUIVisible: Boolean
        get() = isSystemUIVisible(window.decorView.systemUiVisibility)

    private fun showSystemUI() {
        setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        if (isInMultiWindowModeCompat) {
            showOverlayUI(supportActionBar)
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
            )
        } else {
            setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            )
        }
        if (isInMultiWindowModeCompat) {
            hideOverlayUI(supportActionBar)
        }
    }

    private fun setSystemUiVisibility(visibility: Int) {
        window.decorView.systemUiVisibility = visibility
    }

    private val isInMultiWindowModeCompat: Boolean
        get() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && isInMultiWindowMode)

    companion object {
        private val TAG = MediaPlayerActivity::class.java.simpleName
        private const val EXTRA_PATH = "path"
        private fun createIntent(context: Context, item: PlayableItem): Intent {
            val intent = Intent(context, MediaPlayerActivity::class.java)
            intent.data = item.uri
            intent.putExtra(EXTRA_PATH, item.path)
            return intent
        }

        fun start(context: Context, item: PlayableItem) {
            val intent = createIntent(context, item)
            context.startActivity(intent)
        }

        private fun createProcessor(context: Context): OHLAudioProcessor {
            return OHLAudioProcessor(context)
        }

        private fun createPlayer(
            context: Context,
            ohlAudioProcessor: OHLAudioProcessor
        ): ExoPlayer {
            val renderersFactory: DefaultRenderersFactory =
                object : DefaultRenderersFactory(context) {
                    override fun buildAudioSink(
                        context: Context,
                        enableFloatOutput: Boolean,
                        enableAudioTrackPlaybackParams: Boolean,
                        enableOffload: Boolean
                    ): AudioSink {
                        return DefaultAudioSink.Builder()
                            .setAudioProcessorChain(DefaultAudioProcessorChain(ohlAudioProcessor))
                            .build()
                    }
                }
            return ExoPlayer.Builder(context, renderersFactory).build()
        }

        private const val USER_AGENT_NAME = "ohlplayer"
        private fun createExtractorMediaSource(
            context: Context,
            dataSource: Uri
        ): MediaSource {
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)
            return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(dataSource))
        }

        private fun isSystemUIVisible(visibility: Int): Boolean {
            return View.SYSTEM_UI_FLAG_FULLSCREEN and visibility == 0
        }

        private fun showOverlayUI(actionBar: ActionBar?) {
            actionBar?.show()
        }

        private fun hideOverlayUI(actionBar: ActionBar?) {
            actionBar?.hide()
        }
    }
}
