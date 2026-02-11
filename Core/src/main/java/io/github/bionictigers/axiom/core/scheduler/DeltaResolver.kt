package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.System
import io.github.bionictigers.axiom.core.web.Display
import io.github.bionictigers.axiom.core.web.Editable
import io.github.bionictigers.axiom.core.web.Hidden
import io.github.bionictigers.axiom.core.web.Value
import io.github.bionictigers.axiom.core.web.ValueMetadata
import io.github.bionictigers.axiom.core.web.serializable.ObjectType
import io.github.bionictigers.axiom.core.web.serializable.Schedulable
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration

internal object DeltaResolver {

    private class SerializationException(message: String) : RuntimeException(message)

    private val snapshots = ConcurrentHashMap<String, Schedulable>()
    private val snapshotNodes = ConcurrentHashMap<String, SnapshotSchedulable>()

    // Caching for Reflection
    private data class CachedProperty(
        val prop: KProperty1<Any, *>,
        // Pre-calculated metadata for top-level state
        val staticMetadata: ValueMetadata? = null,
        // Cached annotation values for nested objects
        val isEditable: Boolean = false,
        val priority: Int = 0
    )

    private val stateCache = Collections.synchronizedMap(WeakHashMap<KClass<*>, List<CachedProperty>>())
    private val objectCache = Collections.synchronizedMap(WeakHashMap<KClass<*>, List<CachedProperty>>())

    private inline fun <reified T : Annotation> findAnnotationInHierarchy(
        prop: KProperty1<*, *>,
        kClass: KClass<*>
    ): T? {
        return prop.findAnnotation<T>() ?: kClass.allSuperclasses.firstNotNullOfOrNull { superClass ->
            superClass.memberProperties.find { it.name == prop.name }?.findAnnotation<T>()
        }
    }

    private fun getStateProperties(kClass: KClass<*>): List<CachedProperty> {
        return stateCache.getOrPut(kClass) {
            val props = ArrayList<CachedProperty>()
            kClass.memberProperties.forEach { prop ->
                val hidden = findAnnotationInHierarchy<Hidden>(prop, kClass)
                if (hidden?.exclude == true) return@forEach

                val editable = findAnnotationInHierarchy<Editable>(prop, kClass)
                val display = findAnnotationInHierarchy<Display>(prop, kClass)

                val metadata = ValueMetadata(
                    editable == null,
                    display?.priority ?: 0,
                    hidden != null
                )
                prop.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                props.add(CachedProperty(prop as KProperty1<Any, *>, staticMetadata = metadata))
            }
            props
        }
    }

    private fun getObjectProperties(kClass: KClass<*>): List<CachedProperty> {
        return objectCache.getOrPut(kClass) {
            val props = ArrayList<CachedProperty>()
            kClass.memberProperties.forEach { prop ->
                val display = findAnnotationInHierarchy<Display>(prop, kClass) ?: return@forEach
                prop.isAccessible = true

                val editable = findAnnotationInHierarchy<Editable>(prop, kClass)

                @Suppress("UNCHECKED_CAST")
                props.add(
                    CachedProperty(
                        prop as KProperty1<Any, *>,
                        isEditable = editable != null,
                        priority = display.priority
                    )
                )
            }
            props
        }
    }

    data class DeltaReport(
        val structureUpdates: List<Schedulable>,
        val removals: Set<String>,
        val stateUpdates: List<Pair<String, Map<String, Any?>>>,
        val stateUpdateCount: Int,
        val fieldUpdateCount: Int
    )

    data class SnapshotSchedulable(
        val name: String,
        val id: String,
        val state: Map<String, SnapshotNode>,
        val parent: String?,
        val type: ObjectType
    )

    sealed interface SnapshotNode

    private data class SnapshotValue(
        val value: Any?,
        val metadata: ValueMetadata
    ) : SnapshotNode

    private data class SnapshotList(
        val items: List<SnapshotNode>
    ) : SnapshotNode

