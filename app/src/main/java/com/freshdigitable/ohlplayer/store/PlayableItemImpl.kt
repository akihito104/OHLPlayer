package com.freshdigitable.ohlplayer.store

import android.net.Uri
import com.freshdigitable.ohlplayer.model.PlayableItem

/**
 * Created by akihit on 2017/06/06.
 */
private data class PlayableItemImpl(
    override val path: String,
    override val title: String?,
    override val artist: String?,
) : PlayableItem

internal val PlayableItem.uri get() = Uri.parse(path)

internal fun PlayableItem.Companion.create(
    path: String,
    title: String?,
    artist: String?
): PlayableItem = PlayableItemImpl(path, title, artist)
