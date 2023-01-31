package com.freshdigitable.ohlplayer

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.ohlplayer.databinding.ActivityMediaListBinding
import com.freshdigitable.ohlplayer.databinding.ViewMediaListItemBinding
import com.freshdigitable.ohlplayer.model.PlayableItem
import com.freshdigitable.ohlplayer.store.PlayableItemStore
import com.freshdigitable.ohlplayer.store.uri
import kotlinx.coroutines.flow.collectLatest
import java.io.IOException

class MediaListActivity : AppCompatActivity() {
    private var binding: ActivityMediaListBinding? = null
    private val playableItemStore: PlayableItemStore
        get() = (application as MainApplication).playableItemStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMediaListBinding.inflate(LayoutInflater.from(this))
        this.binding = binding
        setContentView(binding.root)
        val linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.isAutoMeasureEnabled = true
        binding.list.layoutManager = linearLayoutManager

        val adapter = ViewAdapter(playableItemStore)
        binding.list.adapter = adapter
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
                playableItemStore.findLocalMediaFiles()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.list?.adapter = null
    }

    companion object {
        @Suppress("unused")
        private val TAG = MediaListActivity::class.simpleName
    }
}

private class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView
    val artist: TextView
    val icon: ImageView

    init {
        val binding = ViewMediaListItemBinding.bind(itemView)
        title = binding.listTitle
        artist = binding.listArtist
        icon = binding.listIcon
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
        holder.icon.setArtwork(item)
        holder.itemView.setOnClickListener { v: View ->
            MediaPlayerActivity.start(v.context, item)
        }
    }

    override fun onViewRecycled(holder: Holder) {
        super.onViewRecycled(holder)
        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount(): Int = playableItemStore.itemCount

    companion object {
        private fun ImageView.setArtwork(item: PlayableItem) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val thumb = context.contentResolver.loadThumbnail(
                        item.uri,
                        Size(200, 200),
                        null
                    )
                    setImageBitmap(thumb)
                } catch (e: IOException) {
                    setImageResource(R.drawable.ic_audiotrack_black)
                }
            } else {
                setImageResource(R.drawable.ic_audiotrack_black)
            }
        }
    }
}

private val diffUtil = object : DiffUtil.ItemCallback<PlayableItem>() {
    override fun areItemsTheSame(oldItem: PlayableItem, newItem: PlayableItem): Boolean =
        oldItem.path == newItem.path

    override fun areContentsTheSame(oldItem: PlayableItem, newItem: PlayableItem): Boolean =
        oldItem == newItem
}
