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
import java.util.Collections;
import java.util.List;

public class MusicListActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_music_list);

    final File externalFilesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    final String[] fileList = externalFilesDir.list();
    final ArrayList<MusicItem> musicItems = new ArrayList<>();
    for (String s : fileList) {
      musicItems.add(new MusicItem(externalFilesDir, s));
    }

    RecyclerView listView = (RecyclerView) findViewById(R.id.list);
    final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
    linearLayoutManager.setAutoMeasureEnabled(true);
    listView.setLayoutManager(linearLayoutManager);
    listView.setAdapter(new ViewAdapter(musicItems));
  }

  private static class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.Holder> {
    private final List<MusicItem> fileList;

    ViewAdapter(List<MusicItem> fileList) {
      this.fileList = fileList;
      Collections.sort(this.fileList);
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
    public void onViewDetachedFromWindow(Holder holder) {
      super.onViewDetachedFromWindow(holder);
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
