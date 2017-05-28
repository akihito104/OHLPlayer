package com.freshdigitable.ohlplayer;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.Serializable;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by akihit on 2017/04/28.
 */
@RealmClass
public class MusicItem implements Serializable, Comparable<MusicItem>, RealmModel {
  @PrimaryKey
  private String path;
  private String title;
  private String artist;

  public MusicItem() { }

  public String getPath() {
    return path;
  }

  public Uri getUri() {
    return Uri.parse(path);
  }

  public String getTitle() {
    return title;
  }

  public String getArtist() {
    return artist;
  }

  private MusicItem(Builder builder) {
    this.path = builder.path;
    this.title = builder.title;
    this.artist = builder.artist;
  }

  @Override
  public int compareTo(@NonNull MusicItem o) {
    return this.title.compareTo(o.title);
  }

  public static class Builder {
    private String path;
    private String title;
    private String artist;

    public Builder(@NonNull String path) {
      this.path = path;
    }

    Builder title(String title) {
      this.title = title;
      return this;
    }

    Builder artist(String artist) {
      this.artist = artist;
      return this;
    }

    MusicItem build() {
      return new MusicItem(this);
    }
  }
}
