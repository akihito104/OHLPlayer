package com.freshdigitable.ohlplayer;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicListActivity extends AppCompatActivity {

  private RecyclerView listView;
  private PlayItemStore playItemStore;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_list);

    listView = (RecyclerView) findViewById(R.id.list);
    final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
    linearLayoutManager.setAutoMeasureEnabled(true);
    listView.setLayoutManager(linearLayoutManager);
    playItemStore = new PlayItemStore();
  }

  @Override
  protected void onStart() {
    super.onStart();
    playItemStore.open();
    final ViewAdapter adapter = new ViewAdapter(playItemStore);
    addNewMusic();
    listView.setAdapter(adapter);
  }

  private void addNewMusic() {
    final File externalFilesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    final String[] fileList = externalFilesDir.list();
    List<File> files = new ArrayList<>(fileList.length);
    for (String name : fileList) {
      files.add(new File(externalFilesDir, name));
    }
    playItemStore.registerIfAbsent(files);
  }

  @Override
  protected void onStop() {
    super.onStop();
    listView.setAdapter(null);
    playItemStore.close();
  }

  private static class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.Holder> {
    private final PlayItemStore playItemStore;

    ViewAdapter(PlayItemStore store) {
      this.playItemStore = store;
      this.playItemStore.addEventListener(new PlayItemStore.StoreEventListener() {
        @Override
        public void onStoreUpdate(PlayItemStore.EventType type, int range, int length) {
          if (type == PlayItemStore.EventType.INSERT) {
            notifyItemRangeInserted(range, length);
          } else if (type == PlayItemStore.EventType.CHANGE) {
            notifyItemRangeChanged(range, length);
          } else if (type == PlayItemStore.EventType.DELETE) {
            notifyItemRangeRemoved(range, length);
          }
        }
      });
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
      final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_music_list_item, parent, false);
      return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
      final MusicItem item = playItemStore.get(position);
      holder.title.setText(item.getTitle());
      holder.artist.setText(item.getArtist());
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          MusicPlayerActivity.start(v.getContext(), item);
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
      return playItemStore.getItemCount();
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
