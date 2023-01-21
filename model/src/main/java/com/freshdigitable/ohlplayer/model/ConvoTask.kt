package com.freshdigitable.ohlplayer.model

/**
 * Created by akihit on 2017/05/14.
 */
interface ConvoTask {
    fun convo(input: ShortArray): AudioChannels
    fun release()
}
