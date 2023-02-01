package com.freshdigitable.ohlplayer.model

/**
 * Created by akihit on 2017/05/14.
 */
interface ConvoTask {
    fun convo(input: ShortArray): AudioChannels
    fun release()

    sealed class Config {
        data class Stereo(
            internal val hrirL30L: ImpulseResponse, internal val hrirL30R: ImpulseResponse,
            internal val hrirR30L: ImpulseResponse, internal val hrirR30R: ImpulseResponse,
        ) : Config()

        data class Center(
            internal val hrirL: ImpulseResponse,
            internal val hrirR: ImpulseResponse,
        ) : Config()
    }

    companion object {
        fun create(config: Config): ConvoTask {
            return when (config) {
                is Config.Stereo -> StereoHRTFConvoTask(
                    hrirL30L = config.hrirL30L,
                    hrirL30R = config.hrirL30R,
                    hrirR30L = config.hrirR30L,
                    hrirR30R = config.hrirR30R,
                )
                is Config.Center -> CenterHRTFConvoTask(hrirL = config.hrirL, hrirR = config.hrirR)
            }
        }
    }
}
