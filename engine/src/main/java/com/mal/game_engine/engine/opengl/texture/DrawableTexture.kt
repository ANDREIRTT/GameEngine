package com.mal.game_engine.engine.opengl.texture

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.drawable.toBitmap
import com.mal.game_engine.engine.opengl.texture.param.TextureMode
import com.mal.game_engine.engine.opengl.texture.param.TextureSize

class DrawableTexture(
    private val context: Context,
    @DrawableRes
    private val resource: Int,
    private val textureSize: TextureSize = TextureSize.Hd(),
    override val textureMode: TextureMode = TextureMode.FIT
) : Texture {

    private var _glGenID: Int = 0
    override val glGenID: Int
        get() {
            if (_glGenID == 0) {
                throw RuntimeException("Texture not loaded")
            }
            return _glGenID
        }

    private var _ratio = 0f
    override val ratio: Float
        get() {
            if (_ratio == 0f) {
                throw RuntimeException("Texture not loaded")
            }
            return _ratio
        }
    private var _width = 0
    override val width: Int
        get() = _width

    private var _height = 0
    override val height: Int
        get() = _height

    override fun loadTexture() {
        val textureHandle = IntArray(1)
        GLES30.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            val options = BitmapFactory.Options()
            options.inScaled = false

            val (bitmap, ratio) = getDrawable(context, resource)!!.let {
                _width = if (it.intrinsicWidth == -1) {
                    textureSize.width
                } else {
                    it.intrinsicWidth
                }
                _height = if (it.intrinsicHeight == -1) {
                    textureSize.height
                } else {
                    it.intrinsicHeight
                }
                Pair(
                    it.toBitmap(
                        textureSize.width,
                        textureSize.height
                    ),
                    textureSize.width.toFloat() / textureSize.height.toFloat()
                )
            }

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
            if (textureMode == TextureMode.FIT) {
                GLES30.glTexParameteri(
                    GLES30.GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_S,
                    GLES30.GL_CLAMP_TO_EDGE
                )
                GLES30.glTexParameteri(
                    GLES30.GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_T,
                    GLES30.GL_CLAMP_TO_EDGE
                )
            }
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)

            bitmap.recycle()

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            this._glGenID = textureHandle[0]
            this._ratio = ratio
        } else {
            throw RuntimeException("Error loading texture.")
        }
    }
}