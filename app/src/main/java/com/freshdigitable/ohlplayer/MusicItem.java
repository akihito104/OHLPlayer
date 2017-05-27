package com.freshdigitable.ohlplayer;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.Serializable;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by akihit on 2017/04/28.
 */
@RealmClass
public class MusicItem implements Serializable, Comparable<MusicItem>, RealmModel {
  private String title;
  @PrimaryKey
  private String path;

  public MusicItem() { }

  public MusicItem(String path, String title) {
    this.path = path;
    this.title = title;
  }

  public MusicItem(File parent, String file) {
    this(parent, file, file);
  }

  public MusicItem(File parent, String file, String title) {
    this(new File(parent, file).getAbsolutePath(), title);

  }

  public String getPath() {
    return path;
  }

  public Uri getUri() {
    return Uri.parse(path);
  }

  public String getTitle() {
    return title;
  }

  @Override
  public int compareTo(@NonNull MusicItem o) {
    return this.title.compareTo(o.title);
  }
}
