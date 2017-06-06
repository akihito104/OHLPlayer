package com.freshdigitable.ohlplayer.store;

import android.net.Uri;
import android.support.annotation.NonNull;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by akihit on 2017/04/28.
 */
@RealmClass
public class PlayableItemRealm implements PlayableItem, RealmModel {
  @PrimaryKey
  private String path;
  private String title;
  private String artist;

  public PlayableItemRealm() { }

  PlayableItemRealm(PlayableItem item) {
    this.path = item.getPath();
    this.title = item.getTitle();
    this.artist = item.getArtist();
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public Uri getUri() {
    return Uri.parse(path);
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getArtist() {
    return artist;
  }

  @Override
  public int compareTo(@NonNull PlayableItem o) {
    return this.title.compareTo(o.getTitle());
  }
}
