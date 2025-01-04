package com.mal.component.opengl.core.texture.param

sealed class TextureSize {
    abstract val width: Int
    abstract val height: Int

    class Hd(
        override val width: Int = 1024,
        override val height: Int = 1080
    ) : TextureSize()

    class Custom(
        override val width: Int,
        override val height: Int
    ) : TextureSize()
}