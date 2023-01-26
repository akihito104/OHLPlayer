package com.freshdigitable.ohlplayer.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Created by akihit on 2017/05/27.
 */
class PlayableItemStore {
    private var mediaItems = MutableStateFlow<List<PlayableItem>>(emptyList())
    fun open() {
    }

    fun getMediaItems(): Flow<List<PlayableItem>> {
        return mediaItems
    }

    fun registerIfAbsent(items: List<PlayableItem>) {
        if (items.isEmpty()) {
            return
        }
        val insertedItems = items.filter { findByPath(it.path) == null }
        if (insertedItems.isEmpty()) {
            return
        }
        mediaItems.getAndUpdate { (it.toSet() + insertedItems.toSet()).toList() }
    }

    operator fun get(index: Int): PlayableItem {
        return mediaItems.value[index]
    }

    val itemCount: Int
        get() = mediaItems.value.size

    fun close() {
    }

    fun findByPath(path: String): PlayableItem? {
        return mediaItems.value.firstOrNull { it.path == path }
    }
}
