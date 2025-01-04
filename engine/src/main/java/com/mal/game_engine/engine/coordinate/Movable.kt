package com.mal.game_engine.engine.coordinate

import kotlin.math.hypot

class Movable(
    private val start: Coordinate,
    private val end: Coordinate
) {
    fun move(t: Double): Coordinate {
        return Coordinate(
            (1.0 - t) * start.x + t * end.x,
            (1.0 - t) * start.y + t * end.y
        )
    }

    fun distance(aspectRatio: Double): Double {
        return hypot(
            (end.x - start.x) * aspectRatio,
            end.y - start.y
        )
    }
}