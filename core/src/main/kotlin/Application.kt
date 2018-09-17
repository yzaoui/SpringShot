package com.bitwiserain.springshot.core

import com.badlogic.gdx.Screen
import ktx.app.KtxGame

class Application : KtxGame<Screen>() {
    override fun create() {
        addScreen(MainScreen())
        setScreen<MainScreen>()
    }
}