    private data class SnapshotMap(
        val entries: Map<String, SnapshotNode>
    ) : SnapshotNode

    fun snapshot(
        commands: Map<String, Command<*>>,
        systems: Map<String, System>
    ): ArrayList<SnapshotSchedulable> {
        val result = ArrayList<SnapshotSchedulable>(commands.size + systems.size)

        result.addAll(commands.values.map { cmd ->
            val previous = snapshotNodes[cmd.id]
            val userState = cmd.state?.let { snapshotStateIncremental(it, previous?.state) } ?: emptyMap()
            val metaState = snapshotStateIncremental(cmd.meta, previous?.state) ?: emptyMap()

            SnapshotSchedulable(
                cmd.name,
                cmd.id,
                userState + metaState,
                cmd.parent?.id,
                ObjectType.Command
            )
        })

        result.addAll(systems.values.map { sys ->
            val previous = snapshotNodes[sys.id]
            SnapshotSchedulable(
                sys.name,
                sys.id,
                snapshotStateIncremental(sys, previous?.state) ?: emptyMap(),
                null,
                ObjectType.System
            )
        })

        return result
    }

    fun serializeSnapshot(snapshot: List<SnapshotSchedulable>): ArrayList<Schedulable> {
        val result = ArrayList<Schedulable>(snapshot.size)

        snapshot.forEach { schedulable ->
            result.add(serializeSnapshotSchedulable(schedulable))
        }

        return result
    }

    fun resolveSnapshot(
        current: List<SnapshotSchedulable>,
        allowRemovals: Boolean = true
    ): DeltaReport {
        val structureUpdates = ArrayList<Schedulable>()
        val stateUpdates = ArrayList<Pair<String, Map<String, Any?>>>()
        var stateUpdateCount = 0
        var fieldUpdateCount = 0

        current.forEach { schedulable ->
            val id = schedulable.id
            val previous = snapshotNodes[id]

            if (previous == null ||
                previous.name != schedulable.name ||
                previous.parent != schedulable.parent ||
                previous.type != schedulable.type
            ) {
                structureUpdates.add(serializeSnapshotSchedulable(schedulable))
            }
        }

        val removals = if (allowRemovals) {
            val currentIds = current.mapTo(HashSet(current.size)) { it.id }
            val removed = snapshotNodes.keys.subtract(currentIds)
            removed.forEach { snapshotNodes.remove(it) }
            removed
        } else {
            emptySet()
        }

        current.forEach { schedulable ->
            val id = schedulable.id
            val previous = snapshotNodes[id]

            if (previous == null) {
                if (schedulable.state.isNotEmpty()) {
                    val state = snapshotStateToSerializable(schedulable.state)
                    stateUpdates.add(id to state)
                    stateUpdateCount++
                    fieldUpdateCount += state.size
                }
            } else {
                val diff = HashMap<String, Any?>()
                schedulable.state.forEach { (key, value) ->
                    if (previous.state[key] != value) {
                        diff[key] = snapshotNodeToSerializable(value)
                    }
                }
                if (diff.isNotEmpty()) {
                    stateUpdates.add(id to diff)
                    stateUpdateCount++
                    fieldUpdateCount += diff.size
                }
            }
            snapshotNodes[id] = schedulable
        }

        return DeltaReport(structureUpdates, removals, stateUpdates, stateUpdateCount, fieldUpdateCount)
    }

    fun ensureSnapshotNodes(
        commands: Map<String, Command<*>>,
        systems: Map<String, System>
    ) {
        commands.values.forEach { cmd ->
            snapshotNodes.putIfAbsent(
                cmd.id,
                SnapshotSchedulable(
                    cmd.name,
                    cmd.id,
                    emptyMap(),
                    cmd.parent?.id,
                    ObjectType.Command
                )
            )
        }

        systems.values.forEach { sys ->
            snapshotNodes.putIfAbsent(
                sys.id,
                SnapshotSchedulable(
                    sys.name,
                    sys.id,
                    emptyMap(),
                    null,
                    ObjectType.System
                )
            )
        }
    }

