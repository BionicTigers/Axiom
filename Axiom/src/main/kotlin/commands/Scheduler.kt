package commands

import io.javalin.plugin.bundled.RouteOverviewUtil.metaInfo
import io.javalin.util.JavalinLogger.warn
import utils.Time
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.kotlinProperty
import kotlin.reflect.typeOf

object Scheduler {
    private val commands = HashMap<Int, Command<*>>()
    private val sortedCommands = ArrayList<Command<*>>()

    private val addQueue: ArrayList<Command<*>> = ArrayList()
    private val removeQueue: ArrayList<Command<*>> = ArrayList()

    private var changed = false

    var loopDeltaTime = Time()

    /**
     * Adds commands to the scheduler.
     *
     * Duplicate commands will be ignored.
     *
     * @param command The commands to add.
     * @see Command
     */
    fun add(vararg command: Command<*>) {
        addQueue.addAll(command)
    }

    fun add(commands: Collection<Command<*>>) {
        addQueue.addAll(commands)
    }

    private fun serializeVariable(state: Any): Any {
        val readOnly = state !is KMutableProperty<*>
        return when (state) {
            is Display -> state.serialize()
            is Number, is String, is Char -> return Value(state, readOnly)
            is Collection<*> -> return state.map { return@map serializeVariable(it!!) }
            else -> {
                throw InternalError("Failed to serialize")
            }
        }
    }

    private fun serializeState(cmdState: CommandState): Map<String, Any> {
        val map = HashMap<String, Any>()

        val fields = cmdState::class.java.declaredFields
        fields.forEach {
            if (it.isSynthetic || it.name == "name") return@forEach
            it.isAccessible = true
            try {
                map[it.name] = it.get(cmdState)?.let { it1 -> serializeVariable(it1) }!!
            } catch(_: InternalError) {
                warn("Failed to serialize ${it.name} from ${cmdState.name} is ${it.type}")
            }
        }

        return map
    }

    private fun serializeCommands(): ArrayList<Map<String, Any>> {
        val array = ArrayList<Map<String, Any>>()
        commands.values.forEachIndexed { _, value ->
            array.add(mapOf("name" to value.state.name, "state" to serializeState(value.state)))
        }

        return array
    }

    fun serialize(): Map<String, Any> {
        return mapOf("type" to "cycle", "commands" to serializeCommands())
    }

    private fun internalAdd(command: Command<*>) {
        changed = true
        commands[command.hashCode()] = command
    }

    /**
     * Adds systems to the scheduler.
     *
     * This is a convenience method that adds the beforeRun and afterRun commands of the system.
     *
     * @param system The systems to add.
     * @see System
     */
    fun addSystem(vararg system: System) {
        addQueue.addAll(system.mapNotNull { it.beforeRun })
        addQueue.addAll(system.mapNotNull { it.afterRun })
    }

    /**
     * Removes a command from the scheduler.
     *
     * If the command is not in the scheduler, it will be ignored.
     *
     * @param command The command to remove.
     * @see Command
     */
    fun remove(vararg command: Command<*>) {
        command.forEach {
            if (it !in commands.values) {
                return
            }

            removeQueue.add(it)
        }
    }

    private fun internalRemove(command: Command<*>) {
        changed = true
        commands.remove(command.hashCode())
    }

    private fun sort() {
        val visited = HashSet<Command<*>>()
        val stack = Stack<Command<*>>()

        for (command in commands.values) {
            if (command !in visited) {
                topologicalSort(command, visited, stack)
            }
        }

        sortedCommands.clear()
        while (stack.isNotEmpty()) {
            sortedCommands.add(stack.pop())
        }
    }

    private fun topologicalSort(command: Command<*>, visited: HashSet<Command<*>>, stack: Stack<Command<*>>) {
        visited.add(command)

        for (dependency in command.dependencies) {
            if (dependency !in visited) {
                topologicalSort(dependency, visited, stack)
            }
        }

        stack.push(command)
    }

    /**
     * Sorts and executes all commands.
     *
     * This should be the only method called in the main loop.
     *
     * @see Command
     */
    fun update() {
        val startTime = java.lang.System.currentTimeMillis()

        addQueue.forEach(this::internalAdd)
        addQueue.clear()

        if (changed) {
            sort()
            changed = false
        }

        sortedCommands.forEach(Command<*>::execute)

        removeQueue.forEach(this::internalRemove)
        removeQueue.clear()

        loopDeltaTime = Time.fromMilliseconds(java.lang.System.currentTimeMillis() - startTime)
    }

    fun clear() {
        commands.clear()
        sortedCommands.clear()
        addQueue.clear()
        removeQueue.clear()
    }
}