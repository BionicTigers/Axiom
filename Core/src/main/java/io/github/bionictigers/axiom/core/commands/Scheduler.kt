package io.github.bionictigers.axiom.core.commands

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.Scheduler.getPersistentState
import io.github.bionictigers.axiom.core.commands.Scheduler.persistentStates
import io.github.bionictigers.axiom.core.web.Editable
import io.github.bionictigers.axiom.core.web.Value
import io.github.bionictigers.axiom.core.utils.convertTo
import io.github.bionictigers.axiom.core.utils.getAnnotationOnProperty
import io.github.bionictigers.axiom.core.utils.hasAnnotationOnProperty
import io.github.bionictigers.axiom.core.web.Display
import io.github.bionictigers.axiom.core.web.Hidden
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.serializable.ObjectType
import io.github.bionictigers.axiom.core.web.serializable.Schedulable
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesInitial
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesUpdate
import io.github.bionictigers.axiom.core.web.serializable.StateUpdate
import org.firstinspires.ftc.robotcore.external.Telemetry
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.measureTime

typealias GenericCommand = Command<out BaseCommandState>

object Scheduler {
    private val commands = ConcurrentHashMap<String, GenericCommand>()
    private val sortedCommands = ArrayList<GenericCommand>()

    private val commandSnapshots: ConcurrentHashMap<String, Schedulable> = ConcurrentHashMap()

    private val systems = ConcurrentHashMap<String, System>()
//    private val systemsToCommands = ConcurrentHashMap<Int, Int>()

    private val addQueue: ConcurrentLinkedQueue<GenericCommand> = ConcurrentLinkedQueue()
    private val removeQueue: ConcurrentLinkedQueue<GenericCommand> = ConcurrentLinkedQueue()
    private val editQueue: ConcurrentLinkedQueue<Pair<String, Any>> = ConcurrentLinkedQueue()

    var tick = 0L

    val persistentStates = ConcurrentHashMap<String, BaseCommandState>()

    var telemetry: Telemetry? = null

    private var changed = false
    private var inUpdateCycle = false

    var loopDeltaTime = Duration.ZERO

    init {
        Server.onNewConnection { send ->
            send(SchedulablesInitial(serialize()) )
        }
    }

    /**
     * Adds commands to the scheduler.
     *
     * Duplicate commands will be ignored.
     *
     * @param command The commands to add.
     * @see Command
     */
    fun schedule(vararg command: GenericCommand) {
        if (inUpdateCycle)
            addQueue.addAll(command)
        else {
            command.forEach {
                internalAdd(it)
            }
        }
    }

    /**
     * Adds systems to the scheduler.
     *
     * This is a convenience method that adds the beforeRun and afterRun commands of the system.
     *
     * @param system The systems to add.
     * @see System
     */
    fun schedule(vararg systems: System) {
        schedule(systems.flatMap {
            it.beforeRun?.parent = it
            it.afterRun?.parent = it
            listOfNotNull(it.beforeRun, it.afterRun)
        })
        systems.forEach {
            Scheduler.systems[it.id] = it
        }
    }

    fun schedule(commands: Collection<GenericCommand>) {
        commands.forEach {
            schedule(it)
        }
    }

    inline fun <reified T : BaseCommandState> getPersistentState(
        name: String,
        default: () -> T,
    ): T = (persistentStates[name] as? T) ?: default().also { persistentStates[name] = it }

    private fun serializeVariable(state: Any?, readOnly: Boolean): Any {
        return when (state) {
            is Number, is String, is Char, is Boolean -> Value(state, readOnly)
            is Collection<*> -> state.map { return@map serializeVariable(it!!, false) }
            else -> {
                if (state == null) return Value(null, readOnly)
                val map = hashMapOf<String, Any?>()
                state::class.java.declaredFields.forEach { field ->
                    if (hasAnnotationOnProperty<Display>(state, field.name)) {
                        field.isAccessible = true
                        map[field.name] = serializeVariable(field.get(state), readOnly || !hasAnnotationOnProperty<Editable>(state, field.name))
                    }
                }
                return map.ifEmpty { throw InternalError("Failed to serialize") }
            }
        }
    }

    private fun serializeState(cmdState: Any): Map<String, Any>? {
        val map = HashMap<String, Any>()

        (cmdState::class.java.declaredFields + cmdState::class.java.superclass.declaredFields).forEach {
            val hiddenAnnotation = getAnnotationOnProperty<Hidden>(cmdState, it.name)
            val isEditable = hasAnnotationOnProperty<Editable>(cmdState, it.name)
            if (it.isSynthetic || it.name == "name" || (hiddenAnnotation != null && hiddenAnnotation.exclude)) return@forEach
            it.isAccessible = true
            try {
                val value = it.get(cmdState)?.let { it1 -> serializeVariable(it1, !isEditable) }
                if (value != null) map[it.name] = value
            } catch(_: InternalError) {
//                println("Failed to serialize ${it.name} from ${cmdState.name} is ${it.type}")
            }
        }

        return map.ifEmpty { null }
    }

    fun serialize(): ArrayList<Schedulable> {
        val array = ArrayList<Schedulable>()
        array += commands.values.map {
            Schedulable(
                it.name,
                it.id,
                serializeState(it.state) ?: mapOf(),
                it.parent?.id,
                ObjectType.Command
            )
        }
        array += systems.values.map {
            Schedulable(
                it.name,
                it.id,
                serializeState(it) ?: mapOf(),
                null,
                ObjectType.System
            )
        }

        return array
    }

    private fun internalAdd(command: GenericCommand) {
        changed = true
        commands[command.id] = command
        command.internalEnter()
    }

