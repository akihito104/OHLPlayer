package com.freshdigitable.ohlplayer.store

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.freshdigitable.ohlplayer.model.PlayableItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import java.io.File

/**
 * Created by akihit on 2017/05/27.
 */
class PlayableItemStore(
    private val applicationContext: Context,
) {
    private var mediaItems = MutableStateFlow<List<PlayableItem>>(emptyList())

    fun getMediaItems(): Flow<List<PlayableItem>> {
        if (mediaItems.value.isEmpty()) {
            findLocalMediaFiles()
        }
        return mediaItems
    }

    fun findLocalMediaFiles(): List<PlayableItem> {
        val playableItems = listOf(Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_MOVIES)
            .flatMap { findPlayableItems(it) }
        registerIfAbsent(playableItems)
        return playableItems
    }

    private fun registerIfAbsent(items: List<PlayableItem>) {
        if (items.isEmpty()) {
            return
        }
        val insertedItems = items.filter { findByPath(it.path) == null }
        if (insertedItems.isEmpty()) {
            return
        }
        mediaItems.getAndUpdate { (it.toSet() + insertedItems.toSet()).toList() }
    }

    private fun findPlayableItems(type: String): List<PlayableItem> {
        val files = findMediaFiles(type)
        if (files.isEmpty()) {
            return emptyList()
        }
        return MediaMetadataRetriever().use { metadataRetriever ->
            files.map { uri ->
                metadataRetriever.setDataSource(applicationContext, uri)
                val title =
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist =
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val path = uri.toString()
                PlayableItem.create(
                    path = path,
                    title = if (title.isNullOrEmpty()) File(path).name else title,
                    artist = artist,
                )
            }
        }
    }

    private fun findMediaFiles(type: String): List<Uri> {
        Log.d(TAG, "findPlayableFiles: $type")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getFileListQ(type)
        } else {
            getFileList(type)
        }
    }

    private fun getFileList(type: String): List<Uri> {
        val externalFilesDir = Environment.getExternalStoragePublicDirectory(type)
        val fileList = externalFilesDir.list() ?: return emptyList()
        return fileList.map { File(externalFilesDir, it) }
            .filter { it.isFile }
            .filter { findByPath(it.absolutePath) == null }
            .map { Uri.fromFile(it) }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun getFileListQ(type: String): List<Uri> {
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (type) {
                Environment.DIRECTORY_MUSIC ->
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                Environment.DIRECTORY_MOVIES ->
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else -> throw IllegalArgumentException()
            }
        } else {
            when (type) {
                Environment.DIRECTORY_MUSIC -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                Environment.DIRECTORY_MOVIES -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> throw IllegalArgumentException()
            }
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        applicationContext.contentResolver.query(
            collection,
            projection,
            null,
            null,
            null
        ).use { cursor ->
            // Cache column indices.
            val idColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val relativePathColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val files = mutableListOf<Uri>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val relativePath = cursor.getString(relativePathColumn)
                if (relativePath.startsWith("Music/") || relativePath.startsWith("Video/")) {
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    Log.d(TAG, "getFileListQ: $contentUri, $relativePath")
                    files.add(contentUri)
                }
            }
            return files
        }
    }

    operator fun get(index: Int): PlayableItem = mediaItems.value[index]

    val itemCount: Int
        get() = mediaItems.value.size

    fun findByPath(path: String): PlayableItem? = mediaItems.value.firstOrNull { it.path == path }

    companion object {
        @Suppress("unused")
        private val TAG = PlayableItemStore::class.simpleName
    }
}
