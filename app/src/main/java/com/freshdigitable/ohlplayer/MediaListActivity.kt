package com.freshdigitable.ohlplayer

import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.ohlplayer.store.PlayableItem
import com.freshdigitable.ohlplayer.store.PlayableItemStore
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class MediaListActivity : AppCompatActivity() {
    private var listView: RecyclerView? = null
    private val playableItemStore: PlayableItemStore
        get() = (application as MainApplication).playableItemStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_list)
        listView = findViewById(R.id.list)
        val linearLayoutManager = LinearLayoutManager(
            applicationContext
        )
        linearLayoutManager.isAutoMeasureEnabled = true
        listView?.layoutManager = linearLayoutManager
    }

    override fun onStart() {
        super.onStart()
        playableItemStore.open()
        val adapter = ViewAdapter(playableItemStore)
        addLocalMediaFiles()
        listView!!.adapter = adapter
        lifecycleScope.launchWhenCreated {
            playableItemStore.getMediaItems().collectLatest {
                adapter.submitList(it)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLocalMediaFiles()
            }
        }
    }

    private fun addLocalMediaFiles() {
        val playableItems = listOf(Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_MOVIES)
            .flatMap { findPlayableItems(it) }
        playableItemStore.registerIfAbsent(playableItems)
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
                PlayableItem.Builder(path)
                    .title(if (TextUtils.isEmpty(title)) File(path).name else title)
                    .artist(artist)
                    .build()
            }
        }
    }

    private fun findMediaFiles(type: String): List<Uri> {
        Log.d("MediaListActivity", "findPlayableFiles: $type")
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
            .filter { playableItemStore.findByPath(it.absolutePath) == null }
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
                    Log.d("MediaListActivity", "getFileListQ: $contentUri, $relativePath")
                    files.add(contentUri)
                }
            }
            return files
        }
    }

    override fun onStop() {
        super.onStop()
        listView!!.adapter = null
        playableItemStore.close()
    }
}

private class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView
    val artist: TextView

    init {
        title = itemView.findViewById(R.id.list_title)
        artist = itemView.findViewById(R.id.list_artist)
    }
}

private class ViewAdapter(
    private val playableItemStore: PlayableItemStore
) : ListAdapter<PlayableItem, Holder>(diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_media_list_item, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = playableItemStore[position]
        holder.title.text = item.title
        holder.artist.text = item.artist
        holder.itemView.setOnClickListener { v: View ->
            MediaPlayerActivity.start(v.context, item)
        }
    }

    override fun onViewRecycled(holder: Holder) {
        super.onViewRecycled(holder)
        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount(): Int {
        return playableItemStore.itemCount
    }
}

private val diffUtil = object : DiffUtil.ItemCallback<PlayableItem>() {
    override fun areItemsTheSame(oldItem: PlayableItem, newItem: PlayableItem): Boolean =
        oldItem.path == newItem.path

    override fun areContentsTheSame(oldItem: PlayableItem, newItem: PlayableItem): Boolean =
        oldItem == newItem
}
