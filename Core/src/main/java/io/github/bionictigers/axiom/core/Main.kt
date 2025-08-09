package io.github.bionictigers.axiom.core

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.BaseCommandState
import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.Scheduler
import io.github.bionictigers.axiom.core.web.Server
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.seconds

data class TestData(var message: String, var number: Int, val messages: List<String>) : BaseCommandState()

fun main() {
    Server.start()

    fun testCommand() = Command.create("Test Command ${Scheduler.tick}", TestData("Hello, World!", 0, listOf("Hello, World!", "Kotlin is great!", "Axiom Framework"))) {
        action { state ->
            RobotLog.i("Test Command executing. Message: ${state.message}, Number: ${state.number}")
            state.number = Scheduler.tick.toInt()
            state.message = state.messages[state.number % state.messages.size]
            state.enteredAt!!.elapsedNow() > 2.seconds
        }
    }

    Scheduler.schedule(testCommand())

    fixedRateTimer("Scheduler", false, 0, 200) {
        Scheduler.update()
    }

    fixedRateTimer("Scheduler", false, 0, 400) {
        Scheduler.schedule(testCommand())
    }
}