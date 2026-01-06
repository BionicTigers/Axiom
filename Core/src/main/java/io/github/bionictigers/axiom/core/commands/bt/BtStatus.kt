package io.github.bionictigers.axiom.core.commands.bt

/**
 * Status returned by behavior tree nodes.
 *
 * - [SUCCESS]: The node completed successfully
 * - [FAILURE]: The node failed
 * - [RUNNING]: The node is still executing
 */
enum class BtStatus {
    SUCCESS,
    FAILURE,
    RUNNING
}
