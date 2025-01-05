package com.mal.game_engine.engine.opengl.shape.line

import com.mal.game_engine.engine.coordinate.Coordinate

data class LineShapeData(
    val vpMatrix: FloatArray,
    val startCoordinate: Coordinate,
    val endCoordinate: Coordinate,
    val thickness: Double
)