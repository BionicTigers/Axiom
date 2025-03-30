package io.github.bionictigers

import com.qualcomm.robotcore.hardware.DcMotorEx
import io.github.bionictigers.axiom.commands.Scheduler
import io.github.bionictigers.axiom.web.Server

fun main() {
    Scheduler.addSystem(Slides(DcMotorEx()))
    Server.start()

    while (true) {
        Scheduler.update()
    }
}