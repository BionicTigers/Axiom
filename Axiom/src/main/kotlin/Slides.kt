package io.github.bionictigers

import com.qualcomm.robotcore.hardware.DcMotorEx
import io.github.bionictigers.axiom.commands.BaseCommandState
import io.github.bionictigers.axiom.commands.Command
import io.github.bionictigers.axiom.commands.System
import io.github.bionictigers.axiom.utils.Time
import io.github.bionictigers.axiom.web.Editable

class ControlHub() {
    fun refreshBulkData() {}

    fun getTicks(motor: Int): Int {
        return 0
    }
}

data class SlidesAfterRunState(
    val pid: PID = PID(30.0, 0.0, 0.0, 0.0, 10000.0, -1.0, 1.0)
) : BaseCommandState("Slides After Run")

class Slides(private val motor: DcMotorEx) : System {
    override val name: String = "Slides"

    val controlHub = ControlHub()

    var ticks = 0
        private set

    @Editable var setPoint = 10000

    override val beforeRun = Command.continuous("Slides Data") {
        controlHub.refreshBulkData()
        //ticks = controlHub.getTicks(0)
    }

    override val afterRun = Command.continuous(SlidesAfterRunState(), Time.fromSeconds(1)) {
        motor.power = it.pid.calculate(setPoint, ticks)
        ticks += (motor.power * 100).toInt()

        println(ticks)
    }
}