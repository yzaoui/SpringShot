package com.bitwiserain.springshot.core

import ktx.math.vec2

private const val X_SPEED = 3f
private const val Y_SPEED = 10f

enum class Facing {
    LEFT,
    RIGHT
}

class Player {
    var horizontalState = HorizontalState.STATIC
    var verticalState = VerticalState.MOVING
    private var facing = Facing.RIGHT
    val width = 32
    val height = 32
    val pos: Position = vec2(64f, 96f)
    val vel: Velocity = vec2(0f, 0f)

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
            vel.y = Y_SPEED
        }
    }

    fun preStep() {
        if (vel.y != 0f) verticalState = VerticalState.MOVING

        if (horizontalState == HorizontalState.MOVING) {
            pos.x += when(facing) {
                Facing.LEFT -> -X_SPEED
                Facing.RIGHT -> +X_SPEED
            }
        }
    }
}
