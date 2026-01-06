package io.github.bionictigers.axiom.core.commands.bt

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.scheduler.Scheduler

/**
 * State for TracedBehaviorTree, exposed for Seek debugging.
 */
data class TracedBehaviorTreeState(
    val treeName: String,
    var rootStatus: String = BtStatus.RUNNING.name,
    val blackboard: Map<String, Any?>
)

/**
 * A wrapper command that runs a behavior tree and emits trace events to Seek.
 *
 * This command:
 * 1. Schedules the tree's root node on enter
 * 2. Emits a [BehaviorTreeTrace] each tick while running
 * 3. Completes when the root node completes
 *
 * Usage:
 * ```kotlin
 * val tree = behaviorTree("MyTree") { ... }
 * val traced = TracedBehaviorTree(tree)
 * Scheduler.schedule(traced)
 * ```
 *
 * @param tree The behavior tree to run and trace
 * @param traceEveryNTicks Only emit trace every N ticks (1 = every tick)
 */
class TracedBehaviorTree(
    private val tree: BehaviorTree,
    private val traceEveryNTicks: Int = 1
) : Command<TracedBehaviorTreeState>(
    tree.name,
    TracedBehaviorTreeState(tree.name, blackboard = tree.blackboard.data)
) {
    private var tickCounter = 0

    init {
        enter {
            tree.root.reset()
            tree.root.blackboard = tree.blackboard
            tickCounter = 0
            state!!.rootStatus = BtStatus.RUNNING.name
            Scheduler.schedule(tree.root)
        }

        action { state, _ ->
            // Update blackboard reference in state for Seek
            // Note: state is immutable data class, so we track status separately
            state.rootStatus = tree.root.status.name

            // Emit trace
            tickCounter++
            if (tickCounter >= traceEveryNTicks) {
                tickCounter = 0
                BtTracer.emitTrace(tree)
            }

            // Check if root is done
            if (!tree.root.running) {
                stop()
            }
        }

        exit {
            // Clean up: unschedule root if still running
            if (tree.root.running) {
                Scheduler.unschedule(tree.root)
            }
        }
    }

    /**
     * Get the final status of the behavior tree root.
     */
    val treeStatus: BtStatus
        get() = tree.root.status
}

/**
 * Create a traced version of a behavior tree.
 *
 * @param traceEveryNTicks Only emit trace every N ticks (1 = every tick, default)
 * @return A command that runs and traces the tree
 */
fun BehaviorTree.traced(traceEveryNTicks: Int = 1): TracedBehaviorTree {
    return TracedBehaviorTree(this, traceEveryNTicks)
}
