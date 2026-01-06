package io.github.bionictigers.axiom.core.commands.bt.composites

import io.github.bionictigers.axiom.core.commands.bt.Blackboard
import io.github.bionictigers.axiom.core.commands.bt.BtCommand
import io.github.bionictigers.axiom.core.commands.bt.BtStatus
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for Selector composite, exposed for Seek debugging.
 */
data class SelectorState(
    val childNames: List<String>,
    var currentIndex: Int = 0,
    var status: String = BtStatus.RUNNING.name
)

/**
 * Selector (Fallback) composite node.
 *
 * Tries children in order until one succeeds:
 * - If a child returns SUCCESS, the Selector returns SUCCESS
 * - If a child returns FAILURE, try the next child
 * - If all children return FAILURE, the Selector returns FAILURE
 * - If a child returns RUNNING, the Selector returns RUNNING
 *
 * @param name Name of this selector node
 * @param children The child BT nodes to try in order
 */
class Selector(
    name: String = "Selector",
    private val children: List<BtCommand<*>>
) : BtCommand<SelectorState>(
    name,
    SelectorState(children.map { it.name })
) {
    private var currentChild: BtCommand<*>? = null

    init {
        require(children.isNotEmpty()) { "Selector must have at least one child" }

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
                        // Child succeeded, selector succeeds
                        state.status = BtStatus.SUCCESS.name
                        succeed()
                    }
                    BtStatus.FAILURE -> {
                        // Child failed, try next
                        val nextIndex = state.currentIndex + 1
                        if (nextIndex < children.size) {
                            state.currentIndex = nextIndex
                            currentChild = children[nextIndex]
                            currentChild?.blackboard = blackboard
                            currentChild?.reset()
                            Scheduler.schedule(currentChild!!)
                        } else {
                            // All children failed
                            state.status = BtStatus.FAILURE.name
                            fail()
                        }
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
