package com.mal.game_engine.engine.opengl.texture.param

import com.mal.game_engine.engine.opengl.texture.Texture

data class TextureData(
    val texture: Texture,
    val textureTransformation: TextureTransformation = TextureTransformation(0f)
)
