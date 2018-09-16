package com.bitwiserain.desktop

import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.bitwiserain.core.Application

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        LwjglApplication(Application(), LwjglApplicationConfiguration().apply {
            title = "SpringShot"
            addIcon("icon-128.png", Files.FileType.Internal)
            addIcon("icon-32.png", Files.FileType.Internal)
            addIcon("icon-16.png", Files.FileType.Internal)
        })
    }
}
