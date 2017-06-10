package com.freshdigitable.ohlplayer;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import io.realm.Realm;

/**
 * Created by akihit on 2017/05/27.
 */

public class MainApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);

    Realm.init(getApplicationContext());
  }
}
