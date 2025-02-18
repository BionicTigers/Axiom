package io.github.bionictigers.axiom.commands

import io.github.bionictigers.axiom.utils.Time
import io.github.bionictigers.axiom.web.Hidden
import io.github.bionictigers.axiom.web.Server
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation

object Scheduler {
    private val commands = HashMap<Int, Command<*>>()
    private val sortedCommands = ArrayList<Command<*>>()

    private val addQueue: ArrayList<Command<*>> = ArrayList()
    private val removeQueue: ArrayList<Command<*>> = ArrayList()

    private var changed = false
    private var inUpdateCycle = false

    var loopDeltaTime = Time()

    init {
        println("starting Server")
        Server.start()
    }

    /**
     * Adds commands to the scheduler.
     *
     * Duplicate commands will be ignored.
     *
     * @param command The commands to add.
     * @see Command
     */
    fun add(vararg command: Command<*>) {
        if (inUpdateCycle)
            addQueue.addAll(command)
        else {
            command.forEach {
                internalAdd(it)
            }
        }
    }

    fun add(commands: Collection<Command<*>>) {
        commands.forEach {
            add(it)
        }
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
        fields.forEach { field ->
            // Skip synthetic fields, the "name" field, or any field annotated with @Hidden
            println(field.isAnnotationPresent(Hidden::class.java))
            if (field.isSynthetic || field.name == "name" || field.isAnnotationPresent(Hidden::class.java)) {
                return@forEach
            }
            field.isAccessible = true
            try {
                val value = field.get(cmdState)?.let { serializeVariable(it) }
                if (value != null) {
                    map[field.name] = value
                }
            } catch (_: InternalError) {
                // Handle errors if needed
            }
        }

        return map
    }

    fun serialize(): ArrayList<Map<String, Any>> {
        val array = ArrayList<Map<String, Any>>()
        commands.values.forEachIndexed { _, value ->
            array.add(mapOf("name" to value.state.name, "state" to serializeState(value.state)))
        }

        return array
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
        add(system.mapNotNull { it.beforeRun })
        add(system.mapNotNull { it.afterRun })
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
        inUpdateCycle = true
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
        inUpdateCycle = false
    }

    fun clear() {
        commands.clear()
        sortedCommands.clear()
        addQueue.clear()
        removeQueue.clear()
    }
}