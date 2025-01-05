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
                _width = textureSize.width
                _height = textureSize.height
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