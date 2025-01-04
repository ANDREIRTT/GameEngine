package com.mal.game_engine.engine.opengl

import android.content.Context
import android.opengl.GLES30
import androidx.annotation.RawRes
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

fun Context.readRawResource(@RawRes resourceId: Int): String {
    return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
}

inline fun Int.withGL30(block: (Int) -> Unit): Int {
    if (this == -1) {
        throw RuntimeException("OpenGL error value=-1")
    }
    block(this)
    val error = GLES30.glGetError()
    if (error != GLES30.GL_NO_ERROR) {
        throw RuntimeException("OpenGL error: $error")
    }
    return this
}

fun Unit.checkGL30() {
    val error = GLES30.glGetError()
    if (error != GLES30.GL_NO_ERROR) {
        throw RuntimeException("OpenGL error: $error")
    }
}

fun FloatArray.toGLBuffer(): FloatBuffer {
    return ByteBuffer.allocateDirect(this.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(this@toGLBuffer)
            position(0)
        }
    }
}

fun DoubleArray.toGLBuffer(): FloatBuffer {
    return ByteBuffer.allocateDirect(this.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(this@toGLBuffer.toFloatArray())
            position(0)
        }
    }
}

fun ShortArray.toGLBuffer(): ShortBuffer {
    return ByteBuffer.allocateDirect(this.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
            put(this@toGLBuffer)
            position(0)
        }
    }
}

fun DoubleArray.toFloatArray(): FloatArray {
    return FloatArray(size) { i -> get(i).toFloat() }
}