    fun resolve(current: List<Schedulable>): DeltaReport {
        val structureUpdates = ArrayList<Schedulable>()
        val stateUpdates = ArrayList<Pair<String, Map<String, Any?>>>()
        var stateUpdateCount = 0
        var fieldUpdateCount = 0

        current.forEach { schedulable ->
            val id = schedulable.id
            val previous = snapshots[id]

            if (previous == null ||
                previous.name != schedulable.name ||
                previous.parent != schedulable.parent ||
                previous.type != schedulable.type
            ) {
                structureUpdates.add(schedulable)
            }
        }

        val currentIds = current.mapTo(HashSet(current.size)) { it.id }
        val removals = snapshots.keys.subtract(currentIds)
        removals.forEach { snapshots.remove(it) }

        current.forEach { schedulable ->
            val id = schedulable.id
            val previous = snapshots[id]

            if (previous == null) {
                // New, full state
                if (schedulable.state.isNotEmpty()) {
                    stateUpdates.add(id to schedulable.state)
                    stateUpdateCount++
                    fieldUpdateCount += schedulable.state.size
                }
            } else {
                val diff = HashMap<String, Any?>()
                schedulable.state.forEach { (key, value) ->
                    if (previous.state[key] != value) {
                        diff[key] = value
                    }
                }
                if (diff.isNotEmpty()) {
                    stateUpdates.add(id to diff)
                    stateUpdateCount++
                    fieldUpdateCount += diff.size
                }
            }
            // Update snapshot
            snapshots[id] = schedulable
        }

        return DeltaReport(structureUpdates, removals, stateUpdates, stateUpdateCount, fieldUpdateCount)
    }

    fun serialize(
        commands: Map<String, Command<*>>,
        systems: Map<String, System>
    ): ArrayList<Schedulable> {
        val result = ArrayList<Schedulable>(commands.size + systems.size)

        result.addAll(commands.values.map { cmd ->
            // Combine user state with meta information
            val userState = cmd.state?.let { serializeState(it) } ?: emptyMap()
            val metaState = serializeState(cmd.meta) ?: emptyMap()
            
            Schedulable(
                cmd.name,
                cmd.id,
                userState + metaState,
                cmd.parent?.id,
                ObjectType.Command
            )
        })

        result.addAll(systems.values.map { sys ->
            Schedulable(
                sys.name,
                sys.id,
                serializeState(sys) ?: emptyMap(),
                null,
                ObjectType.System
            )
        })

        return result
    }

    private fun snapshotState(cmdState: Any): Map<String, SnapshotNode>? {
        val map = HashMap<String, SnapshotNode>()

        getStateProperties(cmdState::class).forEach { (prop, metadata, _, _) ->
            try {
                val value = prop.getter.call(cmdState)
                if (value != null && metadata != null) {
                    map[prop.name] = snapshotVariable(value, metadata)
                }
            } catch (_: SerializationException) {
                // Skip properties that fail serialization
            }
        }

        return map.ifEmpty { null }
    }

    private fun snapshotStateIncremental(
        cmdState: Any,
        previous: Map<String, SnapshotNode>?
    ): Map<String, SnapshotNode>? {
        val map = HashMap<String, SnapshotNode>()

        getStateProperties(cmdState::class).forEach { (prop, metadata, _, _) ->
            try {
                val value = prop.getter.call(cmdState)
                if (value != null && metadata != null) {
                    val previousNode = previous?.get(prop.name)
                    map[prop.name] = snapshotVariableIncremental(previousNode, value, metadata)
                }
            } catch (_: SerializationException) {
                // Skip properties that fail serialization
            }
        }

        return map.ifEmpty { null }
    }

