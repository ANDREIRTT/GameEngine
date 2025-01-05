package com.mal.game_engine.engine.opengl.texture

import android.content.res.Resources
import android.opengl.GLES30
import com.mal.game_engine.engine.opengl.texture.param.TextureMode
import com.mal.game_engine.engine.opengl.texture.param.TextureTransformation
import com.mal.game_engine.engine.opengl.checkGL30
import com.mal.game_engine.engine.opengl.toGLBuffer
import java.lang.Math.toRadians
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

interface Texture {

    val glGenID: Int

    val ratio: Float

    val width: Int

    val height: Int

    val textureMode: TextureMode

    fun loadTexture()

    fun deleteTexture() {
        GLES30.glDeleteTextures(1, intArrayOf(glGenID), 0).checkGL30()
    }

    fun getTextureBuffer(): FloatBuffer {
        val repeat = when (textureMode) {
            TextureMode.REPEAT -> {
                val screenWidth = Resources.getSystem().displayMetrics.widthPixels.toFloat()
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()

                max(
                    screenWidth / screenHeight,
                    screenHeight / screenWidth
                )
            }

            TextureMode.FIT -> {
                1f
            }
        }
        return floatArrayOf(
            0.0f, repeat,
            0.0f, 0.0f,
            repeat, 0.0f,
            repeat, repeat
        ).toGLBuffer()
    }

    fun getTransformationMatrix(
        rotationDegrees: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ): FloatArray {
        val radians = toRadians(rotationDegrees.toDouble())
        val cos = cos(radians).toFloat()
        val sin = sin(radians).toFloat()

        return floatArrayOf(
            cos * scaleX, -sin * scaleY, 0f,
            sin * scaleX, cos * scaleY, 0f,
            0f, 0f, 1f
        )
    }

    fun getTransformationMatrix(
        transformation: TextureTransformation
    ): FloatArray {
        val radians = toRadians(transformation.rotationDegrees.toDouble())
        val cos = cos(radians).toFloat()
        val sin = sin(radians).toFloat()

        return floatArrayOf(
            cos * transformation.scaleX, -sin * transformation.scaleY, 0f,
            sin * transformation.scaleX, cos * transformation.scaleY, 0f,
            0f, 0f, 1f
        )
    }
}