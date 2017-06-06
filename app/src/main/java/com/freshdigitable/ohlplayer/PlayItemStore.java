package com.freshdigitable.ohlplayer;

import android.media.MediaMetadataRetriever;

import java.io.File;
import java.util.List;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by akihit on 2017/05/27.
 */

public class PlayItemStore {
  private Realm playList;
  private final RealmConfiguration realmConfig = new RealmConfiguration.Builder()
      .name("play_list")
      .deleteRealmIfMigrationNeeded()
      .build();
  private RealmResults<MediaItem> mediaItems;

  public void open() {
    playList = Realm.getInstance(realmConfig);
    mediaItems = playList
        .where(MediaItem.class)
        .findAllSorted("title");
  }

  public void addEventListener(final StoreEventListener l) {
    mediaItems.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<MediaItem>>() {
      @Override
      public void onChange(RealmResults<MediaItem> mediaItems, OrderedCollectionChangeSet changeSet) {
        for (OrderedCollectionChangeSet.Range r : changeSet.getInsertionRanges()) {
          l.onStoreUpdate(EventType.INSERT, r.startIndex, r.length);
        }
        for (OrderedCollectionChangeSet.Range r : changeSet.getChangeRanges()) {
          l.onStoreUpdate(EventType.CHANGE, r.startIndex, r.length);
        }
        for (OrderedCollectionChangeSet.Range r : changeSet.getDeletionRanges()) {
          l.onStoreUpdate(EventType.DELETE, r.startIndex, r.length);
        }
      }
    });
  }

  public void registerIfAbsent(final List<File> files) {
    for (final File file : files) {
      final String path = file.getAbsolutePath();
      final MediaItem item = findByPath(path);
      if (item != null) {
        continue;
      }
      final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
      metadataRetriever.setDataSource(path);
      final String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
      final String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
      metadataRetriever.release();
      playList.executeTransactionAsync(new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
          final MediaItem mediaItem = new MediaItem.Builder(path).title(title).artist(artist).build();
          realm.insert(mediaItem);
        }
      });
    }
  }

  public MediaItem get(int index) {
    return mediaItems.get(index);
  }

  public int getItemCount() {
    return mediaItems == null ? 0 : mediaItems.size();
  }

  public void close() {
    mediaItems.removeAllChangeListeners();
    playList.close();
  }

  public MediaItem findByPath(String path) {
    return playList.where(MediaItem.class)
        .equalTo("path", path)
        .findFirst();
  }

  public enum EventType {
    INSERT, CHANGE, DELETE
  }

  public interface StoreEventListener {
    void onStoreUpdate(EventType type, int range, int length);
  }
}