    private fun snapshotVariable(state: Any?, metadata: ValueMetadata): SnapshotNode {
        return when (state) {
            is Number, is String, is Char, is Boolean -> SnapshotValue(state, metadata)
            is Duration -> SnapshotValue(state.inWholeMilliseconds, metadata)
            is Map<*, *> -> SnapshotMap(state.entries.associate { (k, v) ->
                k.toString() to snapshotVariable(v, metadata.copy(readonly = true))
            })
            is Collection<*> -> SnapshotList(state.map {
                snapshotVariable(it!!, metadata.copy(readonly = false))
            })
            is Array<*> -> SnapshotList(state.map {
                snapshotVariable(it!!, metadata.copy(readonly = false))
            })
            null -> SnapshotValue(null, metadata)
            else -> snapshotObject(state, metadata)
        }
    }

    private fun snapshotVariableIncremental(
        previous: SnapshotNode?,
        state: Any?,
        metadata: ValueMetadata
    ): SnapshotNode {
        return when (state) {
            is Number, is String, is Char, is Boolean -> {
                val current = SnapshotValue(state, metadata)
                if (previous is SnapshotValue &&
                    previous.value == current.value &&
                    previous.metadata == current.metadata
                ) {
                    previous
                } else {
                    current
                }
            }
            is Duration -> {
                val current = SnapshotValue(state.inWholeMilliseconds, metadata)
                if (previous is SnapshotValue &&
                    previous.value == current.value &&
                    previous.metadata == current.metadata
                ) {
                    previous
                } else {
                    current
                }
            }
            is Map<*, *> -> {
                val previousMap = previous as? SnapshotMap
                val result = HashMap<String, SnapshotNode>(state.size)
                var unchanged = previousMap != null && previousMap.entries.size == state.size
                state.entries.forEach { (k, v) ->
                    val key = k.toString()
                    val prevNode = previousMap?.entries?.get(key)
                    val nextNode = snapshotVariableIncremental(prevNode, v, metadata.copy(readonly = true))
                    if (prevNode !== nextNode) {
                        unchanged = false
                    }
                    result[key] = nextNode
                }
                if (unchanged && previousMap != null) {
                    previousMap
                } else {
                    SnapshotMap(result)
                }
            }
            is Collection<*> -> {
                val previousList = previous as? SnapshotList
                val result = ArrayList<SnapshotNode>(state.size)
                var unchanged = previousList != null && previousList.items.size == state.size
                state.forEachIndexed { index, item ->
                    val prevNode = previousList?.items?.getOrNull(index)
                    val nextNode = snapshotVariableIncremental(prevNode, item!!, metadata.copy(readonly = false))
                    if (prevNode !== nextNode) {
                        unchanged = false
                    }
                    result.add(nextNode)
                }
                if (unchanged && previousList != null) {
                    previousList
                } else {
                    SnapshotList(result)
                }
            }
            is Array<*> -> {
                val previousList = previous as? SnapshotList
                val result = ArrayList<SnapshotNode>(state.size)
                var unchanged = previousList != null && previousList.items.size == state.size
                state.forEachIndexed { index, item ->
                    val prevNode = previousList?.items?.getOrNull(index)
                    val nextNode = snapshotVariableIncremental(prevNode, item!!, metadata.copy(readonly = false))
                    if (prevNode !== nextNode) {
                        unchanged = false
                    }
                    result.add(nextNode)
                }
                if (unchanged && previousList != null) {
                    previousList
                } else {
                    SnapshotList(result)
                }
            }
            null -> {
                val current = SnapshotValue(null, metadata)
                if (previous is SnapshotValue &&
                    previous.value == current.value &&
                    previous.metadata == current.metadata
                ) {
                    previous
                } else {
                    current
                }
            }
            else -> snapshotObjectIncremental(previous, state, metadata)
        }
    }

    private fun snapshotObject(state: Any, metadata: ValueMetadata): SnapshotNode {
        val map = HashMap<String, SnapshotNode>()

        getObjectProperties(state::class).forEach { (prop, _, isEditable, priority) ->
            val value = prop.getter.call(state)

            val newMetadata = metadata.copy(
                readonly = metadata.readonly || !isEditable,
                priority = priority
            )

            map[prop.name] = snapshotVariable(value, newMetadata)
        }

        if (map.isEmpty()) {
            throw SerializationException("Failed to serialize object: $state")
        }
        return SnapshotMap(map)
    }

