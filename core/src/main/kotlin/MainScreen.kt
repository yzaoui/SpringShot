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
import com.badlogic.gdx.math.Vector3
import ktx.app.KtxScreen
import ktx.assets.file
import ktx.graphics.circle
import ktx.graphics.use
import ktx.math.*
import kotlin.math.*

private const val TIMESTEP: Float = .01f
private const val GRAVITY = -.3f
private const val FLAG_CAMERA_STOP_AT_WORLD_BOUNDARIES = true
private const val VIEWPORT_WIDTH = 640f
private const val VIEWPORT_HEIGHT = 480f

class MainScreen : KtxScreen {
    /****************************
     * LibGDX-related properties
     ****************************/
    private val backgroundMusic = Gdx.audio.newMusic(file("music.wav")).apply {
        isLooping = true
        play()
    }
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
                Input.Keys.UP, Input.Keys.W, Input.Keys.SPACE -> player.jump()
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
            mouseTarget.set(screenX.toFloat(), screenY.toFloat())

            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            mouseTarget.set(screenX.toFloat(), screenY.toFloat())

            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            held = false
            touchReleased()

            return true
        }
    }
    /****************************
     * World-related properties
     ****************************/
    val player = Player()
    private var timeAccumulator = 0f
    var held = false
    val mouseTarget = vec2()
    private val worldBounds = Rectangle(
        0f,
        0f,
        ((tiledMap.properties["width"] as Int) * (tiledMap.properties["tilewidth"] as Int)).toFloat(),
        ((tiledMap.properties["height"] as Int) * (tiledMap.properties["tileheight"] as Int)).toFloat()
    )
    private val staticBlockExists: Set<Pair<Int, Int>>
    private var projectiles: MutableList<Projectile> = mutableListOf()

    init {
        val blocks = mutableSetOf<Pair<Int, Int>>()
        tiledMap.layers["collision"].objects.forEach {
            it as RectangleMapObject
            blocks.add(it.rectangle.x.roundToInt() / 16 to it.rectangle.y.roundToInt() / 16)
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

        tiledMapRenderer.setView(camera)
        tiledMapRenderer.render()

        spriteBatch.use {
            it.projectionMatrix = camera.combined

            val coord = textureRegionCoordinatesFromCharacter(player)

            it.draw(playerTextureRegions[coord.second][coord.first], player.position.x, player.position.y)
        }

        with (shapeRenderer) {
            begin(ShapeRenderer.ShapeType.Filled)

            projectionMatrix = camera.combined

            for (proj in projectiles) {
                circle(proj.position + vec2(4f, 4f), 4f)
            }

            end()
        }

        if (held) with (shapeRenderer) {
            begin(ShapeRenderer.ShapeType.Line)

            projectionMatrix = camera.combined
            color = Color.FIREBRICK

            line(player.position.run { vec2(x + player.width / 2, y + player.height / 2) }, camera.unproject(vec3(mouseTarget)).toVector2())

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
                val blockRect = Rectangle(block.first * 16f, block.second * 16f, 16f, 16f)

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
                val blockRect = Rectangle(block.first * 16f, block.second * 16f, 16f, 16f)

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

        for (proj in projectiles) {
        }

        projectiles = projectiles.asSequence().filter {
            it.position.plusAssign(it.velocity)
            it.velocity.y += GRAVITY

            it.position in worldBounds
        }.toMutableList()
    }

    override fun dispose() {
        spriteBatch.dispose()
        playerTexture.dispose()
        tiledMap.dispose()
        backgroundMusic.dispose()
    }

    fun touchReleased() {
        val mouseTargetInWorld = camera.unproject(vec3(mouseTarget)).toVector2()
        val playerToMouse = mouseTargetInWorld - player.position

        // Since position is bottom left, we need to center to player position, then shift to projectile's bottom left
        val projectilePosition = player.position.cpy() // bottom left of player
            .plus(vec2(player.width.toFloat() / 2, player.height.toFloat() / 2)) // center of player
            .minus(vec2(4f, 4f)) // projectile center centered with player center

        val projectileVelocity = vec2Polar(10f, playerToMouse.angleRad())

        projectiles.add(Projectile(projectilePosition, projectileVelocity))
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

fun Vector3.toVector2() = Vector2(x, y)

typealias AngleRadians = Float

fun vec2Polar(magnitude: Float, angle: AngleRadians): Vector2 {
    return vec2(
        x = magnitude * cos(angle),
        y = magnitude * sin(angle)
    )
}
