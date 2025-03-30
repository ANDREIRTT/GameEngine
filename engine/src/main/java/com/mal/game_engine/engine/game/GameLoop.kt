package com.mal.game_engine.engine.game

import android.util.Log
import com.mal.game_engine.engine.coordinate.Coordinate
import com.mal.game_engine.engine.game.component.GameComponent
import com.mal.game_engine.engine.game.ext.calculate
import com.mal.game_engine.engine.game.ext.onClick
import com.mal.game_engine.engine.game.ext.onDragChanged
import com.mal.game_engine.engine.game.ext.onDragFinish
import com.mal.game_engine.engine.game.ext.onStart
import com.mal.game_engine.engine.game.ext.onStop
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class GameLoop(
    private val dispatcher: CoroutineDispatcher,
    val components: List<GameComponent>,
    val render: () -> Unit
) {
    private val mutex = Mutex()

    private val dragPool = Collections.synchronizedList(mutableListOf<DragStatus>())

    val isRunning: Boolean
        get() {
            return isGameRunning.get()
        }

    private var isGameRunning = AtomicBoolean(false)

    fun startGame() {
        stopGameInternal()
        components.onStart()
        isGameRunning.set(true)

        var lastTime = System.nanoTime()
        val nsPerTick = TimeUnit.SECONDS.toNanos(1) / 60.0
        var delta = 0.0

        var tickCount = 0
        var renderCount = 0
        var time = System.currentTimeMillis()

        CoroutineScope(dispatcher).launch {
            while (isGameRunning.get()) {
                val now = System.nanoTime()
                delta += (now - lastTime) / nsPerTick
                lastTime = now
                while (delta >= 1) {
                    tickCount++
                    processInput()
                    components.calculate()
                    delta -= 1
                }
                renderCount++
                render()
                while (mutex.isLocked) {
                    yield()
                }
                if (!this.isActive || !isGameRunning.get()) {
                    break
                }
                mutex.lock()
                if (System.currentTimeMillis() - time >= 1000) {
                    time = System.currentTimeMillis()
                    Log.i("fps", "ticks:$tickCount, render:$renderCount")
                    tickCount = 0
                    renderCount = 0
                }
            }
            cancel()
        }
    }

    fun stopGame() {
        stopGameInternal()
        components.onStop()
    }

    private fun stopGameInternal() {
        isGameRunning.set(false)
        unlockMutex()
    }

    fun onFrameAvailable() {
        unlockMutex()
    }

    private fun unlockMutex() {
        if (mutex.isLocked) {
            try {
                mutex.unlock()
            } catch (_: Exception) {
            }
        }
    }

    fun onDragChanged(coordinate: Coordinate) {
        synchronized(dragPool) {
            dragPool.add(DragStatus.Drag(coordinate))
        }
    }

    fun onDragFinish(coordinate: Coordinate) {
        synchronized(dragPool) {
            dragPool.add(DragStatus.Finish(coordinate))
        }
    }

    fun onClick(coordinate: Coordinate) {
        synchronized(dragPool) {
            dragPool.add(DragStatus.Click(coordinate))
        }
    }

    private suspend fun processInput() {
        synchronized(dragPool) {
            dragPool.forEach { dragStatus ->
                when (dragStatus) {
                    is DragStatus.Drag -> components.onDragChanged(dragStatus.coordinate)
                    is DragStatus.Finish -> components.onDragFinish(dragStatus.coordinate)
                    is DragStatus.Click -> components.onClick(dragStatus.coordinate)
                }
            }
            dragPool.clear()
        }
    }
}