package com.mal.game_engine.engine.surface

import android.view.SurfaceHolder
import com.mal.game_engine.engine.GameEngine
import com.mal.game_engine.engine.GameEngineRenderer
import com.mal.game_engine.engine.di.DISPATCHER_IO
import com.mal.game_engine.engine.game.GameLoop
import com.mal.game_engine.engine.game.component.GameComponent
import com.mal.game_engine.engine.logI
import com.mal.game_engine.engine.opengl.BaseShape
import com.mal.game_engine.engine.surface.thread.GLThread
import org.koin.core.qualifier.named

class GameEngineGLSurface {

    private var glThread: GLThread? = null
    private var gameLoop: GameLoop? = null

    fun attachSurface(
        shapes: List<BaseShape<out Any>>,
        components: List<GameComponent>,
        holder: SurfaceHolder
    ) {
        val gameEngineRenderer = GameEngineRenderer(
            shapes = shapes,
            gameLoop = GameLoop(
                dispatcher = GameEngine.koinApp.koin.get(named(DISPATCHER_IO)),
                components = components
            ) {
                glThread?.requestRender()
            }.also {
                gameLoop = it
            }
        )
        glThread = GLThread(holder.surface, gameEngineRenderer).apply {
            setRenderMode(GLThread.RENDERMODE_WHEN_DIRTY)
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    this@apply.surfaceCreated()
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    this@apply.onWindowResize(width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    this@apply.surfaceDestroyed()
                }
            })
            start()
        }
    }

    fun onDestroy() {
        gameLoop?.stopGame()
        glThread?.requestReleaseEglContext()
        glThread?.requestExitAndWait()
    }

    fun onPause() {
        gameLoop?.stopGame()
        glThread?.onPause()
    }

    fun onResume() {
        glThread?.onResume()
        gameLoop?.startGame()
    }
}