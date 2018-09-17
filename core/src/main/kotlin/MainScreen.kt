package com.bitwiserain.springshot.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.math.plus
import ktx.math.vec2
import ktx.math.vec3
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

private const val TIMESTEP: Float = .01f
private const val GRAVITY = -.3f
private const val FLAG_CAMERA_STOP_AT_WORLD_BOUNDARIES = true
private const val VIEWPORT_WIDTH = 640f
private const val VIEWPORT_HEIGHT = 480f

class MainScreen : KtxScreen {
    /****************************
     * LibGDX-related properties
     ****************************/
    private val spriteBatch = SpriteBatch()
    private val playerTexture = Texture("player.png")
    private val playerTextureRegions = TextureRegion.split(playerTexture, 32, 32)
    private val shapeRenderer = ShapeRenderer()
    private val tiledMap = TmxMapLoader().load("map.tmx")!!
    private val tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap)
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        update()
    }
    private val inputProcessor = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            when (keycode) {
                Input.Keys.LEFT, Input.Keys.A -> player.pressLeft()
                Input.Keys.RIGHT, Input.Keys.D -> player.pressRight()
                Input.Keys.UP, Input.Keys.W -> player.jump()
                else -> return super.keyDown(keycode)
            }

            return true
        }

        override fun keyUp(keycode: Int): Boolean {
            when (keycode) {
                Input.Keys.LEFT, Input.Keys.A -> player.releaseLeft()
                Input.Keys.RIGHT, Input.Keys.D -> player.releaseRight()
                else -> return super.keyUp(keycode)
            }

            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            held = true
            target.set(screenX.toFloat(), screenY.toFloat())

            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            target.set(screenX.toFloat(), screenY.toFloat())

            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            held = false

            return true
        }
    }
    /****************************
     * World-related properties
     ****************************/
    val player = Player()
    private var timeAccumulator = 0f
    var held = false
    val target = vec2()
    private val worldBounds = Rectangle(
        0f,
        0f,
        (tiledMap.properties["width"] as Int) * 32f,
        (tiledMap.properties["height"] as Int) * 32f
    )
    private val staticBlockExists: Set<Pair<Int, Int>>

    init {
        val blocks = mutableSetOf<Pair<Int, Int>>()
        tiledMap.layers["collision"].objects.forEach {
            it as RectangleMapObject
            blocks.add(it.rectangle.x.roundToInt() / 32 to it.rectangle.y.roundToInt() / 32)
        }
        staticBlockExists = blocks
    }

    override fun show() {
        Gdx.input.inputProcessor = inputProcessor
    }

    override fun render(delta: Float) {
        timeAccumulator += delta

        while (timeAccumulator >= TIMESTEP) {
            update()
            timeAccumulator -= TIMESTEP
        }

        camera.run {
            val cameraX = player.position.x + player.width / 2
            val cameraY = player.position.y + player.height / 2
            if (FLAG_CAMERA_STOP_AT_WORLD_BOUNDARIES) {
                position.set(
                    cameraX.coerceIn(VIEWPORT_WIDTH / 2, worldBounds.width - VIEWPORT_WIDTH / 2),
                    cameraY.coerceIn(VIEWPORT_HEIGHT / 2, worldBounds.height - VIEWPORT_HEIGHT / 2),
                    0f
                )
            } else {
                position.set(cameraX, cameraY, 0f)
            }
            update()
        }

        with (Gdx.gl) {
            glClearColor(173/255f, 216/255f, 230/255f, 1f)
            glClear(GL20.GL_COLOR_BUFFER_BIT)
        }

        with (spriteBatch) {
            begin()

            projectionMatrix = camera.combined

            val coord = textureRegionCoordinatesFromCharacter(player)

            draw(playerTextureRegions[coord.second][coord.first], player.position.x, player.position.y)

            end()
        }

        tiledMapRenderer.setView(camera)
        tiledMapRenderer.render()

        if (held) with (shapeRenderer) {
            begin(ShapeRenderer.ShapeType.Line)

            projectionMatrix = camera.combined
            color = Color.FIREBRICK

            line(player.position.run { vec2(x + player.width / 2, y + player.height / 2) }, camera.unproject(vec3(target)).run { vec2(x, y) })

            end()
        }
    }

    fun update() {
        // Desired position
        with (player) {
            preStep()

            velocity.y += GRAVITY

            val newPos = position + velocity

            for (block in staticBlockExists) {
                val charRect = Rectangle(newPos.x, newPos.y, player.width.toFloat(), player.height.toFloat())
                val blockRect = Rectangle(block.first * 32f, block.second * 32f, 32f, 32f)

                // if x don't even overlap, skip
                if (charRect.x > blockRect.x + blockRect.width || charRect.x + charRect.width < blockRect.x) continue

                val disp = charRect.intersectingVector2(blockRect)

                if (disp.x.absoluteValue < disp.y.absoluteValue) {
                    if (disp.x.sign == 1f && !staticBlockExists.contains(block.first + 1 to block.second)
                        || disp.x.sign == -1f && !staticBlockExists.contains(block.first - 1 to block.second)) {
                        newPos.x += disp.x
                    }
                }
            }

            for (block in staticBlockExists) {
                val charRect = Rectangle(newPos.x, newPos.y, player.width.toFloat(), player.height.toFloat())
                val blockRect = Rectangle(block.first * 32f, block.second * 32f, 32f, 32f)

                // if y don't even overlap, skip
                if (charRect.y > blockRect.y + blockRect.height || charRect.y + charRect.height < blockRect.y) continue

                val disp = charRect.intersectingVector2(blockRect)

                if (disp.y.absoluteValue < disp.x.absoluteValue) {
                    if (disp.y.sign == 1f && !staticBlockExists.contains(block.first to block.second + 1)
                        || disp.y.sign == -1f && !staticBlockExists.contains(block.first to block.second - 1)) {
                        newPos.y += disp.y
                    }
                }
            }

            if (newPos.x < worldBounds.x) {
                newPos.x = 0f
            } else if (newPos.x + player.width > worldBounds.x + worldBounds.width) {
                newPos.x = worldBounds.x + worldBounds.width - player.width
            }

            if (newPos.y < worldBounds.y) {
                newPos.y = 0f
            } else if (newPos.y + player.height > worldBounds.y + worldBounds.height) {
                newPos.y = worldBounds.y + worldBounds.height - player.height
            }

            if (position.y == newPos.y) {
                velocity.y = 0f
                verticalState = VerticalState.STATIC
            }

            position.x = newPos.x
            position.y = newPos.y
        }
    }

    override fun dispose() {
        spriteBatch.dispose()
        playerTexture.dispose()
        tiledMap.dispose()
    }
}

typealias Position = Vector2
typealias Velocity = Vector2

fun textureRegionCoordinatesFromCharacter(player: Player): Pair<Int, Int> {
    return if (player.verticalState == VerticalState.STATIC && (player.horizontalState == HorizontalState.STATIC || player.horizontalState == HorizontalState.MOVING_CANCELLED)) {
        0 to 0
    } else {
        1 to 0
    }
}

/**
 * Returns a vector indicating the shortest path [this] must take to exit one of [other]'s corners.
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
