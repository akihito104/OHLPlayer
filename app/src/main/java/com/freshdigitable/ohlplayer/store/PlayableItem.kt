package com.freshdigitable.ohlplayer.store

import android.net.Uri

/**
 * Created by akihit on 2017/06/06.
 */
interface PlayableItem : Comparable<PlayableItem?> {
    val path: String
    val title: String?
    val artist: String?
    val uri: Uri

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

    class Builder(var path: String) {
        var title: String? = null
        var artist: String? = null
        fun title(title: String?): Builder {
            this.title = title
            return this
        }

        fun artist(artist: String?): Builder {
            this.artist = artist
            return this
        }

        fun build(): PlayableItem {
            return PlayableItemImpl(this)
        }
    }
}
