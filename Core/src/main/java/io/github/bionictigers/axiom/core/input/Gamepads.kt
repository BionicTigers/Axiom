package io.github.bionictigers.axiom.core.input

enum class Gamepads {
    GAMEPAD_1,
    GAMEPAD_2,
    BOTH,
    NONE
}

fun Gamepads.matches(gamepad: Gamepads): Boolean = this == gamepad || this == Gamepads.BOTH