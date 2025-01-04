package com.mal.game_engine.engine

import android.content.Context
import com.mal.component.opengl.core.shape.line.LineShape
import com.mal.component.opengl.core.shape.rectangle.RectangleShape
import com.mal.game_engine.engine.di.coreModule
import com.mal.game_engine.engine.di.shapeModule
import com.mal.game_engine.engine.opengl.shape.circle.CircleShape
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication

object GameEngine {

    internal val koinApp = koinApplication {
        modules()
    }

    fun init(context: Context) {
        koinApp.androidContext(context)
        koinApp.modules(shapeModule, coreModule)
    }

    val circleShape by lazy {
        koinApp.koin.get<CircleShape>()
    }

    val lineShape by lazy {
        koinApp.koin.get<LineShape>()
    }

    val rectangleShape by lazy {
        koinApp.koin.get<RectangleShape>()
    }
}