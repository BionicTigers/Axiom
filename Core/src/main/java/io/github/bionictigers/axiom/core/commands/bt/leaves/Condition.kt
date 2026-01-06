package io.github.bionictigers.axiom.core.commands.bt.leaves

import io.github.bionictigers.axiom.core.commands.bt.Blackboard
import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus

/**
 * State for Condition leaf, exposed for Seek debugging.
 */
data class ConditionState(
    var result: Boolean? = null,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Condition leaf node.
 *
 * Immediately evaluates a predicate and returns SUCCESS or FAILURE:
 * - SUCCESS if predicate returns true
 * - FAILURE if predicate returns false
 *
 * Conditions do not stay RUNNING; they complete immediately.
 *
 * @param name Name of this condition node
 * @param predicate The condition to evaluate (receives the blackboard)
 */
class Condition(
    name: String = "Condition",
    private val predicate: (Blackboard?) -> Boolean
) : BtCommand<ConditionState>(
    name,
    ConditionState()
) {
    init {
        enter {
            state!!.result = null
            state.status = BtStatus.RUNNING.name
        }

        action { state, _ ->
            val result = predicate(blackboard)
            state.result = result
            if (result) {
                state.status = BtStatus.SUCCESS.name
                succeed()
            } else {
                state.status = BtStatus.FAILURE.name
                fail()
            }
        }
    }

    override fun reset() {
        super.reset()
        state?.result = null
        state?.status = BtStatus.RUNNING.name
    }
}
