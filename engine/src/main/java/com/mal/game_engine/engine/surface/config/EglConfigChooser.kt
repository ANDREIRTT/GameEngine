package com.mal.game_engine.engine.surface.config

import android.opengl.EGLExt
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

interface EGLConfigChooser {
    fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig
}

class OpenGLES3ConfigChooser(
    redSize: Int,
    greenSize: Int,
    blueSize: Int,
    alphaSize: Int,
    depthSize: Int,
    stencilSize: Int
) : EGLConfigChooser {

    // Исходный массив атрибутов конфигурации, без учета типа рендеринга
    private val baseConfigSpec = intArrayOf(
        EGL10.EGL_RED_SIZE, redSize,
        EGL10.EGL_GREEN_SIZE, greenSize,
        EGL10.EGL_BLUE_SIZE, blueSize,
        EGL10.EGL_ALPHA_SIZE, alphaSize,
        EGL10.EGL_DEPTH_SIZE, depthSize,
        EGL10.EGL_STENCIL_SIZE, stencilSize,
        EGL10.EGL_NONE
    )

    // Метод, который добавляет требуемые атрибуты для выбора ES3
    private fun filterConfigSpec(configSpec: IntArray): IntArray {
        // Создаем новый массив с двумя дополнительными элементами
        val len = configSpec.size
        val newConfigSpec = IntArray(len + 2)
        System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1)
        // Добавляем пару: EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR
        newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE
        newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR  // значение 0x40
        newConfigSpec[len + 1] = EGL10.EGL_NONE
        return newConfigSpec
    }

    private val mConfigSpec = filterConfigSpec(baseConfigSpec)

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        // Сначала получаем число конфигураций, удовлетворяющих mConfigSpec
        val numConfig = IntArray(1)
        if (!egl.eglChooseConfig(display, mConfigSpec, null, 0, numConfig)) {
            throw IllegalArgumentException("eglChooseConfig failed")
        }
        val numConfigs = numConfig[0]
        if (numConfigs <= 0) {
            throw IllegalArgumentException("No configs match configSpec")
        }
        // Запрашиваем все конфигурации
        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs, numConfig)) {
            throw IllegalArgumentException("eglChooseConfig#2 failed")
        }
        // Выбираем первую подходящую конфигурацию (при необходимости можно добавить дополнительную проверку)
        return configs.first { it != null }!!
    }
}