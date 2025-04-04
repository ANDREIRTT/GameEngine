package com.mal.game_engine.engine.opengl.shape.rectangle

import com.mal.game_engine.engine.coordinate.Coordinate
import com.mal.game_engine.engine.opengl.texture.param.TextureData

data class RectangleShapeData(
    val vpMatrix: FloatArray,
    val coordinate: Coordinate,
    val width: Double,
    val height: Double,
    val textureData: TextureData
)