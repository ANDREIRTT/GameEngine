package com.mal.game_engine.engine.surface.thread

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import com.mal.game_engine.engine.surface.EglHelper
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.withLock

class GLThread(
    private val surface: Surface,
    private val renderer: GLSurfaceView.Renderer
) : Thread() {

    companion object {
        const val RENDERMODE_WHEN_DIRTY = 0
        const val RENDERMODE_CONTINUOUSLY = 1
    }

    // Состояния жизненного цикла потока
    private enum class ThreadStatus { RUNNING, PAUSED, EXITING }

    // Состояния EGL-среды
    private enum class EGLState { NO_CONTEXT, CONTEXT_CREATED, SURFACE_CREATED, INITIALIZED }

    // Этапы работы основного цикла
    private enum class RenderStage {
        WAITING,          // Нет валидных условий для рендера (например, размеры не заданы или нет запроса)
        CREATE_CONTEXT,   // Нужно создать EGL-контекст
        CREATE_SURFACE,   // Нужно создать EGL-поверхность
        INIT,             // Нужно вызвать onSurfaceChanged для инициализации
        RENDER            // Отрисовываем кадр
    }

    @Volatile
    private var threadStatus = ThreadStatus.RUNNING

    @Volatile
    private var eglState = EGLState.NO_CONTEXT

    @Volatile
    private var renderMode = RENDERMODE_CONTINUOUSLY

    @Volatile
    private var requestRender = false

    // Размер области отрисовки
    @Volatile
    private var viewportWidth = 0

    @Volatile
    private var viewportHeight = 0

    // Очередь событий для выполнения в GL-потоке
    private val eventQueue = mutableListOf<Runnable>()

    // Помощник для работы с EGL
    private lateinit var eglHelper: EglHelper

    override fun run() {
        name = "GLThread $id"
        Log.i("GLThread", "starting tid=$id")
        try {
            guardedRun()
        } catch (ie: InterruptedException) {
            // Завершаем работу корректно
        } finally {
            GLThreadManager.threadExiting(this)
        }
    }

    /**
     * Основной цикл работы GL-потока.
     */
    private fun guardedRun() {
        eglHelper = EglHelper(surface)
        var gl: GL10? = null

        while (true) {
            // Определяем текущий этап, используя withLock для блокировки
            val currentStage = waitForRenderStage() ?: return

            // Выполнение действий по этапу вне блока withLock
            when (currentStage) {
                RenderStage.CREATE_CONTEXT -> {
                    try {
                        eglHelper.start() // Создаем EGL-контекст
                    } catch (t: RuntimeException) {
                        Log.e("GLThread", "Unable to start EGL context", t)
                        return
                    }
                    eglState = EGLState.CONTEXT_CREATED

                    gl = eglHelper.createGL() as? GL10
                    renderer.onSurfaceCreated(gl, eglHelper.eglConfig)
                }

                RenderStage.CREATE_SURFACE -> {
                    if (!eglHelper.createSurface()) {
                        Log.w("GLThread", "createSurface failed; retrying...")
                        continue
                    }
                    eglState = EGLState.SURFACE_CREATED
                }

                RenderStage.INIT -> {
                    // Вызываем onSurfaceChanged для инициализации
                    renderer.onSurfaceChanged(gl, viewportWidth, viewportHeight)
                    eglState = EGLState.INITIALIZED
                }

                RenderStage.RENDER -> {
                    renderer.onDrawFrame(gl)
                    when (val swapError = eglHelper.swap()) {
                        EGL10.EGL_SUCCESS -> {
                            // успешно обменяли буферы
                        }

                        EGL11.EGL_CONTEXT_LOST -> {
                            Log.i("GLThread", "EGL context lost tid=$id")
                            eglState = EGLState.NO_CONTEXT
                            continue
                        }

                        else -> {
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError)
                            continue
                        }
                    }
                    // Если режим WHEN_DIRTY – сбрасываем флаг запроса
                    GLThreadManager.lock.withLock {
                        if (renderMode == RENDERMODE_WHEN_DIRTY) {
                            requestRender = false
                        }
                        GLThreadManager.condition.signalAll()
                    }
                }

                RenderStage.WAITING -> {}
            }
        }
    }

    private fun waitForRenderStage(): RenderStage? {
        GLThreadManager.lock.withLock {
            while (true) {
                if (threadStatus == ThreadStatus.EXITING) return null

                if (eventQueue.isNotEmpty()) {
                    eventQueue.removeAt(0).run()
                    // После выполнения события продолжаем проверку
                    continue
                }

                val stage = determineRenderStage()
                if (stage == RenderStage.WAITING || threadStatus == ThreadStatus.PAUSED) {
                    // Если условия не выполнены, ожидаем сигнала об изменении состояния
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

    /**
     * Определяет текущий этап (RenderStage) на основе внутренних состояний.
     * Этот метод вызывается внутри блока withLock.
     */
    private fun determineRenderStage(): RenderStage = when {
        viewportWidth <= 0 || viewportHeight <= 0 ||
                (!requestRender && renderMode == RENDERMODE_WHEN_DIRTY) ->
            RenderStage.WAITING

        eglState == EGLState.NO_CONTEXT ->
            RenderStage.CREATE_CONTEXT

        eglState == EGLState.CONTEXT_CREATED ->
            RenderStage.CREATE_SURFACE

        eglState == EGLState.SURFACE_CREATED ->
            RenderStage.INIT

        eglState == EGLState.INITIALIZED &&
                (requestRender || renderMode == RENDERMODE_CONTINUOUSLY) ->
            RenderStage.RENDER

        else -> RenderStage.WAITING
    }

    // --- Методы управления потоком ---

    /**
     * Задает режим рендера.
     */
    fun setRenderMode(mode: Int) {
        require(mode in RENDERMODE_WHEN_DIRTY..RENDERMODE_CONTINUOUSLY) { "Invalid render mode" }
        GLThreadManager.lock.withLock {
            renderMode = mode
            GLThreadManager.condition.signalAll()
        }
    }

    /**
     * Запрос на рендер.
     */
    fun requestRender() {
        GLThreadManager.lock.withLock {
            requestRender = true
            GLThreadManager.condition.signalAll()
        }
    }

    /**
     * Устанавливает размеры области отрисовки.
     */
    fun onWindowResize(width: Int, height: Int) {
        GLThreadManager.lock.withLock {
            viewportWidth = width
            viewportHeight = height
            requestRender = true
            GLThreadManager.condition.signalAll()
        }
    }

    fun onPause() {
        GLThreadManager.lock.withLock {
            threadStatus = ThreadStatus.PAUSED
            GLThreadManager.condition.signalAll()
        }
    }

    // Метод для возобновления
    fun onResume() {
        GLThreadManager.lock.withLock {
            threadStatus = ThreadStatus.RUNNING
            requestRender = true // возможно, для вызова обновления
            GLThreadManager.condition.signalAll()
        }
    }

    /**
     * Добавляет событие для выполнения в GL-потоке.
     */
    fun queueEvent(r: Runnable) {
        GLThreadManager.lock.withLock {
            eventQueue.add(r)
            GLThreadManager.condition.signalAll()
        }
    }

    /**
     * Запрашивает завершение работы потока и ждёт его остановки.
     * Вызывать не из GL-потока.
     */
    fun requestExitAndWait() {
        if (Thread.currentThread() == this) {
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