package io.github.bionictigers.axiom.core.commands.bt

/**
 * Shared data store for behavior tree nodes.
 *
 * The blackboard provides a type-safe way for BT nodes to share data.
 * It serializes to a Map for streaming to Seek.
 *
 * Usage:
 * ```kotlin
 * // Write
 * blackboard["targetPose"] = Pose2d(10.0, 20.0, 0.0)
 * blackboard["hasGamePiece"] = true
 *
 * // Read
 * val pose: Pose2d? = blackboard["targetPose"]
 * val hasPiece: Boolean = blackboard["hasGamePiece"] ?: false
 * ```
 */
class Blackboard {
    private val _data = mutableMapOf<String, Any?>()

    /**
     * Exposed for serialization. DeltaResolver will serialize this Map.
     */
    val data: Map<String, Any?> get() = _data.toMap()

    /**
     * Get a value from the blackboard with type inference.
     *
     * @param key The key to look up
     * @return The value cast to type T, or null if not found or wrong type
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? = _data[key] as? T

    /**
     * Set a value in the blackboard.
     *
     * @param key The key to store under
     * @param value The value to store (should be serializable: primitives, collections, or @Display objects)
     */
    operator fun set(key: String, value: Any?) {
        _data[key] = value
    }

    /**
     * Remove a value from the blackboard.
     *
     * @param key The key to remove
     * @return The removed value, or null if not found
     */
    fun remove(key: String): Any? = _data.remove(key)

    /**
     * Clear all values from the blackboard.
     */
    fun clear() = _data.clear()

    /**
     * Check if the blackboard contains a key.
     *
     * @param key The key to check
     * @return true if the key exists
     */
    fun contains(key: String): Boolean = key in _data

    /**
     * Get all keys in the blackboard.
     */
    val keys: Set<String> get() = _data.keys.toSet()

    /**
     * Get the number of entries in the blackboard.
     */
    val size: Int get() = _data.size

    /**
     * Check if the blackboard is empty.
     */
    fun isEmpty(): Boolean = _data.isEmpty()

    /**
     * Get a value with a default if not found.
     *
     * @param key The key to look up
     * @param default The default value if key not found
     * @return The value or default
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrDefault(key: String, default: T): T = _data[key] as? T ?: default

    /**
     * Get a value or compute and store it if not found.
     *
     * @param key The key to look up
     * @param compute Function to compute the value if not found
     * @return The existing or computed value
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrPut(key: String, compute: () -> T): T {
        val existing = _data[key]
        if (existing != null) return existing as T
        val computed = compute()
        _data[key] = computed
        return computed
    }
}