    /**
     * Removes a command from the scheduler.
     *
     * If the command is not in the scheduler, it will be ignored.
     *
     * @param command The command to remove.
     * @see Command
     */
    fun remove(vararg command: GenericCommand) {
        command.forEach {
            if (it !in commands.values) {
                return
            }

            removeQueue.add(it)
        }
    }

    private fun internalRemove(command: GenericCommand) {
        changed = true
        commands.remove(command.id)
        command.dependencies.forEach { dep ->
            if (dep !in commands.values) {
                return
            }
            dep.dependencies.remove(command)
        }
        command.internalExit()
    }

    private fun sort() {
        val visited = HashSet<GenericCommand>()
        val stack = Stack<GenericCommand>()

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

    private fun topologicalSort(command: GenericCommand, visited: HashSet<GenericCommand>, stack: Stack<GenericCommand>) {
        visited.add(command)

        for (dependency in command.dependencies) {
            if (dependency !in visited) {
                topologicalSort(dependency, visited, stack)
            }
        }

        stack.push(command)
    }

    fun edit(path: String, value: Any) {
        if (inUpdateCycle)
            editQueue.add(path to value)
        else
            internalEdit(path to value)
    }

    //TODO: Make more universal/delegate
    private fun internalEdit(edit: Pair<String, Any>) {
        val (path, value) = edit
        val splitPath = path.split(".")
        val type = if (splitPath[0] == "Command") commands else systems
        val command = type[splitPath[1]] ?: return

        //Unsafe magic!
        var index = 2
        try {
            var obj: Any = if (splitPath[0] == "Command") (command as GenericCommand).state else command
            lateinit var field: Field
            splitPath.subList(2, splitPath.size).forEach { fieldName ->
                field = obj::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                index++
                if (splitPath.size != index) obj = field.get(obj)!!
            }

            val target = field.get(obj)!!

            field.set(obj, (value as String).convertTo(target::class))
        } catch(e: NoSuchFieldException) {
            RobotLog.ww("Axiom", "Unable to modify $path, failed to find $index: ${splitPath[index]} in ${splitPath[index - 1]}.")
        } catch(e: NumberFormatException) {
            RobotLog.ww("Axiom", "Unable to set $path to $value as they do not share a type.")
        } catch(e: IllegalArgumentException) {
            RobotLog.ww("Axiom", "Unable to set $path to $value as they do not share a type.")
        }
    }

    /**
     * Calculates the list of schedulables that have changed (excluding the state) since the last update.
     */
    private fun calculateSchedulableDeltas(): Pair<List<Schedulable>, Set<String>> {
        val deltas = mutableListOf<Schedulable>()
        val current = serialize()

        current.forEach { schedulable ->
            val id = schedulable.id
            val previous = commandSnapshots[id]
            if (previous == null) {
                // New command/system, no delta to report
                deltas += schedulable
            } else {
                if (previous.name != schedulable.name ||
                    previous.parent != schedulable.parent ||
                    previous.type != schedulable.type) {
                    deltas += schedulable
                }
            }
        }

        val removals = commandSnapshots.keys.subtract(current.map { it.id })
        removals.forEach { commandSnapshots.remove(it) }

        return Pair(deltas, removals)
    }

    private fun calculateStateDelta(): List<Pair<String, Map<String, Any?>>> {
        //Use a list instead of a map to preserve order
        val deltas = mutableListOf<Pair<String, Map<String, Any?>>>()
        val current = serialize()

        current.forEach { schedulable ->
            val id = schedulable.id
            val previous = commandSnapshots[id]
            if (previous == null) {
                // New command/system, no delta to report
                deltas += id to schedulable.state
            } else {
                val currentState = schedulable.state
                val diff = mutableMapOf<String, Any?>()
                currentState.forEach { (key, value) ->
                    if (previous.state[key] != value) {
                        diff[key] = value
                    }
                }

                if (diff.isNotEmpty()) {
                    deltas += id to diff
                }
            }

            commandSnapshots[id] = schedulable
        }

        return deltas
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

        loopDeltaTime = measureTime {
            editQueue.forEachAndRemove(this::internalEdit)
            addQueue.forEachAndRemove(this::internalAdd)

            if (changed) {
                sort()
                changed = false
            }

            sortedCommands.forEach(GenericCommand::execute)

            removeQueue.forEachAndRemove(this::internalRemove)
        }

        if (telemetry != null) {
            telemetry!!.addData("Scheduler Loop Time", loopDeltaTime)
        }

        val (schedulableDeltas, removalDeltas) = calculateSchedulableDeltas()
        if (schedulableDeltas.isNotEmpty() || removalDeltas.isNotEmpty()) {
            Server.send(SchedulablesUpdate(schedulableDeltas, removalDeltas))
        }

        val stateDeltas = calculateStateDelta()
        if (stateDeltas.isNotEmpty()) {
            Server.send(StateUpdate(stateDeltas))
        }

        tick++
        inUpdateCycle = false
    }

    fun reset() {
        commands.clear()
        sortedCommands.clear()
        addQueue.clear()
        removeQueue.clear()
        telemetry = null
    }
}

inline fun <reified T : BaseCommandState> persistentState(
    name: String,
    noinline default: () -> T
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {

    private var cache: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (cache == null) {
            cache = getPersistentState(name, default)
        }
        return cache!!
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        cache = value
        persistentStates[name] = value
    }
}

private fun <T> ConcurrentLinkedQueue<T>.forEachAndRemove(action: (T) -> Unit) {
    while (isNotEmpty()) {
        action(poll() ?: continue)
    }
}