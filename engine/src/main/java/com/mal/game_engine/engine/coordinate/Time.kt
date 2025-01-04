package com.mal.game_engine.engine.coordinate

data class Time(
    val t: Double = 0.0
) {
    private val lastUpdateTime: Long = System.currentTimeMillis()

    fun update(speed: Double): Time {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0

        return Time(t = (t + deltaTime * speed))
    }

    fun isEnd(): Boolean {
        return t >= 1f
    }
}