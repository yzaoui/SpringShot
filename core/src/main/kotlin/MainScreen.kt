package com.bitwiserain.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.MapObjects
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.math.plus
import ktx.math.plusAssign
import ktx.math.vec2
import kotlin.math.absoluteValue

private const val TIMESTEP: Float = .01f

class MainScreen : KtxScreen {
    val batch = SpriteBatch()
    val img = Texture("player.png")
    val char = Character()
    var accumulator = 0f
    val shapeRenderer = ShapeRenderer()
    var held = false
    val target = vec2()
    val tiledMap = TmxMapLoader().load("map.tmx")!!
    val tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap)
    val collisionObjects: MapObjects = tiledMap.layers["collision"]!!.objects
    val camera = OrthographicCamera(200f, 150f).apply {
        setToOrtho(false)
        update()
    }

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

            draw(img, char.pos.x, char.pos.y)

            end()
        }

        camera.update()
        tiledMapRenderer.setView(camera)
        tiledMapRenderer.render()

        if (held) with (shapeRenderer) {
            begin(ShapeRenderer.ShapeType.Line)
            color = Color.FIREBRICK
            curve(char.pos.x + (char.width / 2), char.pos.y + (char.height / 2), char.pos.x + 100f, char.pos.y + 100f, char.pos.x + 200f, char.pos.y + 200f, target.x, 0f, 100)
            end()
        }
    }

    fun update() {
        // Desired position
        with (char) {
            preStep(GRAVITY)

            val newPos = pos + vel

            for (block in collisionObjects) {
                val x = block.properties["x"] as Float
                val y = block.properties["y"] as Float
                val width = block.properties["width"] as Float
                val height = block.properties["height"] as Float

                // if x don't even overlap, skip
                if (newPos.x > x + width || newPos.x + char.width < x) continue

                val obstacleDisplacement = Rectangle(newPos.x, newPos.y, char.width, char.height).intersectingVector2(Rectangle(x, y, width, height))

                if (obstacleDisplacement.x.absoluteValue < obstacleDisplacement.y.absoluteValue) {
                    newPos.x += obstacleDisplacement.x
                }
            }

            for (block in collisionObjects) {
                val x = block.properties["x"] as Float
                val y = block.properties["y"] as Float
                val width = block.properties["width"] as Float
                val height = block.properties["height"] as Float

                // if y don't even overlap, skip
                if (newPos.y > y + height || newPos.y + char.height < y) continue

                val obstacleDisplacement = Rectangle(newPos.x, newPos.y, char.width, char.height).intersectingVector2(Rectangle(x, y, width, height))

                if (obstacleDisplacement.y.absoluteValue < obstacleDisplacement.x.absoluteValue) {
                    newPos.y += obstacleDisplacement.y
                }
            }

            if (pos.y == newPos.y) {
                vel.y = 0f
                verticalState = VerticalState.GROUND
            }

            pos.x = newPos.x
            pos.y = newPos.y
        }
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

typealias Position = Vector2
typealias Velocity = Vector2
typealias Acceleration = Vector2

class Character {
    private var horizontalState = HorizontalState.IDLE
    private var facing = Facing.RIGHT
    val width = 32f
    val height = 32f
    val pos: Position = vec2(64f, 96f)
    val vel: Velocity = vec2(0f, 0f)
    val X_SPEED = 3f
    var verticalState = VerticalState.AIR

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
            vel.y = 10f
        }
    }

    fun preStep(acceleration: Acceleration) {
        vel += acceleration

        if (vel.y != 0f) verticalState = VerticalState.AIR

        if (horizontalState == HorizontalState.MOVING) {
            pos.x += when(facing) {
                Facing.LEFT -> -X_SPEED
                Facing.RIGHT -> +X_SPEED
            }
        }
    }
}

/**
 * Returns a vector indicating how much [this] must be displaced to exit [other].
 */
fun Rectangle.intersectingVector2(other: Rectangle): Vector2 {
    val displacement = vec2()

    // Get the displacement between both mid-points
    val dx = (this.x + (this.width / 2)) - (other.x + (other.width / 2))
    val dy = (this.y + (this.height / 2)) - (other.y + (other.height / 2))

    // The distance between both mid-points if the rectangles are just touching
    val xTouchingMidpointDistance = (this.width + other.width) / 2
    val yTouchingMidpointDistance = (this.height + other.height) / 2

    if (dx.absoluteValue < xTouchingMidpointDistance && dy.absoluteValue < yTouchingMidpointDistance) {
        displacement.x = if (dx >= 0) {
            other.x + other.width - this.x
        } else {
            other.x - (this.x + this.width)
        }

        displacement.y = if (dy >= 0) {
             other.y + other.height - this.y
        } else {
            other.y - (this.y + this.height)
        }
    }

    return displacement
}
