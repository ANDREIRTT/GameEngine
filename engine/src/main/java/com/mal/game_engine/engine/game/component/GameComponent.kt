package com.mal.game_engine.engine.game.component

interface GameComponent {

    fun getBehaviorComponent(): BehaviorComponent {
        return object : BehaviorComponent {}
    }

    fun getRenderComponent(): RenderComponent {
        return object : RenderComponent {
            override fun init() {
            }

            override fun onScreenChanged() {
            }

            override fun render() {
            }

        }
    }
}