package com.freshdigitable.ohlplayer.store

import android.net.Uri

/**
 * Created by akihit on 2017/06/06.
 */
internal data class PlayableItemImpl(
    override val path: String,
    override val title: String?,
    override val artist: String?,
) : PlayableItem {
    override val uri: Uri
        get() = Uri.parse(path)

    constructor(builder: PlayableItem.Builder) : this(
        path = builder.path,
        title = builder.title,
        artist = builder.artist,
    )
}
