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
        val stateUpdates: List<Pair<String, Map<String, Any?>>>
    )

    fun resolve(current: List<Schedulable>): DeltaReport {
        val structureUpdates = ArrayList<Schedulable>()
        val stateUpdates = ArrayList<Pair<String, Map<String, Any?>>>()

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

        val currentIds = current.map { it.id }.toSet()
        val removals = snapshots.keys.subtract(currentIds)
        removals.forEach { snapshots.remove(it) }

        current.forEach { schedulable ->
            val id = schedulable.id
            val previous = snapshots[id]

            if (previous == null) {
                // New, full state
                if (schedulable.state.isNotEmpty()) {
                    stateUpdates.add(id to schedulable.state)
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
                }
            }
            // Update snapshot
            snapshots[id] = schedulable
        }

        return DeltaReport(structureUpdates, removals, stateUpdates)
    }

    fun serialize(
        commands: Map<String, Command<*>>,
        systems: Map<String, System>
    ): ArrayList<Schedulable> {
        val result = ArrayList<Schedulable>(commands.size + systems.size)

        result.addAll(commands.values.map { cmd ->
            Schedulable(
                cmd.name,
                cmd.id,
                cmd.state?.let { serializeState(it) } ?: emptyMap(),
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
