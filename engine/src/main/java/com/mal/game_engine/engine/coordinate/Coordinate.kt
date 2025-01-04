package com.mal.game_engine.engine.coordinate

import kotlin.math.atan2

data class Coordinate(
    val x: Double,
    val y: Double,
) {
    fun applyProjection(screenRatio: Double): Coordinate {
        return calculateMatrix(screenRatio)
    }

    fun removeProjection(screenRatio: Double): Coordinate {
        return calculateMatrix(1.0 / screenRatio)
    }

    fun fromPixelsToOpenGl(width: Double, height: Double): Coordinate {
        return Coordinate(
            x = (x / width) * 2.0 - 1.0,
            y = -((y / height) * 2.0 - 1.0)
        )
    }

    fun findAngle(target: Coordinate, screenRatio: Double): Double {
        return atan2(target.y - y, (target.x - x) * screenRatio)
    }

    private fun calculateMatrix(maxValue: Double): Coordinate {
        return this.copy(
            x = x * maxValue
        )
    }

    operator fun plus(radius: Double): Coordinate {
        return Coordinate(x + radius, y + radius)
    }
}