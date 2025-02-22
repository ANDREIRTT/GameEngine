package com.mal.game_engine.engine.surface.thread

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import co.touchlab.stately.concurrency.AtomicBoolean
import com.mal.game_engine.engine.surface.EglHelper
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.withLock

internal class GLThread(
    private val surface: Surface,
    private val renderer: GLSurfaceView.Renderer
) : Thread() {

    companion object {
        const val RENDERMODE_WHEN_DIRTY = 0
        const val RENDERMODE_CONTINUOUSLY = 1
    }

    // Статусы потока
    private enum class ThreadStatus { RUNNING, PAUSED, EXITING }

    // Статусы EGL-среды (состояния контекста и поверхности)
    private enum class EGLState { NO_CONTEXT, CONTEXT_CREATED, SURFACE_CREATED, INITIALIZED }

    // Этапы работы основного цикла (state machine)
    private enum class RenderStage {
        WAITING,          // Нет валидных условий для рендера
        CREATE_CONTEXT,   // Нужно создать EGL-контекст
        CREATE_SURFACE,   // Нужно создать EGL-поверхность
        INIT,             // Нужно вызвать onSurfaceChanged (или обработать изменение размера)
        RENDER            // Отрисовка кадра
    }

    // Дополнительные статусы для поверхности
    private enum class SurfaceStatus { NONE, WAITING, AVAILABLE, BAD }

    // Дополнительные статусы для размера
    private enum class SizeStatus { UNKNOWN, CHANGED, KNOWN }

    // Статусы уведомления о рендере
    private enum class RenderNotificationStatus { NONE, REQUESTED, COMPLETE }

    // Статус для запроса освобождения контекста EGL
    private enum class EGLContextRetention { RETAIN, RELEASE_REQUESTED }

    @Volatile
    private var threadStatus = ThreadStatus.RUNNING

    @Volatile
    private var eglState = EGLState.NO_CONTEXT

    @Volatile
    private var renderMode = RENDERMODE_CONTINUOUSLY

    private var requestRender = AtomicBoolean(false)

    @Volatile
    private var viewportWidth = 0

    @Volatile
    private var viewportHeight = 0

    @Volatile
    private var surfaceStatus: SurfaceStatus = SurfaceStatus.NONE

    @Volatile
    private var sizeStatus: SizeStatus = SizeStatus.UNKNOWN

    @Volatile
    private var renderNotificationStatus: RenderNotificationStatus = RenderNotificationStatus.NONE

    @Volatile
    private var eglContextRetention: EGLContextRetention = EGLContextRetention.RETAIN

    // Очередь событий для выполнения в GL-потоке
    private val eventQueue = mutableListOf<Runnable>()

    // Завершающий runnable для уведомления об окончании отрисовки
    private var finishDrawingRunnable: Runnable? = null

    // Помощник для работы с EGL (реализация должна быть предоставлена отдельно)
    private lateinit var eglHelper: EglHelper

    override fun run() {
        try {
            guardedRun()
        } catch (_: InterruptedException) {
        } finally {
            GLThreadManager.lock.withLock {
                GLThreadManager.condition.signalAll()
            }
        }
    }

    /**
     * Основной цикл GL-потока.
     */
    private fun guardedRun() {
        eglHelper = EglHelper(surface)
        var gl: GL10? = null

        while (true) {
            val currentStage = waitForRenderStage() ?: return

            when (currentStage) {
                RenderStage.CREATE_CONTEXT -> {
                    try {
                        eglHelper.start()
                    } catch (t: RuntimeException) {
                        Log.e("GLThread", "Unable to start EGL context", t)
                        return
                    }
                    eglState = EGLState.CONTEXT_CREATED
                    gl = eglHelper.createGL() as? GL10
                }

                RenderStage.CREATE_SURFACE -> {
                    if (!eglHelper.createSurface()) {
                        surfaceStatus = SurfaceStatus.BAD
                        continue
                    }
                    eglState = EGLState.SURFACE_CREATED
                    surfaceStatus = SurfaceStatus.AVAILABLE
                    renderer.onSurfaceCreated(gl, eglHelper.eglConfig)
                }

                RenderStage.INIT -> {
                    renderer.onSurfaceChanged(gl, viewportWidth, viewportHeight)
                    sizeStatus = SizeStatus.KNOWN
                    eglState = EGLState.INITIALIZED
                }

                RenderStage.RENDER -> {
                    if (renderMode == RENDERMODE_WHEN_DIRTY) {
                        requestRender.value = false
                    }

                    renderer.onDrawFrame(gl)
                    when (val swapError = eglHelper.swap()) {
                        EGL10.EGL_SUCCESS -> { /* всё успешно */
                        }

                        EGL11.EGL_CONTEXT_LOST -> {
                            eglState = EGLState.NO_CONTEXT
                            continue
                        }

                        else -> {
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError)
                            continue
                        }
                    }
                    if (renderNotificationStatus == RenderNotificationStatus.REQUESTED) {
                        finishDrawingRunnable?.run()
                        finishDrawingRunnable = null
                        renderNotificationStatus = RenderNotificationStatus.COMPLETE
                    }
                }

                RenderStage.WAITING -> {
                    // В этой стадии просто ждём изменения условий.
                }
            }
        }
    }

    /**
     * Внутренний цикл ожидания. Проверяет условия и ждет сигнала.
     */
    private fun waitForRenderStage(): RenderStage? {
        GLThreadManager.lock.withLock {
            while (true) {
                if (threadStatus == ThreadStatus.EXITING) return null
                if (eventQueue.isNotEmpty()) {
                    eventQueue.removeAt(0).run()
                    continue
                }
                if (eglContextRetention == EGLContextRetention.RELEASE_REQUESTED) {
                    eglState = EGLState.NO_CONTEXT
                    eglContextRetention = EGLContextRetention.RETAIN
                    eglHelper.finish()
                }
                val stage = determineRenderStage()
                if (stage == RenderStage.WAITING) {
                    try {
                        GLThreadManager.condition.await()
                    } catch (ie: InterruptedException) {
                        currentThread().interrupt()
                        return null
                    }
                } else {
                    return stage
                }
            }
        }
    }

    private fun determineRenderStage(): RenderStage = when {
        threadStatus == ThreadStatus.PAUSED -> RenderStage.WAITING
        surfaceStatus != SurfaceStatus.AVAILABLE -> RenderStage.WAITING

        viewportWidth <= 0 || viewportHeight <= 0 -> RenderStage.WAITING

        !requestRender.value && renderMode == RENDERMODE_WHEN_DIRTY -> RenderStage.WAITING
        eglState == EGLState.NO_CONTEXT -> RenderStage.CREATE_CONTEXT
        eglState == EGLState.CONTEXT_CREATED -> RenderStage.CREATE_SURFACE
        eglState == EGLState.SURFACE_CREATED || sizeStatus == SizeStatus.CHANGED -> RenderStage.INIT
        eglState == EGLState.INITIALIZED &&
                (requestRender.value || renderMode == RENDERMODE_CONTINUOUSLY) -> RenderStage.RENDER

        else -> {
            RenderStage.WAITING
        }
    }

    // --- Методы управления потоком и обновления состояний ---

    fun setRenderMode(mode: Int) {
        require(mode in RENDERMODE_WHEN_DIRTY..RENDERMODE_CONTINUOUSLY) { "Invalid render mode" }
        GLThreadManager.lock.withLock {
            renderMode = mode
            GLThreadManager.condition.signalAll()
        }
    }

    fun requestRender() {
        GLThreadManager.lock.withLock {
            requestRender.value = true
            GLThreadManager.condition.signalAll()
        }
    }

    fun requestRenderAndNotify(finishDrawing: Runnable?) {
        GLThreadManager.lock.withLock {
            if (currentThread() == this) return
            renderNotificationStatus = RenderNotificationStatus.REQUESTED
            requestRender.value = true
            finishDrawingRunnable = if (finishDrawingRunnable != null) {
                Runnable { finishDrawingRunnable!!.run(); finishDrawing?.run() }
            } else finishDrawing
            GLThreadManager.condition.signalAll()
        }
    }

    fun onWindowResize(width: Int, height: Int) {
        GLThreadManager.lock.withLock {
            viewportWidth = width
            viewportHeight = height
            sizeStatus = SizeStatus.CHANGED
            requestRender.value = true
            GLThreadManager.condition.signalAll()
        }
    }

    fun onPause() {
        GLThreadManager.lock.withLock {
            threadStatus = ThreadStatus.PAUSED
            GLThreadManager.condition.signalAll()
        }
    }

    fun onResume() {
        GLThreadManager.lock.withLock {
            threadStatus = ThreadStatus.RUNNING
            requestRender.value = true
            GLThreadManager.condition.signalAll()
        }
    }

    fun surfaceCreated() {
        GLThreadManager.lock.withLock {
            surfaceStatus = SurfaceStatus.AVAILABLE
            GLThreadManager.condition.signalAll()
        }
    }

    fun surfaceDestroyed() {
        GLThreadManager.lock.withLock {
            surfaceStatus = SurfaceStatus.WAITING
            GLThreadManager.condition.signalAll()
        }
    }

    fun requestReleaseEglContext() {
        GLThreadManager.lock.withLock {
            eglContextRetention = EGLContextRetention.RELEASE_REQUESTED
            GLThreadManager.condition.signalAll()
        }
    }

    fun queueEvent(r: Runnable) {
        GLThreadManager.lock.withLock {
            eventQueue.add(r)
            GLThreadManager.condition.signalAll()
        }
    }

    fun requestExitAndWait() {
        if (currentThread() == this) {
            throw RuntimeException("requestExitAndWait cannot be called from GLThread")
        }
        GLThreadManager.lock.withLock {
            threadStatus = ThreadStatus.EXITING
            GLThreadManager.condition.signalAll()
        }
        try {
            join()
        } catch (ie: InterruptedException) {
            currentThread().interrupt()
        }
    }
}