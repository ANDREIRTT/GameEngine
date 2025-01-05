package com.mal.game_engine.engine.opengl.texture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.opengl.GLES30
import android.opengl.GLUtils
import com.mal.game_engine.engine.opengl.texture.param.TextureMode

class TextTexture(
    private val text: String,
    private val textSize: Float,
    private val textAlign: Align,
    private val textColor: Int = Color.WHITE,
    private val borderWidth: Float,
    private val borderColor: Int = Color.BLACK,
    override val textureMode: TextureMode = TextureMode.FIT,
) : Texture {

    private var _glGenID: Int = 0
    override val glGenID: Int
        get() = _glGenID

    private var _ratio = 0f
    override val ratio: Float
        get() = _ratio

    private var _width: Int = 0
    override val width: Int
        get() = _width

    private var _height: Int = 0
    override val height: Int
        get() = _height


    override fun loadTexture() {
        val paint = Paint().also {
            it.textSize = textSize
            it.isAntiAlias = true
            it.textAlign = textAlign
        }

        val width = 512
        val height = 512

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate the x and y coordinates for the center
        val x = width / 2f
        val y = (height / 2f) - ((paint.descent() + paint.ascent()) / 2)

        canvas.drawText(text, x, y, paint.apply {
            style = Paint.Style.FILL
            color = textColor
        })

        canvas.drawText(text, x, y, paint.apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        })

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        val textureHandle = IntArray(1)
        GLES30.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0])

            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()

            _glGenID = textureHandle[0]
            _width = width
            _height = height
            _ratio = width.toFloat() / height.toFloat()
        } else {
            throw RuntimeException("Error loading texture.")
        }
    }
}