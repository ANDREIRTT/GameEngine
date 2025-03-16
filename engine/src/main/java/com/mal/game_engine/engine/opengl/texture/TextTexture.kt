package com.mal.game_engine.engine.opengl.texture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.opengl.GLES30
import android.opengl.GLUtils
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.mal.game_engine.engine.opengl.texture.param.TextureMode
import com.mal.game_engine.engine.opengl.texture.param.TextureSize

class TextTexture(
    private val text: String,
    private val textSize: Float,
    private val textAlign: Align = Align.CENTER,
    private val textColor: Int = Color.WHITE,
    private val borderWidth: Float = 0f,
    private val borderColor: Int = Color.BLACK,
    private val lineSpacing: Float = 0f,
    private val textureSize: TextureSize = TextureSize.Custom(512, 512),
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
        val width = textureSize.width
        val height = textureSize.height

        val bitmap = createBitmap(width, height)
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

    private fun createBitmap(width: Int, height: Int): Bitmap {
        // TextPaint для заливки текста с вашими настройками
        val fillTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = this@TextTexture.textSize
            color = textColor
            textAlign = textAlign
            style = Paint.Style.FILL
        }
        // TextPaint для обводки текста с вашими настройками
        val strokeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = this@TextTexture.textSize
            color = borderColor
            textAlign = textAlign
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }

        // Преобразуем Paint.Align в Layout.Alignment для StaticLayout
        val alignment = when (textAlign) {
            Align.LEFT -> Layout.Alignment.ALIGN_NORMAL
            Align.CENTER -> Layout.Alignment.ALIGN_CENTER
            Align.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        // Создаем StaticLayout для обводки
        val strokeLayout = staticLayout(strokeTextPaint, width, alignment)

        // Создаем StaticLayout для заливки
        val fillLayout = staticLayout(fillTextPaint, width, alignment)

        // Используем высоту layout'а для определения размера текста
        val textHeight = strokeLayout.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Центрирование текста по вертикали
        canvas.translate(0f, (height - textHeight) / 2f)

        // Рисуем сначала обводку, затем заливку
        strokeLayout.draw(canvas)
        fillLayout.draw(canvas)

        return bitmap
    }

    private fun staticLayout(
        fillTextPaint: TextPaint,
        width: Int,
        alignment: Layout.Alignment
    ) = StaticLayout.Builder
        .obtain(text, 0, text.length, fillTextPaint, width)
        .setAlignment(alignment)
        .setIncludePad(false)
        .setLineSpacing(lineSpacing, 1f)
        .build()
}