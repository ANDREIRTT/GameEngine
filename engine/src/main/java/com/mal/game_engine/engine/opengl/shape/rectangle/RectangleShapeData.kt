package com.mal.game_engine.engine.opengl.shape.rectangle

import com.mal.game_engine.engine.opengl.texture.Texture
import com.mal.game_engine.engine.coordinate.Coordinate

data class RectangleShapeData(
    val vpMatrix: FloatArray,
    val texture: Texture,
    val coordinate: Coordinate,
    val width: Double,
    val height: Double
)