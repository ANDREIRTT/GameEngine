package com.mal.component.opengl.core.shape.circle

import com.mal.component.opengl.core.texture.param.TextureData
import com.mal.game_engine.engine.coordinate.Coordinate

data class CircleShapeData(
    val vpMatrix: FloatArray,
    val radius: Double,
    val coordinate: Coordinate,
    val borderWidth: Double = 0.0,
    val textureData: TextureData
)
