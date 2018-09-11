package com.bitwiserain.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.math.vec2

private const val TIMESTEP: Float = .01f

class MainScreen : KtxScreen {
    val batch = SpriteBatch()
    val img = Texture("player.png")
    val char = Character()
    var accumulator = 0f
    val shapeRenderer = ShapeRenderer()
    var held = false
    val target = vec2()

    init {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.LEFT, Input.Keys.A -> char.pressLeft()
                    Input.Keys.RIGHT, Input.Keys.D -> char.pressRight()
                    Input.Keys.UP, Input.Keys.W -> char.jump()
                    else -> return super.keyDown(keycode)
                }

                return true
            }

            override fun keyUp(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.LEFT, Input.Keys.A -> char.releaseLeft()
                    Input.Keys.RIGHT, Input.Keys.D -> char.releaseRight()
                    else -> return super.keyUp(keycode)
                }

                return true
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                held = true
                target.set(screenX.toFloat(), Gdx.graphics.height - screenY.toFloat())

                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                target.set(screenX.toFloat(), Gdx.graphics.height - screenY.toFloat())

                return true
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                held = false

                return true
            }
        }
    }

    override fun render(delta: Float) {
        accumulator += delta

        while (accumulator >= TIMESTEP) {
            update()
            accumulator -= TIMESTEP
        }

        with (Gdx.gl) {
            glClearColor(0f, 0f, 0f, 1f)
            glClear(GL20.GL_COLOR_BUFFER_BIT)
        }

        with (batch) {
            begin()

            draw(img, char.x, char.y)

            end()
        }

        if (held) with (shapeRenderer) {
            begin(ShapeRenderer.ShapeType.Line)
            color = Color.FIREBRICK
            curve(char.x + (char.width / 2), char.y + (char.height / 2), char.x + 100f, char.y + 100f, char.x + 200f, char.y + 200f, target.x, 0f, 100)
            end()
        }
    }

    fun update() {
        char.step()
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }
}

enum class HorizontalState {
    IDLE,
    MOVING,
    MOVING_CANCELLED
}

enum class VerticalState {
    GROUND,
    AIR
}

enum class Facing {
    LEFT,
    RIGHT
}

private val GRAVITY = vec2(0f, -0.3f)

class Character : Rectangle(100f, 100f, 32f, 32f) {
    private var horizontalState = HorizontalState.IDLE
    private var facing = Facing.RIGHT
    private val vel = vec2(3f, 0f)
    private var verticalState = VerticalState.GROUND

    fun pressLeft() {
        if (horizontalState == HorizontalState.MOVING && facing == Facing.RIGHT) horizontalState = HorizontalState.MOVING_CANCELLED
        else if (horizontalState == HorizontalState.IDLE) horizontalState = HorizontalState.MOVING
        facing = Facing.LEFT
    }

    fun releaseLeft() {
        if (horizontalState == HorizontalState.MOVING) {
            horizontalState = HorizontalState.IDLE
        } else if (horizontalState == HorizontalState.MOVING_CANCELLED) {
            horizontalState = HorizontalState.MOVING
            facing = Facing.RIGHT
        }
    }

    fun pressRight() {
        if (horizontalState == HorizontalState.MOVING && facing == Facing.LEFT) horizontalState = HorizontalState.MOVING_CANCELLED
        else if (horizontalState == HorizontalState.IDLE) horizontalState = HorizontalState.MOVING
        facing = Facing.RIGHT
    }

    fun releaseRight() {
        if (horizontalState == HorizontalState.MOVING) {
            horizontalState = HorizontalState.IDLE
        } else if (horizontalState == HorizontalState.MOVING_CANCELLED) {
            horizontalState = HorizontalState.MOVING
            facing = Facing.LEFT
        }
    }

    fun jump() {
        if (verticalState == VerticalState.GROUND) {
            vel.y += 10f
        }
    }

    fun step() {
        vel.add(GRAVITY)

        if (horizontalState == HorizontalState.MOVING) {
            x += when(facing) {
                Facing.LEFT -> -vel.x
                Facing.RIGHT -> vel.x
            }
        }

        if (y + vel.y <= 0) {
            y = 0f
            vel.y = 0f
        } else {
            y += vel.y
        }

        verticalState = if (y > 0) VerticalState.AIR else VerticalState.GROUND
    }
}
