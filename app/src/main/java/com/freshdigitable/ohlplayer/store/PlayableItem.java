package com.freshdigitable.ohlplayer.store;

import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Created by akihit on 2017/06/06.
 */

public interface PlayableItem extends Comparable<PlayableItem> {
  String getPath();

  String getTitle();

  String getArtist();

  Uri getUri();

  class Builder {
    String path;
    String title;
    String artist;

    public Builder(@NonNull String path) {
      this.path = path;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder artist(String artist) {
      this.artist = artist;
      return this;
    }

    public PlayableItem build() {
      return new PlayableItemImpl(this);
    }
  }
}
