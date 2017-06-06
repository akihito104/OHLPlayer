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
public class MediaItem implements Serializable, Comparable<MediaItem>, RealmModel {
  @PrimaryKey
  private String path;
  private String title;
  private String artist;

  public MediaItem() { }

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

  private MediaItem(Builder builder) {
    this.path = builder.path;
    this.title = builder.title;
    this.artist = builder.artist;
  }

  @Override
  public int compareTo(@NonNull MediaItem o) {
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

    MediaItem build() {
      return new MediaItem(this);
    }
  }
}
