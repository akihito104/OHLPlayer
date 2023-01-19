package com.freshdigitable.ohlplayer;

import android.app.Application;

import io.realm.Realm;

/**
 * Created by akihit on 2017/05/27.
 */

public class MainApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    Realm.init(this);
  }
}
