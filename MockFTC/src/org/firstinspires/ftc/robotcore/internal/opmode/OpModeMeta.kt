package org.firstinspires.ftc.robotcore.internal.opmode

class OpModeMeta {
    enum class Flavor {
        AUTONOMOUS, TELEOP, SYSTEM
    }

    val flavor = Flavor.TELEOP
    val name = "OpMode"
}