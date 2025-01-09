package com.mal.game_engine.engine.surface.thread

import com.mal.game_engine.engine.surface.EglHelper
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.withLock

class GLThread(
) : Thread() {

    enum class RenderMode { WHEN_DIRTY, CONTINUOUSLY }

    @Volatile private var shouldExit = false
    @Volatile private var exited = false

    @Volatile private var requestPaused = false
    @Volatile private var paused = false

    @Volatile private var hasSurface = false
    @Volatile private var surfaceIsBad = false
    @Volatile private var waitingForSurface = false

    @Volatile private var haveEglContext = false
    @Volatile private var haveEglSurface = false

    @Volatile private var width = 0
    @Volatile private var height = 0
    @Volatile private var sizeChanged = true

    // Вместо int-режима используем enum
    private var renderMode = RenderMode.CONTINUOUSLY

    @Volatile private var requestRender = true

    // Очередь событий
    private val eventQueue = mutableListOf<Runnable>()

    // Пример помощника для EGL
    private val eglHelper = EglHelper()

    // Вместо synchronized(LOCK) + wait/notifyAll
    // используем ReentrantLock и Condition
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    override fun run() {
        name = "KotlinGLThread-${id}"
        try {
            guardedRun()
        } catch (e: InterruptedException) {
            // обработаем прерывание, если нужно
        } finally {
            lock.withLock {
                stopEglSurfaceLocked()
                stopEglContextLocked()
                exited = true
                // Вместо notifyAll() -> condition.signalAll()
                condition.signalAll()
            }
        }
    }

    private fun guardedRun() {
        var gl: GL10? = null
        var createEglContext = false
        var createEglSurface = false
        var createGlInterface = false
        var lostEglContext = false

        while (true) {
            var event: Runnable? = null

            lock.withLock {
                while (true) {
                    if (shouldExit) return

                    if (eventQueue.isNotEmpty()) {
                        event = eventQueue.removeAt(0)
                        break
                    }

                    if (paused != requestPaused) {
                        paused = requestPaused
                        condition.signalAll()
                    }

                    // Если мы потеряли surface
                    if (!hasSurface && !waitingForSurface) {
                        waitingForSurface = true
                        surfaceIsBad = false
                        condition.signalAll()
                    }

                    // Если surface появился
                    if (hasSurface && waitingForSurface) {
                        waitingForSurface = false
                        condition.signalAll()
                    }

                    // Готовы ли мы к отрисовке?
                    if (readyToDraw()) {
                        if (!haveEglContext) {
//                            eglHelper.start()
                            haveEglContext = true
                            createEglContext = true
                            condition.signalAll()
                        }

                        if (haveEglContext && !haveEglSurface) {
                            haveEglSurface = true
                            createEglSurface = true
                            createGlInterface = true
                            sizeChanged = true
                        }

                        if (haveEglSurface) {
                            if (sizeChanged) {
                                sizeChanged = false
                                // Запоминаем размер
                            }
                            requestRender = false
                            condition.signalAll()
                            break
                        }
                    }

                    // Если не готовы к отрисовке — ждём
                    // Вместо wait() -> condition.await()
                    condition.await()
                }
            } // end lock

            // Выполним задачу
            event?.run()

            // Создадим EGL-сурфейс
            if (createEglSurface) {
                /*if (!eglHelper.createSurface()) {
                    lock.withLock {
                        surfaceIsBad = true
                        condition.signalAll()
                    }
                    continue
                }*/
                createEglSurface = false
            }

            // Создадим GL
            if (createGlInterface) {
//                gl = eglHelper.createGL() as? GL10
                createGlInterface = false
            }

            // onSurfaceCreated
            if (createEglContext) {
//                glSurfaceViewRef.get()?.renderer?.onSurfaceCreated(gl, eglHelper.eglConfig)
                createEglContext = false
            }

            // onSurfaceChanged
//            glSurfaceViewRef.get()?.renderer?.onSurfaceChanged(gl, width, height)

            // onDrawFrame
//            glSurfaceViewRef.get()?.renderer?.onDrawFrame(gl)

 /*           // Свап буферов
            val swapError = eglHelper.swap()
            when (swapError) {
                EGL10.EGL_SUCCESS -> {}
                EGL11.EGL_CONTEXT_LOST -> {
                    lostEglContext = true
                }
                else -> {
                    lock.withLock {
                        surfaceIsBad = true
                        condition.signalAll()
                    }
                }
            }*/
        }
    }

    private fun readyToDraw(): Boolean {
        return !paused && hasSurface && !surfaceIsBad &&
                width > 0 && height > 0 &&
                (requestRender || renderMode == RenderMode.CONTINUOUSLY)
    }

    private fun stopEglSurfaceLocked() {
        if (haveEglSurface) {
            haveEglSurface = false
//            eglHelper.destroySurface()
        }
    }

    private fun stopEglContextLocked() {
        if (haveEglContext) {
//            eglHelper.finish()
            haveEglContext = false
        }
    }

    // Методы для UI-потока

    fun surfaceCreated() {
        lock.withLock {
            hasSurface = true
            condition.signalAll()
        }
    }

    fun surfaceDestroyed() {
        lock.withLock {
            hasSurface = false
            condition.signalAll()
        }
    }

    fun onPause() {
        lock.withLock {
            requestPaused = true
            condition.signalAll()
        }
    }

    fun onResume() {
        lock.withLock {
            requestPaused = false
            requestRender = true
            condition.signalAll()
        }
    }

    fun onWindowResize(w: Int, h: Int) {
        lock.withLock {
            width = w
            height = h
            sizeChanged = true
            requestRender = true
            condition.signalAll()
        }
    }

    fun requestRender() {
        lock.withLock {
            requestRender = true
            condition.signalAll()
        }
    }

    fun queueEvent(r: Runnable) {
        lock.withLock {
            eventQueue.add(r)
            condition.signalAll()
        }
    }

    fun requestExitAndWait() {
        lock.withLock {
            shouldExit = true
            condition.signalAll()
            while (!exited) {
                condition.await()
            }
        }
    }
}