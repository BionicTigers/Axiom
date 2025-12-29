package org.firstinspires.ftc.robotcore.internal.opmode

class RegisteredOpModes {
    companion object {
        fun getInstance(): RegisteredOpModes {
            return RegisteredOpModes()
        }
    }

    val opModes: List<OpModeMeta> = listOf()

    fun waitOpModesRegistered() {}
}