    private fun snapshotObjectIncremental(
        previous: SnapshotNode?,
        state: Any,
        metadata: ValueMetadata
    ): SnapshotNode {
        val previousMap = previous as? SnapshotMap
        val map = HashMap<String, SnapshotNode>()
        var unchanged = previousMap != null

        getObjectProperties(state::class).forEach { (prop, _, isEditable, priority) ->
            val value = prop.getter.call(state)

            val newMetadata = metadata.copy(
                readonly = metadata.readonly || !isEditable,
                priority = priority
            )

            val prevNode = previousMap?.entries?.get(prop.name)
            val nextNode = snapshotVariableIncremental(prevNode, value, newMetadata)
            if (prevNode !== nextNode) {
                unchanged = false
            }
            map[prop.name] = nextNode
        }

        if (map.isEmpty()) {
            throw SerializationException("Failed to serialize object: $state")
        }

        if (unchanged && previousMap != null && previousMap.entries.size == map.size) {
            return previousMap
        }
        return SnapshotMap(map)
    }

    private fun snapshotStateToSerializable(state: Map<String, SnapshotNode>): Map<String, Any> {
        val map = HashMap<String, Any>(state.size)
        state.forEach { (key, value) ->
            map[key] = snapshotNodeToSerializable(value)
        }
        return map
    }

    private fun serializeSnapshotSchedulable(schedulable: SnapshotSchedulable): Schedulable {
        val state = snapshotStateToSerializable(schedulable.state)
        return Schedulable(
            schedulable.name,
            schedulable.id,
            state,
            schedulable.parent,
            schedulable.type
        )
    }

    private fun snapshotNodeToSerializable(node: SnapshotNode): Any {
        return when (node) {
            is SnapshotValue -> Value(node.value, node.metadata)
            is SnapshotList -> node.items.map { snapshotNodeToSerializable(it) }
            is SnapshotMap -> node.entries.mapValues { (_, value) ->
                snapshotNodeToSerializable(value)
            }
        }
    }

    private fun serializeState(cmdState: Any): Map<String, Any>? {
        val map = HashMap<String, Any>()

        getStateProperties(cmdState::class).forEach { (prop, metadata, _, _) ->
            try {
                val value = prop.getter.call(cmdState)
                if (value != null && metadata != null) {
                    map[prop.name] = serializeVariable(value, metadata)
                }
            } catch (_: SerializationException) {
                // Skip properties that fail serialization
            }
        }

        return map.ifEmpty { null }
    }

    private fun serializeVariable(state: Any?, metadata: ValueMetadata): Any {
        return when (state) {
            is Number, is String, is Char, is Boolean -> Value(state, metadata)
            is Duration -> Value(state.inWholeMilliseconds, metadata)
            is Map<*, *> -> state.entries.associate { (k, v) ->
                k.toString() to serializeVariable(v, metadata.copy(readonly = true))
            }
            is Collection<*> -> state.map {
                serializeVariable(it!!, metadata.copy(readonly = false))
            }

            is Array<*> -> state.map {
                serializeVariable(it!!, metadata.copy(readonly = false))
            }

            null -> Value(null, metadata)
            else -> serializeObject(state, metadata)
        }
    }

    private fun serializeObject(state: Any, metadata: ValueMetadata): Any {
        val map = HashMap<String, Any?>()

        getObjectProperties(state::class).forEach { (prop, _, isEditable, priority) ->
            val value = prop.getter.call(state)

            // Re-calculate metadata for the child property
            val newMetadata = metadata.copy(
                readonly = metadata.readonly || !isEditable,
                priority = priority
            )

            map[prop.name] = serializeVariable(value, newMetadata)
        }

        if (map.isEmpty()) {
            throw SerializationException("Failed to serialize object: $state")
        }
        return map
    }
}
