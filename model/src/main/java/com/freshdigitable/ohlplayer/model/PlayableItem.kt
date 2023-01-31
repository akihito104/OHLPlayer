package com.freshdigitable.ohlplayer.model

/**
 * Created by akihit on 2017/06/06.
 */
interface PlayableItem : Comparable<PlayableItem?> {
    val path: String
    val title: String?
    val artist: String?

    override fun compareTo(other: PlayableItem?): Int {
        if (other == null) {
            return -1
        }
        if (title == null && other.title == null) {
            return 0
        }
        return title?.compareTo(other.title ?: "") ?: -1
    }

    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean

    companion object
}
