package com.mal.game_engine.engine.di

import com.mal.game_engine.engine.opengl.shape.line.LineShape
import com.mal.game_engine.engine.opengl.shape.rectangle.RectangleShape
import com.mal.game_engine.engine.opengl.shape.circle.CircleShape
import org.koin.dsl.module

internal val shapeModule = module {
    single {
        CircleShape(
            context = get()
        )
    }
    single {
        LineShape(
            context = get()
        )
    }
    single {
        RectangleShape(
            context = get()
        )
    }
}