package io.github.bionictigers.axiom.core.scheduler

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.Schedulable
import io.github.bionictigers.axiom.core.utils.findPropertyInHierarchy
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.serializable.Notification
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal object PropertyEditor {
    /**
     * Edit a property at the specified path with the given value.
     *
     * @param path The path to the property to edit.
     * @param value The new value to set.
     */
    fun edit(path: String, value: String) {
        val (obj, leaf) = navigatePath(path)

        applyValue(obj, leaf, value)
    }

    private fun navigatePath(path: String): Pair<Any, String> {
        val segments = path.split(".")
        check(segments.size >= 3) { "Invalid path format: $path" }

        val (schedulable, isCommand) = resolveRoot(segments[0], segments[1])
        val leafPair = resolveLeaf(if (isCommand) (schedulable as Command<*>).state!! else schedulable, segments.drop(2))
        return leafPair
    }

    private fun resolveRoot(type: String, id: String): Pair<Schedulable, Boolean> {
        check(type == "command" || type == "system") {
            "Invalid schedulable type: $type"
        }

        val store = if (type == "command") SchedulerState.commands else SchedulerState.systems
        val schedulable = store[id] ?: run {
            Server.send(Notification(
                "Edit Failure",
                "No schedulable found with id $id in ${type}s\nPlease resynchronize.",
                Notification.Type.WARNING
            ))
            RobotLog.ww("Axiom", "PropertyEditor: No schedulable found with id $id")
        }

        return Pair(schedulable as Schedulable, type == "command")
    }

    private fun resolveLeaf(root: Any, segments: List<String>): Pair<Any, String> {
        var obj: Any = root
        segments.dropLast(1).forEach { segment ->
            obj = getNextObject(obj, segment)
                ?: throw IllegalStateException("Path segment '$segment' resulted in null")
        }

        return Pair(obj, segments.last())
    }

    private fun getNextObject(current: Any, segment: String): Any? {
        val index = segment.toIntOrNull()
        if (index != null) {
            return when (current) {
                is List<*> -> current[index]
                is Array<*> -> current[index]
                is IntArray -> current[index]
                is DoubleArray -> current[index]
                is LongArray -> current[index]
                is FloatArray -> current[index]
                is BooleanArray -> current[index]
                else -> throw IllegalStateException("Cannot index into ${current::class.simpleName}")
            }
        }

        // Handle Property Reflection
        val prop = current::class.memberProperties
            .find { it.name == segment } as? KMutableProperty1<*, *>
            ?: return null

        prop.isAccessible = true
        return prop.getter.call(current)
    }

    private fun applyValue(parent: Any, leaf: String, rawValue: String) {
        val index = leaf.toIntOrNull()

        // CASE 1: Array/List Indexing
        if (index != null) {
            setInArrayOrList(parent, index, rawValue)
            return
        }

        // CASE 2: Object Property (Using Kotlin Reflection)
        val prop = parent::class.memberProperties
            .find { it.name == leaf } as? KMutableProperty1<*, *>
            ?: throw NoSuchFieldException("Property '$leaf' not found on ${parent::class.simpleName}")

        // Determine type for conversion
        val targetType = prop.returnType.classifier as? KClass<*> ?: String::class
        val convertedValue = rawValue.convertTo(targetType)

        prop.isAccessible = true
        prop.setter.call(parent, convertedValue)
    }

    // Helper to handle the specific nature of Kotlin Arrays
    private fun setInArrayOrList(container: Any, index: Int, rawValue: String) {
        when (container) {
            is MutableList<*> -> {
                @Suppress("UNCHECKED_CAST")
                val list = container as MutableList<Any?>
                // Attempt to guess type from existing element, fallback to String
                val targetType = list.firstOrNull()?.let { it::class } ?: String::class
                list[index] = rawValue.convertTo(targetType)
            }
            // Primitive arrays must be handled explicitly in Kotlin
            is IntArray -> container[index] = rawValue.toInt()
            is LongArray -> container[index] = rawValue.toLong()
            is FloatArray -> container[index] = rawValue.toFloat()
            is DoubleArray -> container[index] = rawValue.toDouble()
            is BooleanArray -> container[index] = rawValue.toBooleanStrict()
            is ShortArray -> container[index] = rawValue.toShort()
            is ByteArray -> container[index] = rawValue.toByte()
            is CharArray -> container[index] = rawValue.single()
            is Array<*> -> {
                // Object Arrays
                @Suppress("UNCHECKED_CAST")
                val array = container as Array<Any?>
                val componentType = container::class.componentType() ?: String::class
                array[index] = rawValue.convertTo(componentType)
            }
            else -> throw IllegalArgumentException("Cannot set index on type: ${container::class.simpleName}")
        }
    }

    // Extension to get component type of an Array<T> using Kotlin Reflection
    private fun KClass<*>.componentType(): KClass<*>? {
        // There isn't a direct "componentType" in KClass, but we can inspect constructors or just default to null
        // A safe heuristic is checking the first non-null element if possible, or usually passed via context.
        // However, for pure safety, we can return null and let 'convertTo' default to String or throw.
        return null
    }

    fun String.convertTo(kClass: KClass<*>?): Any {
        return when (kClass) {
            Int::class -> this.toInt()
            Long::class -> this.toLong()
            Double::class -> this.toDouble()
            Float::class -> this.toFloat()
            Boolean::class -> this.toBooleanStrict()
            Byte::class -> this.toByte()
            Short::class -> this.toShort()
            Char::class -> this.single()
            else -> this // Default to String
        }
    }
}