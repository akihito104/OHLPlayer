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
import java.util.List;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedCollectionChangeSet.Range;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class MusicListActivity extends AppCompatActivity {

  private RecyclerView listView;
  private Realm playList;
  private RealmResults<MusicItem> musicItems;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_list);

    listView = (RecyclerView) findViewById(R.id.list);
    final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
    linearLayoutManager.setAutoMeasureEnabled(true);
    listView.setLayoutManager(linearLayoutManager);
  }

  @Override
  protected void onStart() {
    super.onStart();
    final RealmConfiguration realmConfig = new RealmConfiguration.Builder()
        .name("play_list")
        .deleteRealmIfMigrationNeeded()
        .build();
    playList = Realm.getInstance(realmConfig);
    addNewMusic();
    musicItems = playList
        .where(MusicItem.class)
        .findAllSorted("path");
    final ViewAdapter adapter = new ViewAdapter(musicItems);
    musicItems.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<MusicItem>>() {
      @Override
      public void onChange(RealmResults<MusicItem> musicItems, OrderedCollectionChangeSet changeSet) {
        for (Range r : changeSet.getInsertionRanges()) {
          adapter.notifyItemRangeInserted(r.startIndex, r.length);
        }
        for (Range r : changeSet.getChangeRanges()) {
          adapter.notifyItemRangeChanged(r.startIndex, r.length);
        }
        for (Range r : changeSet.getDeletionRanges()) {
          adapter.notifyItemRangeRemoved(r.startIndex, r.length);
        }
      }
    });
    listView.setAdapter(adapter);
  }

  private void addNewMusic() {
    final File externalFilesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    final String[] fileList = externalFilesDir.list();
    for (final String fileName : fileList) {
      final String filePath = new File(externalFilesDir, fileName).getAbsolutePath();
      playList.executeTransactionAsync(new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
          final MusicItem item = realm.where(MusicItem.class)
              .equalTo("path", filePath)
              .findFirst();
          if (item == null) {
            final MusicItem musicItem = new MusicItem(filePath, fileName);
            realm.insert(musicItem);
          }
        }
      });
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    musicItems.removeAllChangeListeners();
    listView.setAdapter(null);
    playList.close();
  }

  private static class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.Holder> {
    private final List<MusicItem> fileList;

    ViewAdapter(List<MusicItem> fileList) {
      this.fileList = fileList;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
      final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_music_list_item, parent, false);
      return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
      final MusicItem item = fileList.get(position);
      holder.title.setText(item.getTitle());
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
      return fileList.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
      private final TextView title;

      Holder(View itemView) {
        super(itemView);
        title = (TextView) itemView.findViewById(R.id.list_title);
      }
    }
  }
}
