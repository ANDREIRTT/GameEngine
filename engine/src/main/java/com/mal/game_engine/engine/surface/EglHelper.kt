package com.mal.game_engine.engine.surface

import android.util.Log
import android.view.Surface
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

internal class EglHelper(private val surface: Surface) {
    private var egl: EGL10? = null
    private var eglDisplay: EGLDisplay? = null
    var eglConfig: EGLConfig? = null
        private set
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null

    /**
     * Инициализирует EGL: получает дисплей, инициализирует его, выбирает конфигурацию
     * и создаёт EGL-контекст.
     */
    private  val EGL_OPENGL_ES3_BIT_KHR = 0x00000040

    fun start() {
        Log.w("EglHelper", "start() tid=${Thread.currentThread().id}")

        egl = EGLContext.getEGL() as EGL10

        eglDisplay = egl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }

        // Инициализируем дисплей
        val version = IntArray(2)
        if (!egl!!.eglInitialize(eglDisplay, version)) {
            throw RuntimeException("eglInitialize failed")
        }

        // Добавляем атрибут для запроса конфигурации с поддержкой ES 3.0
        val configAttribs = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR, // Запрашиваем ES 3.0
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!egl!!.eglChooseConfig(eglDisplay, configAttribs, configs, 1, numConfigs)) {
            throw RuntimeException("eglChooseConfig failed")
        }
        if (numConfigs[0] <= 0) {
            throw RuntimeException("No configs match configSpec")
        }
        eglConfig = configs[0]

        // Создаём EGL-контекст с указанием версии OpenGL ES 3.0
        val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        val contextAttribs = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE)
        eglContext = egl!!.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribs)
        if (eglContext == null || eglContext === EGL10.EGL_NO_CONTEXT) {
            eglContext = null
            throwEglException("createContext")
        }
        Log.w("EglHelper", "createContext $eglContext tid=${Thread.currentThread().id}")

        // На данном этапе поверхность ещё не создана
        eglSurface = null
    }
    /**
     * Создаёт EGL‑поверхность для заданного Surface. Если поверхность уже существует, уничтожает её.
     *
     * @return true, если поверхность успешно создана и сделана текущей.
     */
    fun createSurface(): Boolean {
        Log.w("EglHelper", "createSurface() tid=${Thread.currentThread().id}")

        // Проверяем, что EGL и его компоненты инициализированы
        if (egl == null) {
            throw RuntimeException("egl not initialized")
        }
        if (eglDisplay == null) {
            throw RuntimeException("eglDisplay not initialized")
        }
        if (eglConfig == null) {
            throw RuntimeException("eglConfig not initialized")
        }

        // Если ранее была создана поверхность – уничтожаем её
        destroySurfaceImp()

        // Создаём оконную EGL‑поверхность, используя переданный Surface
        eglSurface = egl!!.eglCreateWindowSurface(eglDisplay, eglConfig, surface, null)
        if (eglSurface == null || eglSurface === EGL10.EGL_NO_SURFACE) {
            val error = egl!!.eglGetError()
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.")
            }
            return false
        }

        // Делаем созданную поверхность текущей
        if (!egl!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            logEglErrorAsWarning("EglHelper", "eglMakeCurrent", egl!!.eglGetError())
            return false
        }
        return true
    }

    /**
     * Возвращает объект GL, связанный с текущим контекстом.
     */
    fun createGL(): Any {
        return eglContext?.gl ?: throw RuntimeException("eglContext is null")
    }

    /**
     * Выполняет обмен буферов. Возвращает EGL10.EGL_SUCCESS при успешном обмене или код ошибки.
     */
    fun swap(): Int {
        if (!egl!!.eglSwapBuffers(eglDisplay, eglSurface)) {
            return egl!!.eglGetError()
        }
        return EGL10.EGL_SUCCESS
    }

    /**
     * Уничтожает EGL‑поверхность.
     */
    fun destroySurface() {
        Log.w("EglHelper", "destroySurface() tid=${Thread.currentThread().id}")
        destroySurfaceImp()
    }

    private fun destroySurfaceImp() {
        if (eglSurface != null && eglSurface !== EGL10.EGL_NO_SURFACE) {
            egl!!.eglMakeCurrent(
                eglDisplay,
                EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT
            )
            egl!!.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = null
        }
    }

    /**
     * Завершает работу с EGL: уничтожает контекст и завершает дисплей.
     */
    fun finish() {
        Log.w("EglHelper", "finish() tid=${Thread.currentThread().id}")
        if (eglContext != null) {
            egl!!.eglDestroyContext(eglDisplay, eglContext)
            eglContext = null
        }
        if (eglDisplay != null) {
            egl!!.eglTerminate(eglDisplay)
            eglDisplay = null
        }
    }

    private fun throwEglException(function: String) {
        throwEglException(function, egl!!.eglGetError())
    }

    private fun throwEglException(function: String, error: Int): Nothing {
        val message = formatEglError(function, error)
        Log.e("EglHelper", "throwEglException tid=${Thread.currentThread().id} $message")
        throw RuntimeException(message)
    }

    companion object {
        /**
         * Выводит предупреждение об ошибке EGL.
         */
        fun logEglErrorAsWarning(tag: String, function: String, error: Int) {
            Log.w(tag, formatEglError(function, error))
        }

        fun formatEglError(function: String, error: Int): String {
            return "$function failed: 0x${Integer.toHexString(error)}"
        }
    }
}