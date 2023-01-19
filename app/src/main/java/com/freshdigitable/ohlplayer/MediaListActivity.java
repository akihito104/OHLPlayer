package com.freshdigitable.ohlplayer;

import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.freshdigitable.ohlplayer.store.PlayableItem;
import com.freshdigitable.ohlplayer.store.PlayableItemStore;

import java.io.File;
import java.io.IOException;
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

    listView = findViewById(R.id.list);
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
    addLocalMediaFiles();
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
      final List<PlayableItem> items = findPlayableItems(type);
      playableItems.addAll(items);
    }
    playableItemStore.registerIfAbsent(playableItems);
  }

  private List<PlayableItem> findPlayableItems(String type) {
    final List<Uri> files = findMediaFiles(type);
    if (files.isEmpty()) {
      return Collections.emptyList();
    }

    try (final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever()) {
      final List<PlayableItem> items = new ArrayList<>();
      for (final Uri uri : files) {
        metadataRetriever.setDataSource(getApplicationContext(), uri);
        final String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        final String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        final String path = uri.toString();
        final PlayableItem item = new PlayableItem.Builder(path)
                .title(TextUtils.isEmpty(title) ? new File(path).getName() : title)
                .artist(artist)
                .build();
        items.add(item);
      }
      return items;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NonNull
  private List<Uri> findMediaFiles(String type) {
    Log.d("MediaListActivity", "findPlayableFiles: " + type);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      return getFileListQ(type);
    } else {
      return getFileList(type);
    }
  }

  @NonNull
  private List<Uri> getFileList(String type) {
    final File externalFilesDir = Environment.getExternalStoragePublicDirectory(type);
    final String[] fileList = externalFilesDir.list();
    if (fileList == null) {
      return Collections.emptyList();
    }
    final List<Uri> files = new ArrayList<>();
    for (String name : fileList) {
      Log.d("MediaListActivity", "findPlayableFiles> file: " + name);
      final File f = new File(externalFilesDir, name);
      if (f.isDirectory()) {
        continue;
      }
      final String path = f.getAbsolutePath();
      if (playableItemStore.findByPath(path) == null) {
        files.add(Uri.fromFile(f));
      }
    }
    return files;
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  private List<Uri> getFileListQ(String type) {
    final Uri collection;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (Environment.DIRECTORY_MUSIC.equals(type)) {
        collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
      } else if (Environment.DIRECTORY_MOVIES.equals(type)) {
        collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
      } else {
        throw new IllegalArgumentException();
      }
    } else {
      if (Environment.DIRECTORY_MUSIC.equals(type)) {
        collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
      } else if (Environment.DIRECTORY_MOVIES.equals(type)) {
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
      } else {
        throw new IllegalArgumentException();
      }
    }

    final String[] projection = new String[] {
            MediaStore.Video.Media._ID,
            MediaStore.MediaColumns.RELATIVE_PATH,
    };
    try (final Cursor cursor = getApplicationContext().getContentResolver().query(
            collection,
            projection,
            null,
            null,
            null
    )) {
      // Cache column indices.
      final int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
      final int relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH);
      final List<Uri> files = new ArrayList<>();
      while (cursor.moveToNext()) {
        final long id = cursor.getLong(idColumn);
        final String relativePath = cursor.getString(relativePathColumn);
        if (relativePath.startsWith("Music/") || relativePath.startsWith("Video/")) {
          final Uri contentUri = ContentUris.withAppendedId(collection, id);
          Log.d("MediaListActivity", "getFileListQ: " + contentUri.toString() +
                  ", " + relativePath);
          files.add(contentUri);
        }
      }
      return files;
    }
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
      this.playableItemStore.addEventListener((type, range, length) -> {
        if (type == PlayableItemStore.EventType.INSERT) {
          notifyItemRangeInserted(range, length);
        } else if (type == PlayableItemStore.EventType.CHANGE) {
          notifyItemRangeChanged(range, length);
        } else if (type == PlayableItemStore.EventType.DELETE) {
          notifyItemRangeRemoved(range, length);
        }
      });
    }

    @NonNull
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
      holder.itemView.setOnClickListener(v -> MediaPlayerActivity.start(v.getContext(), item));
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
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
        title = itemView.findViewById(R.id.list_title);
        artist = itemView.findViewById(R.id.list_artist);
      }
    }
  }
}
