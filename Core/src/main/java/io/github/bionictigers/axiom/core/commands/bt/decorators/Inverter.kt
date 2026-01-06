package io.github.bionictigers.axiom.core.commands.bt.decorators

import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for Inverter decorator, exposed for Seek debugging.
 */
data class InverterState(
    val childName: String,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Inverter decorator node.
 *
 * Inverts the result of its child:
 * - SUCCESS becomes FAILURE
 * - FAILURE becomes SUCCESS
 * - RUNNING stays RUNNING
 *
 * @param name Name of this inverter node
 * @param child The child BT node to invert
 */
class Inverter(
    name: String = "Inverter",
    private val child: BtCommand<*>
) : BtCommand<InverterState>(
    name,
    InverterState(child.name)
) {
    init {
        enter {
            child.reset()
            child.blackboard = blackboard
            state!!.status = BtStatus.RUNNING.name
            Scheduler.schedule(child)
        }

        action { state, _ ->
            if (!child.running) {
                when (child.status) {
                    BtStatus.SUCCESS -> {
                        state.status = BtStatus.FAILURE.name
                        fail()
                    }
                    BtStatus.FAILURE -> {
                        state.status = BtStatus.SUCCESS.name
                        succeed()
                    }
                    BtStatus.RUNNING -> {
                        // Should not happen if !child.running
                    }
                }
            }
        }

        exit {
            if (child.running) {
                Scheduler.unschedule(child)
            }
        }
    }

    override fun reset() {
        super.reset()
        state?.status = BtStatus.RUNNING.name
        child.reset()
    }
}
