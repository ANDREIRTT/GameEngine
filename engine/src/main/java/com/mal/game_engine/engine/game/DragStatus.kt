package com.mal.game_engine.engine.game

import com.mal.game_engine.engine.coordinate.Coordinate

internal sealed class DragStatus {
    class Drag(
        val coordinate: Coordinate
    ) : DragStatus()

    class Finish(
        val coordinate: Coordinate
    ) : DragStatus()
}