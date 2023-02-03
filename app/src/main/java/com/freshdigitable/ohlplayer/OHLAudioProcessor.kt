package com.freshdigitable.ohlplayer

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import com.freshdigitable.ohlplayer.model.AudioChannels
import com.freshdigitable.ohlplayer.model.ConvoTask
import com.freshdigitable.ohlplayer.model.ImpulseResponse
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.AudioProcessor.UnhandledAudioFormatException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.min

/**
 * Created by akihit on 2017/04/27.
 */
class OHLAudioProcessor(context: Context) : AudioProcessor {
    private val context: Context
    private var channelCount = 0
    private var samplingFreq = 0
    private var convoTask: ConvoTask? = null

    @Throws(UnhandledAudioFormatException::class)
    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        val sampleRateHz = inputAudioFormat.sampleRate
        val channelCount = inputAudioFormat.channelCount
        if (encoding != C.ENCODING_PCM_16BIT) {
            this.channelCount = 0
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        if (!ImpulseResponse.SamplingFreq.isCapable(sampleRateHz) || channelCount > 2) {
            this.channelCount = 0
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        if (samplingFreq != sampleRateHz) {
            try {
                convoTask = ConvoTask.createStereo(context, sampleRateHz)
                samplingFreq = sampleRateHz
            } catch (e: IOException) {
                Log.e(TAG, "creating ConvoTask is failed...", e)
                convoTask = null
                throw IllegalStateException()
            }
        }
        this.channelCount = channelCount
        return AudioProcessor.AudioFormat(sampleRateHz, 2, C.ENCODING_PCM_16BIT)
    }

    override fun isActive(): Boolean {
        return (channelCount != 0
            && convoTask != null)
    }

    private var buf = AudioProcessor.EMPTY_BUFFER
    private var outBuf = AudioProcessor.EMPTY_BUFFER
    private var tail = AudioChannels()
    private var inBuf = ShortArray(0)
    override fun queueInput(inputBuf: ByteBuffer) {
//    Log.d(TAG, "queueInput: ");
        if (inputBuf.remaining() <= 0) {
            return
        }
        if (buf.remaining() != inputBuf.remaining()) {
            buf = ByteBuffer.allocate(inputBuf.remaining()).order(ByteOrder.nativeOrder())
        } else {
            buf.clear()
        }
        val shortBuffer = inputBuf.asShortBuffer()
        setupBuffer(shortBuffer)
        if (enabled) {
            convo(inBuf)
        } else {
            thru(inBuf)
        }
        inputBuf.position(inputBuf.position() + inputBuf.remaining())
        buf.flip()
        outBuf = buf
    }

    private fun setupBuffer(shortBuffer: ShortBuffer) {
        val remaining = shortBuffer.remaining()
        val bufLength = if (channelCount == 1) remaining * 2 else remaining
        if (inBuf.size != bufLength) {
            inBuf = ShortArray(bufLength)
        }
        if (channelCount == 1) {
            for (i in 0 until remaining) {
                val s = shortBuffer[i]
                inBuf[2 * i] = s
                inBuf[2 * i + 1] = s
            }
        } else {
            shortBuffer[inBuf]
        }
    }

    private var throughFactor = 0.6
    private fun thru(inBuf: ShortArray) {
        val size = min(buf.remaining() / 4, tail.size())
        for (i in 0 until size) {
            buf.putShort((tail.getL(i) + inBuf[i * 2] * throughFactor).toInt().toShort())
            buf.putShort((tail.getR(i) + inBuf[i * 2 + 1] * throughFactor).toInt().toShort())
        }
        for (i in size * 2 until inBuf.size) {
            buf.putShort((inBuf[i] * throughFactor).toInt().toShort())
        }
        tail = if (size < tail.size()) {
            val newChannels = AudioChannels(tail.size() - size)
            newChannels.copyFrom(tail, size, 0, newChannels.size())
            newChannels
        } else {
            AudioChannels()
        }
    }

    private var effectedFactor = 1.0
    private fun convo(inBuf: ShortArray) {
        val convoTask = this.convoTask ?: throw IllegalStateException()

        val windowSize = inBuf.size / 2
        val audioChannels = convoTask.convo(inBuf)
        audioChannels.productFactor(effectedFactor)
        audioChannels.add(tail)
        val ampFactor = audioChannels.checkClipping(LIMIT_VALUE)
        effectedFactor *= ampFactor
        throughFactor *= ampFactor
        for (i in 0 until windowSize) {
            buf.putShort(audioChannels.getL(i).toShort())
            buf.putShort(audioChannels.getR(i).toShort())
        }
        if (tail.size() != audioChannels.size() - windowSize) {
            tail = AudioChannels(audioChannels.size() - windowSize)
        }
        tail.copyFrom(audioChannels, windowSize, 0, tail.size())
    }

    private var inputEnded = false
    override fun queueEndOfStream() {
        Log.d(TAG, "queueEndOfStream: ")
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outBuf
        outBuf = AudioProcessor.EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean {
        return inputEnded
    }

    override fun flush() {
        inputEnded = false
    }

    override fun reset() {
        inputEnded = false
        convoTask!!.release()
    }

    private var enabled = false

    init {
        this.context = context.applicationContext
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    companion object {
        private val TAG = OHLAudioProcessor::class.java.simpleName
        private const val LIMIT_VALUE = 30000 // ~92% of max PCM 16bit value

        private fun ConvoTask.Companion.createStereo(
            context: Context,
            sampleRateHz: Int,
        ): ConvoTask {
            val freq = ImpulseResponse.SamplingFreq.Companion.valueOf(sampleRateHz)
            val hrirL30L = ImpulseResponse.load(
                context,
                ImpulseResponse.DIRECTION.L30,
                ImpulseResponse.CHANNEL.L,
                freq
            )
            val hrirL30R = ImpulseResponse.load(
                context,
                ImpulseResponse.DIRECTION.L30,
                ImpulseResponse.CHANNEL.R,
                freq
            )
            val hrirR30L = ImpulseResponse.load(
                context,
                ImpulseResponse.DIRECTION.R30,
                ImpulseResponse.CHANNEL.L,
                freq
            )
            val hrirR30R = ImpulseResponse.load(
                context,
                ImpulseResponse.DIRECTION.R30,
                ImpulseResponse.CHANNEL.R,
                freq
            )
            return create(ConvoTask.Config.Stereo(hrirL30L, hrirL30R, hrirR30L, hrirR30R))
        }

        @Suppress("unused")
        private fun ConvoTask.Companion.createCenter(context: Context): ConvoTask {
            val hrirL = ImpulseResponse.load(
                context,
                ImpulseResponse.DIRECTION.C,
                ImpulseResponse.CHANNEL.L,
                ImpulseResponse.SamplingFreq.HZ_44100
            )
            val hrirR = ImpulseResponse.load(
                context,
                ImpulseResponse.DIRECTION.C,
                ImpulseResponse.CHANNEL.R,
                ImpulseResponse.SamplingFreq.HZ_44100
            )
            return create(ConvoTask.Config.Center(hrirL, hrirR))
        }

        @Throws(IOException::class)
        fun ImpulseResponse.Companion.load(
            context: Context,
            dir: ImpulseResponse.DIRECTION,
            ch: ImpulseResponse.CHANNEL,
            freq: ImpulseResponse.SamplingFreq
        ): ImpulseResponse {
            val name = "imp" + dir.name + ch.name + "_" + freq.fileName + "_20k.DDB"
            return load(context, name)
        }

        private val ImpulseResponse.SamplingFreq.fileName: String get() = "${this.freq}"

        @Throws(IOException::class)
        private fun load(context: Context, name: String): ImpulseResponse {
            return load(context.assets.openFd(name))
        }

        @Throws(IOException::class)
        private fun load(afd: AssetFileDescriptor): ImpulseResponse {
            val bb = ByteBuffer.allocate(afd.length.toInt()).order(ByteOrder.LITTLE_ENDIAN)
            val bufSize: Int = afd.createInputStream().channel
                .use { fc -> fc.read(bb) }
            bb.flip()
            val doubleBuf = DoubleArray(bufSize / 8)
            bb.asDoubleBuffer()[doubleBuf]
            return ImpulseResponse(doubleBuf)
        }
    }
}
