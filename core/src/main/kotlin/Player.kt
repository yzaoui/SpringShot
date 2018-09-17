package com.bitwiserain.springshot.core

import ktx.math.vec2

private const val X_SPEED = 3f
private const val Y_SPEED = 10f

enum class Facing {
    LEFT,
    RIGHT
}

enum class HorizontalState {
    STATIC,
    MOVING,
    MOVING_CANCELLED
}

enum class VerticalState {
    STATIC,
    MOVING
}

class Player : Entity() {
    var horizontalState = HorizontalState.STATIC
    var verticalState = VerticalState.MOVING
    private var facing = Facing.RIGHT
    override val width = 32
    override val height = 32
    override val position: Position = vec2(64f, 96f)
    override val velocity: Velocity = vec2(0f, 0f)

    fun pressLeft() {
        if (horizontalState == HorizontalState.MOVING && facing == Facing.RIGHT) horizontalState = HorizontalState.MOVING_CANCELLED
        else if (horizontalState == HorizontalState.STATIC) horizontalState = HorizontalState.MOVING
        facing = Facing.LEFT
    }

    fun releaseLeft() {
        if (horizontalState == HorizontalState.MOVING) {
            horizontalState = HorizontalState.STATIC
        } else if (horizontalState == HorizontalState.MOVING_CANCELLED) {
            horizontalState = HorizontalState.MOVING
            facing = Facing.RIGHT
        }
    }

    fun pressRight() {
        if (horizontalState == HorizontalState.MOVING && facing == Facing.LEFT) horizontalState = HorizontalState.MOVING_CANCELLED
        else if (horizontalState == HorizontalState.STATIC) horizontalState = HorizontalState.MOVING
        facing = Facing.RIGHT
    }

    fun releaseRight() {
        if (horizontalState == HorizontalState.MOVING) {
            horizontalState = HorizontalState.STATIC
        } else if (horizontalState == HorizontalState.MOVING_CANCELLED) {
            horizontalState = HorizontalState.MOVING
            facing = Facing.LEFT
        }
    }

    fun jump() {
        if (verticalState == VerticalState.STATIC) {
            velocity.y = Y_SPEED
        }
    }

    fun preStep() {
        if (velocity.y != 0f) verticalState = VerticalState.MOVING

        if (horizontalState == HorizontalState.MOVING) {
            position.x += when(facing) {
                Facing.LEFT -> -X_SPEED
                Facing.RIGHT -> +X_SPEED
            }
        }
    }
}
