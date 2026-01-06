package io.github.bionictigers.axiom.core.commands.bt

import io.github.bionictigers.axiom.core.commands.bt.composites.Parallel
import io.github.bionictigers.axiom.core.commands.bt.composites.Selector
import io.github.bionictigers.axiom.core.commands.bt.composites.Sequence
import io.github.bionictigers.axiom.core.commands.bt.decorators.Inverter
import io.github.bionictigers.axiom.core.commands.bt.decorators.Repeater
import io.github.bionictigers.axiom.core.commands.bt.decorators.Succeeder
import io.github.bionictigers.axiom.core.commands.bt.decorators.UntilFail
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.serializable.BehaviorTreeTrace
import io.github.bionictigers.axiom.core.web.serializable.NodeSnapshot

/**
 * Utility for collecting trace data from a behavior tree.
 */
object BtTracer {

    /**
     * Collect all node snapshots from a BT starting from the given root.
     *
     * @param root The root BtCommand
     * @param parentId The parent node's ID (null for root)
     * @return List of all node snapshots in the tree
     */
    fun collectNodeSnapshots(root: BtCommand<*>, parentId: String? = null): List<NodeSnapshot> {
        val snapshots = mutableListOf<NodeSnapshot>()
        collectNodeSnapshotsRecursive(root, parentId, snapshots)
        return snapshots
    }

    private fun collectNodeSnapshotsRecursive(
        node: BtCommand<*>,
        parentId: String?,
        snapshots: MutableList<NodeSnapshot>
    ) {
        val snapshot = NodeSnapshot(
            id = node.id,
            name = node.name,
            type = getNodeType(node),
            status = node.status.name,
            active = node.running,
            parentId = parentId
        )
        snapshots.add(snapshot)

        // Recurse into children based on node type
        when (node) {
            is Selector -> {
                val children = getChildren(node)
                children.forEach { child ->
                    collectNodeSnapshotsRecursive(child, node.id, snapshots)
                }
            }
            is Sequence -> {
                val children = getChildren(node)
                children.forEach { child ->
                    collectNodeSnapshotsRecursive(child, node.id, snapshots)
                }
            }
            is Parallel -> {
                val children = getChildren(node)
                children.forEach { child ->
                    collectNodeSnapshotsRecursive(child, node.id, snapshots)
                }
            }
            is Inverter -> {
                getDecoratorChild(node)?.let { child ->
                    collectNodeSnapshotsRecursive(child, node.id, snapshots)
                }
            }
            is Repeater -> {
                getDecoratorChild(node)?.let { child ->
                    collectNodeSnapshotsRecursive(child, node.id, snapshots)
                }
            }
            is Succeeder -> {
                getDecoratorChild(node)?.let { child ->
                    collectNodeSnapshotsRecursive(child, node.id, snapshots)
                }
            }
            is UntilFail -> {
                getDecoratorChild(node)?.let { child ->
                    collectNodeSnapshotsRecursive(child, node.id, snapshots)
                }
            }
            // Leaves don't have children
        }
    }

    /**
     * Collect the active path from root to the currently executing leaf.
     *
     * @param root The root BtCommand
     * @return List of node names from root to active leaf
     */
    fun collectActivePath(root: BtCommand<*>): List<String> {
        val path = mutableListOf<String>()
        collectActivePathRecursive(root, path)
        return path
    }

    private fun collectActivePathRecursive(node: BtCommand<*>, path: MutableList<String>): Boolean {
        if (!node.running && node.status == BtStatus.RUNNING) {
            // Not yet started
            return false
        }

        path.add(node.name)

        if (node.running) {
            // Check children
            when (node) {
                is Selector -> {
                    val children = getChildren(node)
                    for (child in children) {
                        if (collectActivePathRecursive(child, path)) {
                            return true
                        }
                    }
                }
                is Sequence -> {
                    val children = getChildren(node)
                    for (child in children) {
                        if (collectActivePathRecursive(child, path)) {
                            return true
                        }
                    }
                }
                is Parallel -> {
                    val children = getChildren(node)
                    for (child in children) {
                        if (collectActivePathRecursive(child, path)) {
                            return true
                        }
                    }
                }
                is Inverter -> {
                    getDecoratorChild(node)?.let { child ->
                        collectActivePathRecursive(child, path)
                    }
                }
                is Repeater -> {
                    getDecoratorChild(node)?.let { child ->
                        collectActivePathRecursive(child, path)
                    }
                }
                is Succeeder -> {
                    getDecoratorChild(node)?.let { child ->
                        collectActivePathRecursive(child, path)
                    }
                }
                is UntilFail -> {
                    getDecoratorChild(node)?.let { child ->
                        collectActivePathRecursive(child, path)
                    }
                }
            }
            return true
        }

        return false
    }

    private fun getNodeType(node: BtCommand<*>): String {
        return node::class.simpleName ?: "Unknown"
    }

    @Suppress("UNCHECKED_CAST")
    private fun getChildren(node: BtCommand<*>): List<BtCommand<*>> {
        return try {
            val field = node::class.java.getDeclaredField("children")
            field.isAccessible = true
            field.get(node) as? List<BtCommand<*>> ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDecoratorChild(node: BtCommand<*>): BtCommand<*>? {
        return try {
            val field = node::class.java.getDeclaredField("child")
            field.isAccessible = true
            field.get(node) as? BtCommand<*>
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Emit a behavior tree trace to connected Seek clients.
     *
     * @param tree The behavior tree to trace
     */
    fun emitTrace(tree: BehaviorTree) {
        val trace = BehaviorTreeTrace(
            treeId = tree.root.id,
            treeName = tree.name,
            activeNodePath = collectActivePath(tree.root),
            nodes = collectNodeSnapshots(tree.root),
            blackboard = tree.blackboard.data
        )
        Server.send(trace)
    }
}
