package com.mal.game_engine.engine.game.ext

import com.mal.game_engine.engine.coordinate.Coordinate
import com.mal.game_engine.engine.game.component.GameComponent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal suspend fun List<GameComponent>.calculate() {
    val mutex = Mutex()
    forEach {
        mutex.withLock {
            it.getBehaviorComponent().calculate()
        }
    }
}

internal fun List<GameComponent>.onDragChanged(coordinate: Coordinate) {
    forEach { it.getBehaviorComponent().onDragChanged(coordinate) }
}

internal fun List<GameComponent>.onDragFinish(coordinate: Coordinate) {
    forEach { it.getBehaviorComponent().onDragFinish(coordinate) }
}

internal fun List<GameComponent>.onClick(coordinate: Coordinate) {
    forEach { it.getBehaviorComponent().onClickConfirmed(coordinate) }
}

internal fun List<GameComponent>.onStop() {
    forEach { it.getBehaviorComponent().onStop() }
    forEach { it.getRenderComponent().onStop() }
}

internal fun List<GameComponent>.onStart() {
    forEach { it.getBehaviorComponent().onStart() }
    forEach { it.getRenderComponent().onStart() }
}

internal fun List<GameComponent>.render() {
    forEach { it.getRenderComponent().render() }
}

internal fun List<GameComponent>.init() {
    forEach { it.getBehaviorComponent().init() }
    forEach { it.getRenderComponent().init() }
}

internal fun List<GameComponent>.onScreenChanged() {
    forEach { it.getRenderComponent().onScreenChanged() }
}