package com.freshdigitable.ohlplayer;

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
  private RealmResults<MusicItem> musicItems;

  public void open() {
    playList = Realm.getInstance(realmConfig);
    musicItems = playList
        .where(MusicItem.class)
        .findAllSorted("path");
  }

  public void addEventListener(final StoreEventListener l) {
    musicItems.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<MusicItem>>() {
      @Override
      public void onChange(RealmResults<MusicItem> musicItems, OrderedCollectionChangeSet changeSet) {
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
      final MusicItem item = findByPath(path);
      if (item != null) {
        continue;
      }
      playList.executeTransactionAsync(new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
          final MusicItem musicItem = new MusicItem(path, file.getName());
          realm.insert(musicItem);
        }
      });
    }
  }

  public MusicItem get(int index) {
    return musicItems.get(index);
  }

  public int getItemCount() {
    return musicItems == null ? 0 : musicItems.size();
  }

  public void close() {
    musicItems.removeAllChangeListeners();
    playList.close();
  }

  public MusicItem findByPath(String path) {
    return playList.where(MusicItem.class)
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
