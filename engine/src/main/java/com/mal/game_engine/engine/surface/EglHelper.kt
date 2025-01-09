package com.mal.game_engine.engine.surface

import android.util.Log
import android.view.Surface
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

internal class EglHelper {
    private var egl: EGL10? = null
    private var eglDisplay: EGLDisplay? = null
    private var eglSurface: EGLSurface? = null
    private var eglContext: EGLContext? = null

    fun initializeEgl(surface: Surface) {
        egl = EGLContext.getEGL() as EGL10
        eglDisplay = egl?.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }
        if (egl?.eglInitialize(eglDisplay, IntArray(2)) == false) {
            throw RuntimeException("eglInitialize failed")
        }

        // Параметры конфигурации (RGBA8888, поддержка OpenGL ES 2/3)
        val attribList = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT (2.0+)
            EGL10.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        egl?.eglChooseConfig(eglDisplay, attribList, configs, 1, numConfigs)
        val eglConfig = configs[0]

        // Создаём контекст под OpenGL ES 3.0 (0x3098 – это EGL_CONTEXT_CLIENT_VERSION)
        val contextAttribs = intArrayOf(0x3098, 3, EGL10.EGL_NONE)
        eglContext = egl?.eglCreateContext(
            eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribs
        )

        // Привязываем EGLSurface к SurfaceHolder.surface или любому другому surface
        eglSurface = egl?.eglCreateWindowSurface(eglDisplay, eglConfig, surface, null)
        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            val error: Int? = egl?.eglGetError()
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.")
            }
        }
        // Делаем текущий контекст активным
        egl?.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    fun swapBuffers() {
        egl?.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * Освобождаем ресурсы
     */
    fun release() {
        egl?.eglMakeCurrent(
            eglDisplay,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT
        )
        egl?.eglDestroySurface(eglDisplay, eglSurface)
        egl?.eglDestroyContext(eglDisplay, eglContext)
        egl?.eglTerminate(eglDisplay)

        egl = null
        eglDisplay = null
        eglSurface = null
        eglContext = null
    }
}