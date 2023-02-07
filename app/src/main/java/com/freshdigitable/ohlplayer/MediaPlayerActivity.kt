package com.freshdigitable.ohlplayer

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.freshdigitable.ohlplayer.databinding.ActivityMediaPlayerBinding
import com.freshdigitable.ohlplayer.model.ComplexArray
import com.freshdigitable.ohlplayer.model.PlayableItem
import com.freshdigitable.ohlplayer.store.PlayableItemStore
import com.freshdigitable.ohlplayer.store.uri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.audio.DefaultAudioSink.DefaultAudioProcessorChain
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.video.VideoSize
import java.lang.System.arraycopy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10

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

        val simpleExoPlayer = createPlayer(
            applicationContext,
            audioProcessor,
            VisualizerProcessor(object : VisualizerProcessor.Listener {
                override fun onInput(
                    audioFormat: AudioProcessor.AudioFormat,
                    ampL: DoubleArray,
                    ampR: DoubleArray,
                ) {
//                    Log.d(TAG, "onInput: $audioFormat")
                    binding.playerVisualizer.setAmp(ampL, ampR)
                }
            }),
        )
        this.simpleExoPlayer = simpleExoPlayer
        val surfaceContainer = binding.playerSurfaceViewContainer
        val mimeType = playableItemStore.findByPath(path)?.mimeType
        if (MimeTypes.isVideo(mimeType)) {
            binding.playerVisualizer.visibility = View.GONE
            binding.playerSurfaceView.visibility = View.VISIBLE
            simpleExoPlayer.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val aspectRatio: Float = if (videoSize.height == 0) 1.0f
                    else videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    surfaceContainer.setAspectRatio(aspectRatio)
                }

                override fun onRenderedFirstFrame() {}
            })
            simpleExoPlayer.setVideoSurfaceView(binding.playerSurfaceView)
        } else if (MimeTypes.isAudio(mimeType)) {
            binding.playerVisualizer.visibility = View.VISIBLE
            binding.playerSurfaceView.visibility = View.GONE
        }
        binding.playerController.apply {
            player = simpleExoPlayer
            show()
        }
        val extractorMediaSource = createExtractorMediaSource(applicationContext, uri)
        simpleExoPlayer.setMediaSource(extractorMediaSource)
        simpleExoPlayer.prepare()
    }

    override fun onStart() {
        super.onStart()
        setupTitle()

//        val controller = binding?.playerController ?: return
//        window.decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
//            if (isSystemUIVisible(visibility)) {
//                showOverlayUI(supportActionBar)
//                controller.show()
//            } else {
//                hideOverlayUI(supportActionBar)
//                controller.hide()
//            }
//        }
//        (controller.parent as? View)?.setOnClickListener { _: View? ->
//            if (isSystemUIVisible) {
//                hideSystemUI()
//                controller.hide()
//            } else {
//                showSystemUI()
//                controller.show()
//            }
//        }
    }

    private fun setupTitle() {
        val supportActionBar = supportActionBar ?: return
        val item = playableItemStore.findByPath(path) ?: return
        supportActionBar.title = item.title
        supportActionBar.subtitle = item.artist
    }

    override fun onStop() {
        super.onStop()
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

    private val uri: Uri
        get() = intent.data
            ?: throw IllegalStateException("use MediaPlayerActivity.start() to launch")
    private val path: String
        get() = intent.getStringExtra(EXTRA_PATH)
            ?: throw IllegalStateException("use MediaPlayerActivity.start() to launch")
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
        @Suppress("unused")
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
            ohlAudioProcessor: OHLAudioProcessor,
            visualizerProcessor: AudioProcessor,
        ): ExoPlayer {
            val renderersFactory: DefaultRenderersFactory =
                object : DefaultRenderersFactory(context) {
                    override fun buildAudioSink(
                        context: Context,
                        enableFloatOutput: Boolean,
                        enableAudioTrackPlaybackParams: Boolean,
                        enableOffload: Boolean
                    ): AudioSink = DefaultAudioSink.Builder()
                        .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setOffloadMode(
                            if (enableOffload) DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED
                            else DefaultAudioSink.OFFLOAD_MODE_DISABLED
                        )
                        .setAudioProcessorChain(
                            DefaultAudioProcessorChain(
                                ohlAudioProcessor,
                                visualizerProcessor,
                            )
                        )
                        .build()
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

class VisualizerProcessor(
    private val listener: Listener,
) : BaseAudioProcessor() {

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat = inputAudioFormat

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) {
            return
        }
        val buffer = inputBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder())
        val buf = setupBuffer(buffer.asShortBuffer())
        visualize(buf)
        replaceOutputBuffer(remaining).put(inputBuffer).flip()
    }

    private var inBuf: ShortArray = ShortArray(0)
    private fun setupBuffer(shortBuffer: ShortBuffer): ShortArray {
        val remaining = shortBuffer.remaining()
        val bufLength = if (inputAudioFormat.channelCount == 1) remaining * 2 else remaining
        if (inBuf.size != bufLength) {
            inBuf = ShortArray(bufLength)
        }
        if (inputAudioFormat.channelCount == 1) {
            for (i in 0 until remaining) {
                val s = shortBuffer[i]
                inBuf[2 * i] = s
                inBuf[2 * i + 1] = s
            }
        } else {
            shortBuffer.get(inBuf)
        }
        return inBuf
    }

    private var chL = ShortArray(0)
    private var chR = ShortArray(0)
    private var fftChL = ComplexArray(0)
    private var fftChR = ComplexArray(0)
    private fun setupChannel(input: ShortArray) {
        val size = input.size / 2
        if (size != chL.size) {
            chL = ShortArray(size)
            chR = ShortArray(size)
        }
        for (i in 0 until size) {
            chL[i] = input[i * 2]
            chR[i] = input[i * 2 + 1]
        }
        val fftSize: Int = ComplexArray.calcFFTSize(size)
        if (fftChL.size() != fftSize) {
            fftChL = ComplexArray(fftSize)
            fftChR = ComplexArray(fftSize)
        }
    }

    private var ampChL = DoubleArray(0)
    private var ampChR = DoubleArray(0)
    private fun visualize(inBuf: ShortArray) {
        // ComplexArray
        setupChannel(inBuf)
        // process frequency amplitude char.
        ampChL = calcSpectre(chL, fftChL, ampChL)
        ampChR = calcSpectre(chR, fftChR, ampChR)
        // pass to listener
        listener.onInput(inputAudioFormat, ampL = ampChL, ampR = ampChR)
    }

    private fun calcSpectre(
        input: ShortArray,
        fftDest: ComplexArray,
        spectreDest: DoubleArray,
    ): DoubleArray {
        val res = if (spectreDest.size != fftDest.size() / 2) {
            DoubleArray(fftDest.size() / 2)
        } else spectreDest
        input.hammingWindow()
        fftDest.fft(input)
        fftDest.calcAmpCharacteristic(res)
        return res
    }

    companion object {
        private fun ComplexArray.calcAmpCharacteristic(dst: DoubleArray) {
            for (i in dst.indices) {
                dst[i] = 10.0 * log10(real[i] * real[i] + imag[i] * imag[i])
            }
        }

        private fun ShortArray.hammingWindow() {
            val omega = 2.0 * PI / this.size
            for (i in this.indices) {
                this[i] = (this[i] * (0.54 - 0.46 * cos(omega * i))).toInt().toShort()
            }
        }
    }

    interface Listener {
        fun onInput(audioFormat: AudioProcessor.AudioFormat, ampL: DoubleArray, ampR: DoubleArray)
    }
}

class AmplifierVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var ampL: DoubleArray = DoubleArray(0)
    private var ampR: DoubleArray = DoubleArray(0)
    fun setAmp(ampL: DoubleArray, ampR: DoubleArray) {
        if (ampL.size != this.ampL.size) {
            this.ampL = DoubleArray(ampL.size)
        }
        arraycopy(ampL, 0, this.ampL, 0, ampL.size)
        if (ampR.size != this.ampR.size) {
            this.ampR = DoubleArray(ampR.size)
        }
        arraycopy(ampR, 0, this.ampR, 0, ampR.size)
        invalidate()
    }

    private val path = Path()
    private val paint = Paint().apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawAmp(ampL, Color.BLUE)
        canvas.drawAmp(ampR, Color.RED)
    }

    private fun Canvas.drawAmp(amp: DoubleArray, @ColorInt color: Int) {
        path.reset()
        paint.color = color

        val offsetY = height.toFloat() * 0.75f
        val h = height / 2
        path.moveTo(0f, offsetY)
        amp.forEachIndexed { i, a ->
            val x = i.toFloat() / amp.size.toFloat()
            val y: Float = a.toFloat() / 150f
            path.lineTo(x * width, -y * h + offsetY)
        }
        drawPath(path, paint)
    }
}
