package com.qualcomm.robotcore.hardware

class DcMotorEx {
    var power: Double = 0.0
        set(value) {
            field = value.coerceIn(-1.0, 1.0)
        }
}