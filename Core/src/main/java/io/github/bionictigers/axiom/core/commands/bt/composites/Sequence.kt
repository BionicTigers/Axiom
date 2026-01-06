package io.github.bionictigers.axiom.core.commands.bt.composites

import io.github.bionictigers.axiom.core.commands.bt.Blackboard
import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for Sequence composite, exposed for Seek debugging.
 */
data class SequenceState(
    val childNames: List<String>,
    var currentIndex: Int = 0,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Sequence composite node.
 *
 * Runs children in order until one fails:
 * - If a child returns SUCCESS, run the next child
 * - If all children return SUCCESS, the Sequence returns SUCCESS
 * - If a child returns FAILURE, the Sequence returns FAILURE
 * - If a child returns RUNNING, the Sequence returns RUNNING
 *
 * @param name Name of this sequence node
 * @param children The child BT nodes to run in order
 */
class Sequence(
    name: String = "Sequence",
    private val children: List<BtCommand<*>>
) : BtCommand<SequenceState>(
    name,
    SequenceState(children.map { it.name })
) {
    private var currentChild: BtCommand<*>? = null

    init {
        require(children.isNotEmpty()) { "Sequence must have at least one child" }

        enter {
            // Reset all children and start with the first one
            children.forEach { it.reset() }
            state!!.currentIndex = 0
            state.status = BtStatus.RUNNING.name
            currentChild = children.first()
            currentChild?.blackboard = blackboard
            Scheduler.schedule(currentChild!!)
        }

        action { state, _ ->
            val child = currentChild ?: return@action

            if (!child.running) {
                when (child.status) {
                    BtStatus.SUCCESS -> {
                        // Child succeeded, try next
                        val nextIndex = state.currentIndex + 1
                        if (nextIndex < children.size) {
                            state.currentIndex = nextIndex
                            currentChild = children[nextIndex]
                            currentChild?.blackboard = blackboard
                            currentChild?.reset()
                            Scheduler.schedule(currentChild!!)
                        } else {
                            // All children succeeded
                            state.status = BtStatus.SUCCESS.name
                            succeed()
                        }
                    }
                    BtStatus.FAILURE -> {
                        // Child failed, sequence fails
                        state.status = BtStatus.FAILURE.name
                        fail()
                    }
                    BtStatus.RUNNING -> {
                        // Should not happen if !child.running, but handle gracefully
                    }
                }
            }
        }

        exit {
            // Clean up: unschedule current child if still running
            currentChild?.let {
                if (it.running) {
                    Scheduler.unschedule(it)
                }
            }
        }
    }

    override fun reset() {
        super.reset()
        state?.currentIndex = 0
        state?.status = BtStatus.RUNNING.name
        children.forEach { it.reset() }
        currentChild = null
    }
}
