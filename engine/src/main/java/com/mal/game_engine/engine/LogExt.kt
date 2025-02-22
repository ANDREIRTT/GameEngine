package com.mal.game_engine.engine

import android.util.Log

internal fun Any.logI(message: String) {
    if (BuildConfig.DEBUG) {
        Log.i("GameEngine ${this::class.java.simpleName}", message)
    }
}