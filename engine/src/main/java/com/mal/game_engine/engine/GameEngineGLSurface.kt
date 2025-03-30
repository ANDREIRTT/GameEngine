package com.mal.game_engine.engine

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.mal.game_engine.engine.coordinate.Coordinate
import com.mal.game_engine.engine.di.DISPATCHER_IO
import com.mal.game_engine.engine.di.IsolatedKoinComponent
import com.mal.game_engine.engine.game.GameLoop
import com.mal.game_engine.engine.game.component.GameComponent
import com.mal.game_engine.engine.opengl.BaseShape
import org.koin.core.component.get
import org.koin.core.qualifier.named
import kotlin.math.abs

class GameEngineGLSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs), IsolatedKoinComponent {

    private val touchPoints = mutableMapOf<Int, Coordinate>()
    private val initialTouch = mutableMapOf<Int, Pair<Coordinate, Long>>()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
    }

    private lateinit var gameEngineRenderer: GameEngineRenderer

    fun create(
        shapes: List<BaseShape<out Any>>,
        components: List<GameComponent>
    ) {
        gameEngineRenderer = GameEngineRenderer(
            shapes = shapes,
            gameLoop = GameLoop(
                dispatcher = get(named(DISPATCHER_IO)),
                components = components
            ) {
                requestRender()
            }
        )
        setRenderer(gameEngineRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val coordinate = Coordinate(
                    event.getX(pointerIndex).toDouble(),
                    event.getY(pointerIndex).toDouble()
                )
                initialTouch[pointerId] = coordinate to System.currentTimeMillis()
                touchPoints[pointerId] = coordinate
                gameEngineRenderer.onTouch(coordinate)
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val (initial) = initialTouch[id] ?: continue
                    val currentX = event.getX(i).toDouble()
                    val currentY = event.getY(i).toDouble()

                    val dx = abs(currentX - initial.x)
                    val dy = abs(currentY - initial.y)
                    val isMovement = dx > touchSlop || dy > touchSlop
                    if (isMovement) {
                        touchPoints[id]?.let {
                            val coordinate = Coordinate(
                                event.getX(i).toDouble(),
                                event.getY(i).toDouble()
                            )
                            touchPoints[id] = coordinate
                            gameEngineRenderer.onTouch(coordinate)
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.pointerCount == 1) {
                    val delay = initialTouch[pointerId]?.second ?: 0
                    if (System.currentTimeMillis() - delay >= 100) {
                        gameEngineRenderer.onStop(
                            Coordinate(
                                event.x.toDouble(),
                                event.y.toDouble()
                            )
                        )
                    } else {
                        gameEngineRenderer.onClick(
                            Coordinate(
                                event.x.toDouble(),
                                event.y.toDouble()
                            )
                        )
                    }
                }
                initialTouch.remove(pointerId)
                touchPoints.remove(pointerId)
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        stopGame()
        super.onDetachedFromWindow()
    }

    fun stopGame() {
        gameEngineRenderer.gameLoop.stopGame()
    }

    fun startGame() = with(gameEngineRenderer.gameLoop) {
        if (!isRunning) {
            startGame()
        }
    }
}