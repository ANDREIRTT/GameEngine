package com.mal.game_engine.engine.di

import com.mal.game_engine.engine.GameEngine
import org.koin.core.component.KoinComponent

interface IsolatedKoinComponent : KoinComponent {
    override fun getKoin() = GameEngine.koinApp.koin
}