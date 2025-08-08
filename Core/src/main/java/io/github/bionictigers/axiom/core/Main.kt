package io.github.bionictigers.axiom.core

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.BaseCommandState
import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.Scheduler
import io.github.bionictigers.axiom.core.web.Server
import kotlin.concurrent.fixedRateTimer

data class TestData(var message: String, var number: Int, val messages: List<String>) : BaseCommandState()

fun main() {
    Server.start()

    val testCommand = Command.create("Test Command", TestData("Hello, World!", 0, listOf("Hello, World!", "Kotlin is great!", "Axiom Framework"))) {
        action { state ->
            RobotLog.i("Test Command executing. Message: ${state.message}, Number: ${state.number}")
            state.number = Scheduler.tick.toInt()
            state.message = state.messages[state.number % state.messages.size]
            Scheduler.tick % 4L == 0L
        }
    }

    Scheduler.schedule(testCommand)

    fixedRateTimer("Scheduler", false, 0, 200) {
        Scheduler.update()
    }

    fixedRateTimer("Scheduler", false, 0, 200 * 8) {
        Scheduler.schedule(testCommand)
    }
}