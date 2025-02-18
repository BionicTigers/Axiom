package io.github.bionictigers

import io.github.bionictigers.axiom.commands.Command
import io.github.bionictigers.axiom.commands.Scheduler
import io.github.bionictigers.axiom.commands.CommandState
import io.github.bionictigers.axiom.web.Hidden
//import io.github.bionictigers.web.WebServer
import kotlin.concurrent.fixedRateTimer
import kotlin.math.cbrt
import kotlin.math.sin
import kotlin.math.sqrt
import com.qualcomm.robotcore.hardware.Gamepad as FTCGamepad

interface ArmCommand : CommandState {
    @Hidden
    var ticks: Double
    var setPoint: Int

    companion object {
        fun default(): ArmCommand {
            return object : ArmCommand, CommandState by CommandState.default("Arm") {
                override var ticks = 0.0
                override var setPoint = 1000
            }
        }
    }
}

interface PivotCommand : CommandState {
    var ticks: Int
    var setPoint: Int

    companion object {
        fun default(): PivotCommand {
            return object : PivotCommand, CommandState by CommandState.default("Pivot") {
                override var ticks = 0
                override var setPoint = 2000
            }
        }
    }
}

fun main() {
//    val (gp1, gp2) = Pair(FTCGamepad(), FTCGamepad())

//    val gamepadSystem = GamepadSystem(gp1, gp2)
    val command = Command(ArmCommand.default())
        .setAction {
            it.ticks += sin(it.timeInScheduler.seconds())
            false
        }

    val otherCommand = Command(PivotCommand.default())
        .setAction {
//            it.ticks += (sqrt((it.setPoint - it.ticks).toDouble()) * it.deltaTime.seconds()).toInt()
            false
        }
//    Scheduler.addSystem(gamepadSystem)
    Scheduler.add(command, otherCommand)
//    val (g1, g2) = gamepadSystem.gamepads

//    g1.getBooleanButton(Gamepad.Buttons.A).onDown {
//        println("down")
//    }

    //test code
//    fixedRateTimer(period = 500) {
//        gp1.a = !gp1.a
//    }

//    generateMotionProfile(100.0, 200.0, 400.0, 2000.0, 1000.0, 5000.0)

    while (true) {
        Scheduler.update()
    }
}