package com.mal.game_engine.engine.surface

import android.view.SurfaceHolder

class GameEngineGLSurface {

    private val eglManager = EglHelper()

    fun attachSurface(holder: SurfaceHolder) {
        eglManager.initializeEgl(holder.surface)
    }
}