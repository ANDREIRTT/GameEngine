package com.mal.game_engine.engine.game.component

interface RenderComponent {
    fun init()

    fun onScreenChanged()

    fun render()
}