package com.freshdigitable.ohlplayer;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.Serializable;

/**
 * Created by akihit on 2017/04/28.
 */

public class MusicItem implements Serializable, Comparable<MusicItem> {
  private final Uri uri;
  private final String title;

  public MusicItem(Uri uri, String title) {
    this.uri = uri;
    this.title = title;
  }

  public MusicItem(File parent, String file) {
    this(parent, file, file);
  }

  public MusicItem(File parent, String file, String title) {
    this(Uri.parse(new File(parent, file).getAbsolutePath()), title);

  }

  public Uri getUri() {
    return uri;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public int compareTo(@NonNull MusicItem o) {
    return this.title.compareTo(o.title);
  }
}
