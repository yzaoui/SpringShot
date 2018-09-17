package com.bitwiserain.springshot.core

abstract class Entity {
    abstract val position: Position
    abstract val velocity: Velocity
//    abstract val oldPosition: Position
//    abstract val oldVelocity: Velocity
    abstract val width: Int
    abstract val height: Int
}
