package io.github.bionictigers.axiom.core.web.serializable

import io.github.bionictigers.axiom.core.web.Serializable

/**
 * Snapshot of a single BT node's state for tracing.
 */
data class NodeSnapshot(
    val id: String,
    val name: String,
    val type: String,
    val status: String,
    val active: Boolean,
    val parentId: String?
)

/**
 * Trace event for a behavior tree, emitted each tick.
 * Contains the full tree state for debugging in Seek.
 */
data class BehaviorTreeTrace(
    val treeId: String,
    val treeName: String,
    val activeNodePath: List<String>,
    val nodes: List<NodeSnapshot>,
    val blackboard: Map<String, Any?>
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "behavior_tree_trace",
            "tick" to tick,
            "data" to mapOf(
                "treeId" to treeId,
                "treeName" to treeName,
                "activeNodePath" to activeNodePath,
                "nodes" to nodes.map { node ->
                    mapOf(
                        "id" to node.id,
                        "name" to node.name,
                        "type" to node.type,
                        "status" to node.status,
                        "active" to node.active,
                        "parentId" to node.parentId
                    )
                },
                "blackboard" to blackboard
            )
        )
    }
}
