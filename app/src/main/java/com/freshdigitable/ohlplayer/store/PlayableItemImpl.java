package com.freshdigitable.ohlplayer.store;

import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Created by akihit on 2017/06/06.
 */

class PlayableItemImpl implements PlayableItem {
  private String path;
  private String title;
  private String artist;

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public Uri getUri(){
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
    return title.compareTo(o.getTitle());
  }

  PlayableItemImpl(Builder builder) {
    this.path = builder.path;
    this.title = builder.title;
    this.artist = builder.artist;
  }
}
