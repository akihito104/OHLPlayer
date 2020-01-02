package com.freshdigitable.ohlplayer.store;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by akihit on 2017/05/27.
 */

public class PlayableItemStore {
  private Realm playList;
  private final RealmConfiguration realmConfig = new RealmConfiguration.Builder()
      .name("play_list")
      .deleteRealmIfMigrationNeeded()
      .build();
  private RealmResults<PlayableItemRealm> mediaItems;

  public void open() {
    playList = Realm.getInstance(realmConfig);
    mediaItems = playList
        .where(PlayableItemRealm.class)
        .findAll()
        .sort("title");
  }

  public void addEventListener(@NonNull final StoreEventListener l) {
    mediaItems.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<PlayableItemRealm>>() {
      @Override
      public void onChange(RealmResults<PlayableItemRealm> mediaItems, OrderedCollectionChangeSet changeSet) {
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

  public void registerIfAbsent(@NonNull final List<PlayableItem> items) {
    if (items.isEmpty()) {
      return;
    }

    final List<PlayableItemRealm> insertedItems = new ArrayList<>();
    for (PlayableItem i : items) {
      if (findByPath(i.getPath()) != null) {
        continue;
      }
      insertedItems.add(new PlayableItemRealm(i));
    }
    if (insertedItems.isEmpty()) {
      return;
    }

    playList.executeTransactionAsync(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insert(insertedItems);
      }
    });
  }

  public PlayableItem get(int index) {
    return mediaItems.get(index);
  }

  public int getItemCount() {
    return mediaItems == null ? 0 : mediaItems.size();
  }

  public void close() {
    mediaItems.removeAllChangeListeners();
    playList.close();
  }

  public PlayableItem findByPath(String path) {
    return playList.where(PlayableItemRealm.class)
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
