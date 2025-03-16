package com.mal.game_engine.engine.game.component

import com.mal.game_engine.engine.coordinate.Coordinate

interface BehaviorComponent {
    fun init(){}

    fun calculate() {}

    fun onDragChanged(coordinate: Coordinate) {}

    fun onDragFinish(coordinate: Coordinate) {}

    fun onStop() {}

    fun onStart() {}
}