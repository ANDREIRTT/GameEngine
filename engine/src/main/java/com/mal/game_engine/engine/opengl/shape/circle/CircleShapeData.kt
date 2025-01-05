package com.mal.game_engine.engine.opengl.shape.circle

import com.mal.game_engine.engine.opengl.texture.param.TextureData
import com.mal.game_engine.engine.coordinate.Coordinate

data class CircleShapeData(
    val vpMatrix: FloatArray,
    val radius: Double,
    val coordinate: Coordinate,
    val borderWidth: Double = 0.0,
    val textureData: TextureData
)
