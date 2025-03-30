package com.mal.game_engine.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.mal.game_engine.engine.coordinate.Coordinate
import com.mal.game_engine.engine.game.GameLoop
import com.mal.game_engine.engine.game.GameState
import com.mal.game_engine.engine.game.ext.init
import com.mal.game_engine.engine.game.ext.onScreenChanged
import com.mal.game_engine.engine.game.ext.render
import com.mal.game_engine.engine.opengl.BaseShape
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class GameEngineRenderer(
    private val shapes: List<BaseShape<out Any>>,
    val gameLoop: GameLoop
) : GLSurfaceView.Renderer {

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        gameLoop.stopGame()
        shapes.forEach { it.create() }
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        val isFirstRun = GameState.screenRatio == 0.0
        GLES30.glViewport(0, 0, width, height)
        GameState.screenRatio = width.toDouble() / height.toDouble()
        GameState.screenWidth = width.toDouble()
        GameState.screenHeight = height.toDouble()

        if (isFirstRun) {
            gameLoop.components.init()
        } else {
            gameLoop.components.onScreenChanged()
        }
        gameLoop.startGame()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(
            projectionMatrix, 0, -GameState.screenRatio.toFloat(),
            GameState.screenRatio.toFloat(), -1f, 1f, 3f, 7f
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
//         Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GameState.vpMatrix = vPMatrix
        gameLoop.components.render()
        gameLoop.onFrameAvailable()
    }

    fun onTouch(pixelCoordinate: Coordinate) {
        gameLoop.onDragChanged(
            pixelCoordinate.fromPixelsToOpenGl(
                GameState.screenWidth,
                GameState.screenHeight
            )
        )
    }

    fun onStop(pixelCoordinate: Coordinate) {
        gameLoop.onDragFinish(
            pixelCoordinate.fromPixelsToOpenGl(
                GameState.screenRatio,
                GameState.screenRatio
            )
        )
    }

    fun onClick(pixelCoordinate: Coordinate) {
        gameLoop.onClick(
            pixelCoordinate.fromPixelsToOpenGl(
                GameState.screenWidth,
                GameState.screenHeight
            )
        )
    }
}