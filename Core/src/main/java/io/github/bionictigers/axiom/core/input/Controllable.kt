package io.github.bionictigers.axiom.core.input

interface Controllable<P> {
    fun bindControls(profile: P, gamepad: Gamepads, builder: Controls.Builder)
}