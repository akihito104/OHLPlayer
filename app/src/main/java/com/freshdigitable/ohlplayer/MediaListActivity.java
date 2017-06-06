package com.freshdigitable.ohlplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.freshdigitable.ohlplayer.store.PlayableItemStore;
import com.freshdigitable.ohlplayer.store.PlayableItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MediaListActivity extends AppCompatActivity {

  private RecyclerView listView;
  private PlayableItemStore playableItemStore;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_list);

    listView = (RecyclerView) findViewById(R.id.list);
    final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
    linearLayoutManager.setAutoMeasureEnabled(true);
    listView.setLayoutManager(linearLayoutManager);
    playableItemStore = new PlayableItemStore();
  }

  @Override
  protected void onStart() {
    super.onStart();
    playableItemStore.open();
    final ViewAdapter adapter = new ViewAdapter(playableItemStore);
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) {
      addLocalMediaFiles();
    } else {
      if (!ActivityCompat.shouldShowRequestPermissionRationale(
          this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
            100);
      }
    }
    listView.setAdapter(adapter);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 100) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        addLocalMediaFiles();
      }
    }
  }

  private void addLocalMediaFiles() {
    final List<PlayableItem> playableItems = new ArrayList<>();
    for (String type : Arrays.asList(Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_MOVIES)) {
      final List<PlayableItem> items = findNewMediaFiles(type);
      playableItems.addAll(items);
    }
    playableItemStore.registerIfAbsent(playableItems);
  }

  private List<PlayableItem> findNewMediaFiles(String type) {
    final File externalFilesDir = Environment.getExternalStoragePublicDirectory(type);
    final String[] fileList = externalFilesDir.list();
    final ArrayList<String> files = new ArrayList<>();
    for (String name : fileList) {
      final File f = new File(externalFilesDir, name);
      final String path = f.getAbsolutePath();
      if (playableItemStore.findByPath(path) == null) {
        files.add(path);
      }
    }
    if (files.isEmpty()) {
      return Collections.emptyList();
    }

    final List<PlayableItem> items = new ArrayList<>();
    for (String path : files) {
      final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
      metadataRetriever.setDataSource(path);
      final String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
      final String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
      metadataRetriever.release();
      final PlayableItem item = new PlayableItem.Builder(path)
          .title(title)
          .artist(artist)
          .build();
      items.add(item);
    }
    return items;
  }

  @Override
  protected void onStop() {
    super.onStop();
    listView.setAdapter(null);
    playableItemStore.close();
  }

  private static class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.Holder> {
    private final PlayableItemStore playableItemStore;

    ViewAdapter(PlayableItemStore store) {
      this.playableItemStore = store;
      this.playableItemStore.addEventListener(new PlayableItemStore.StoreEventListener() {
        @Override
        public void onStoreUpdate(PlayableItemStore.EventType type, int range, int length) {
          if (type == PlayableItemStore.EventType.INSERT) {
            notifyItemRangeInserted(range, length);
          } else if (type == PlayableItemStore.EventType.CHANGE) {
            notifyItemRangeChanged(range, length);
          } else if (type == PlayableItemStore.EventType.DELETE) {
            notifyItemRangeRemoved(range, length);
          }
        }
      });
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
      final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_media_list_item, parent, false);
      return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
      final PlayableItem item = playableItemStore.get(position);
      holder.title.setText(item.getTitle());
      holder.artist.setText(item.getArtist());
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          MediaPlayerActivity.start(v.getContext(), item);
        }
      });
    }

    @Override
    public void onViewRecycled(Holder holder) {
      super.onViewRecycled(holder);
      holder.itemView.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
      return playableItemStore.getItemCount();
    }

    static class Holder extends RecyclerView.ViewHolder {
      private final TextView title;
      private final TextView artist;

      Holder(View itemView) {
        super(itemView);
        title = (TextView) itemView.findViewById(R.id.list_title);
        artist = (TextView) itemView.findViewById(R.id.list_artist);
      }
    }
  }
}
