package com.mal.component.opengl.core.shape.rectangle

import com.mal.component.opengl.core.texture.Texture
import com.mal.game_engine.engine.coordinate.Coordinate

data class RectangleShapeData(
    val vpMatrix: FloatArray,
    val texture: Texture,
    val coordinate: Coordinate,
    val width: Double,
    val height: Double
)