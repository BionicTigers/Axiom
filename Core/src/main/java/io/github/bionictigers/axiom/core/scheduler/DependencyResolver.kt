package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.GenericCommand
import java.util.HashSet

/**
 * internal object DependencyResolver {
 *     fun sort(commands: Map<String, GenericCommand>): List<GenericCommand>
 *
 *     private fun dfs(...)
 * }
 */
internal object DependencyResolver {
    fun sort(commands: Map<String, GenericCommand>): List<GenericCommand> {
        val visited = HashSet<GenericCommand>()
        val onPath = HashSet<GenericCommand>()
        val stack = mutableListOf<GenericCommand>()

        val allNodes = buildSet {
            addAll(commands.values)
            commands.values.forEach {
                command -> addAll(command.dependencies.mapNotNull { it.get() })
            }
        }

        for (node in allNodes.sortedBy { it.id }) {
            if (node !in visited) dfs(node, visited, onPath, stack)
        }

        val sortedCommands = mutableListOf<GenericCommand>()
        while (stack.isNotEmpty()) sortedCommands.add(stack.removeAt(stack.lastIndex))

        return sortedCommands
    }

    private fun dfs(
        node: GenericCommand,
        visited: MutableSet<GenericCommand>,
        onPath: MutableSet<GenericCommand>,
        out: MutableList<GenericCommand>
    ) {
        if (node in onPath) {
            throw IllegalStateException("Cyclic dependency detected at ${node.name} (${node.id})")
        }
        if (node in visited) return

        onPath.add(node)
        for (dependency in node.dependencies.mapNotNull { it.get() })
            dfs(dependency, visited, onPath, out)
        onPath.remove(node)

        visited.add(node)
        out.add(0, node)
    }
}