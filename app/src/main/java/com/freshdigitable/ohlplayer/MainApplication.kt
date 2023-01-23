package com.freshdigitable.ohlplayer

import android.app.Application
import com.freshdigitable.ohlplayer.store.PlayableItemStore

/**
 * Created by akihit on 2017/05/27.
 */
class MainApplication : Application() {
    val playableItemStore = PlayableItemStore();
}
