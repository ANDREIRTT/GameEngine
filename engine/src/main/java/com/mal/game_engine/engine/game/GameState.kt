package com.mal.game_engine.engine.game

import kotlin.concurrent.Volatile

object GameState {

    @Volatile
    var screenRatio: Double = 0.0

    @Volatile
    var screenWidth: Double = 0.0

    @Volatile
    var screenHeight: Double = 0.0

    @Volatile
    var vpMatrix = FloatArray(16)
}