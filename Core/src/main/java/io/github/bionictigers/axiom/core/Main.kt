package io.github.bionictigers.axiom.core

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.Scheduler
import io.github.bionictigers.axiom.core.web.Server
import kotlin.concurrent.fixedRateTimer

fun main() {
    Server.start()

    val testCommand = Command.continuous {
        println("sneezy for always")
    }

    Scheduler.schedule(testCommand)

    fixedRateTimer("Scheduler", false, 0, 20) {
        Scheduler.update()
    }
}