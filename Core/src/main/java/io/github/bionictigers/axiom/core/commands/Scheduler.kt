package io.github.bionictigers.axiom.core.commands

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.Scheduler.getPersistentState
import io.github.bionictigers.axiom.core.commands.Scheduler.persistentStates
import io.github.bionictigers.axiom.core.web.Editable
import io.github.bionictigers.axiom.core.web.Value
import io.github.bionictigers.axiom.core.utils.convertTo
import io.github.bionictigers.axiom.core.utils.findPropertyInHierarchy
import io.github.bionictigers.axiom.core.web.Display
import io.github.bionictigers.axiom.core.web.Hidden
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.ValueMetadata
import io.github.bionictigers.axiom.core.web.serializable.ObjectType
import io.github.bionictigers.axiom.core.web.serializable.Schedulable
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesInitial
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesUpdate
import io.github.bionictigers.axiom.core.web.serializable.SchedulerOrder
import io.github.bionictigers.axiom.core.web.serializable.StateUpdate
import org.firstinspires.ftc.robotcore.external.Telemetry
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration
import kotlin.time.measureTime

typealias GenericCommand = Command<out BaseCommandState>

private class SerializationUndefined(message: String) : RuntimeException(message, null, false, false)

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
            send(SchedulerOrder(sortedCommands.map { it.id }))
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

    private fun serializeVariable(state: Any?, metadata: ValueMetadata): Any =
        when (state) {
            is Number, is String, is Char, is Boolean -> Value(state, metadata)
            is Duration -> Value(state.inWholeMilliseconds, metadata)
            is Collection<*> -> state.map { return@map serializeVariable(it!!, metadata.copy(false)) }
            is Array<*> -> state.map { return@map serializeVariable(it!!, metadata.copy(false)) }
            null -> Value(null, metadata)
            else -> {
                val map = hashMapOf<String, Any?>()

                state::class.memberProperties.forEach { prop ->
                    val displayAnnotation = prop.findAnnotation<Display>()
                    if (displayAnnotation != null) {
                        prop.isAccessible = true
                        val value = prop.getter.call(state)
                        val newMetadata = metadata.copy(metadata.readOnly || (prop.findAnnotation<Editable>() == null), displayAnnotation.priority)
                        map[prop.name] = serializeVariable(value, newMetadata)
                    }
                }

                map.ifEmpty { throw SerializationUndefined("Failed to serialize: $state") }
            }
        }

    private fun serializeState(cmdState: Any): Map<String, Any>? {
        val map = HashMap<String, Any>()

        cmdState::class.memberProperties.forEach { prop ->
            val hiddenAnnotation = prop.findAnnotation<Hidden>()
            if (hiddenAnnotation?.exclude == true) return@forEach

            val metadata = ValueMetadata(
                prop.findAnnotation<Editable>() == null,
                prop.findAnnotation<Display>()?.priority ?: 0,
                hiddenAnnotation != null
            )

            prop.isAccessible = true
            val value = prop.getter.call(cmdState)
            try {
                if (value != null)
                    map[prop.name] = serializeVariable(value, metadata)
            } catch (e: SerializationUndefined) {}
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
        val onPath = HashSet<GenericCommand>()
        val stack = mutableListOf<GenericCommand>()

        val allNodes = buildSet {
            addAll(commands.values)
            commands.values.forEach { addAll(it.dependencies) }
        }

        for (node in allNodes.sortedBy { it.id }) {
            if (node !in visited) dfs(node, visited, onPath, stack)
        }

        sortedCommands.clear()
        while (stack.isNotEmpty()) sortedCommands.add(stack.removeAt(stack.lastIndex))

        Server.send(SchedulerOrder(sortedCommands.map { it.id }))
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
        for (dependency in node.dependencies)
            dfs(dependency, visited, onPath, out)
        onPath.remove(node)

        visited.add(node)
        out.add(0, node)
    }


    fun edit(path: String, value: Any) {
        if (inUpdateCycle)
            editQueue.add(path to value)
        else
            internalEdit(path to value)
    }

    private fun internalEdit(edit: Pair<String, Any>) {
        val (path, rawValue) = edit
        val parts = path.split(".")
        if (parts.size < 3) {
            RobotLog.ww("Axiom", "Unable to modify $path: path too short.")
            return
        }

        val isCommand = parts[0].equals("command", ignoreCase = true)
        val id = parts[1]
        val store = if (isCommand) commands else systems
        val schedulable = store[id] ?: return

        var obj: Any = if (isCommand) (schedulable as GenericCommand).state else schedulable
        val segments = parts.subList(2, parts.size)

        try {
            // Walk to the parent object of the leaf
            segments.dropLast(1).forEachIndexed { index, segment ->
                val prop = obj::class.findPropertyInHierarchy(segment) ?: throw NoSuchFieldException(segment)
                prop.isAccessible = true
                obj = prop.get(obj) ?: throw IllegalStateException(
                    "Property ${segments.take(index + 1).joinToString(".")} is null"
                )
            }

            val leafName = segments.last()
            val maybeIndex = leafName.toIntOrNull()

            // Case 1: parent is indexable and last segment is an index
            if (maybeIndex != null) {
                val idx = maybeIndex
                when (obj) {
                    is MutableList<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val list = obj as MutableList<Any?>
                        require(idx in 0..list.lastIndex) { "Index $idx out of bounds (size ${list.size})" }
                        val current = list[idx]
                        val targetKClass = current?.let { it::class } ?: Any::class
                        val converted = (rawValue as String).convertTo(targetKClass)
                        list[idx] = converted
                    }
                    is Array<*> -> {
                        val component = obj.javaClass.componentType.kotlin
                        val converted = (rawValue as String).convertTo(component)
                        java.lang.reflect.Array.set(obj, idx, converted)
                    }
                    is IntArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).toInt() }
                    is LongArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).toLong() }
                    is DoubleArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).toDouble() }
                    is FloatArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).toFloat() }
                    is ShortArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).toShort() }
                    is ByteArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).toByte() }
                    is CharArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).single() }
                    is BooleanArray -> { require(idx in obj.indices); obj[idx] = (rawValue as String).toBooleanStrict() }
                    else -> throw IllegalStateException("Tried to index into ${obj::class.simpleName}, which is not indexable")
                }
                return
            }

            // Case 2: normal property set
            val leaf = obj::class.findPropertyInHierarchy(leafName) ?: throw NoSuchFieldException(leafName)
            @Suppress("UNCHECKED_CAST")
            val mutable = leaf as? KMutableProperty1<Any, Any?>
                ?: throw IllegalStateException("Property '$leafName' is not mutable")
            val targetKClass = (leaf.returnType.classifier as? KClass<*>) ?: Any::class
            val converted = (rawValue as String).convertTo(targetKClass)

            mutable.isAccessible = true
            mutable.set(obj, converted)   // IMPORTANT: pass receiver + value
        } catch (e: NoSuchFieldException) {
            RobotLog.ww("Axiom", "Unable to modify $path: no property '${e.message}' on '${obj::class.simpleName}'.")
        } catch (e: IllegalStateException) {
            RobotLog.ww("Axiom", "Unable to modify $path: ${e.message}")
        } catch (e: NumberFormatException) {
            RobotLog.ww("Axiom", "Unable to set $path to $rawValue: type conversion failed.")
        } catch (e: IllegalArgumentException) {
            RobotLog.ww("Axiom", "Unable to set $path to $rawValue: incompatible type.")
        } catch (e: Exception) {
            RobotLog.ww("Axiom", "Unable to modify $path: ${e::class.simpleName}: ${e.message}")
        }
    }


    /**
     * Calculates the list of schedulables that have changed (excluding the state) since the last update.
     */
    private fun calculateSchedulableDeltas(current: ArrayList<Schedulable>): Pair<List<Schedulable>, Set<String>> {
        val deltas = mutableListOf<Schedulable>()

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

    private fun calculateStateDelta(current: ArrayList<Schedulable>): List<Pair<String, Map<String, Any?>>> {
        //Use a list instead of a map to preserve order
        val deltas = mutableListOf<Pair<String, Map<String, Any?>>>()

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

        val serialized = serialize()
        val (schedulableDeltas, removalDeltas) = calculateSchedulableDeltas(serialized)
        if (schedulableDeltas.isNotEmpty() || removalDeltas.isNotEmpty()) {
            Server.send(SchedulablesUpdate(schedulableDeltas, removalDeltas))
        }

        val stateDeltas = calculateStateDelta(serialized)
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