package io.github.bionictigers.axiom.core.web.serializable

import io.github.bionictigers.axiom.core.web.Serializable

enum class ObjectType {
    Command,
    System
}

data class Schedulable(
    val name: String,
    val id: String,
    val state: Map<String, Any>,
    val parent: String?, // Id of parent system if type is command
    val type: ObjectType
)

internal data class SchedulerOrder(
    val order: List<String>
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "schedulable_order",
            "tick" to tick,
            "data" to order
        )
    }
}

internal data class SchedulerDetails(
    val tick: Long,
    val executionTime: Double,
    val currentTime: Double
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "scheduler_details",
            "tick" to tick,
            "data" to mapOf(
                "tick" to this.tick,
                "executionTime" to executionTime,
                "currentTime" to currentTime
            )
        )
    }
}

internal data class SchedulablesInitial(
    val objects: List<Schedulable>,
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "schedulable_initial",
            "tick" to tick,
            "data" to objects.map { obj ->
                mapOf(
                    "name" to obj.name,
                    "id" to obj.id,
                    "state" to obj.state,
                    "parent" to obj.parent,
                    "type" to obj.type.name,
                )
            }
        )
    }
}

internal data class SchedulablesUpdate(
    val objects: List<Schedulable>,
    val removed: Set<String>,
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "schedulable_update",
            "tick" to tick,
            "data" to mapOf(
                "updated" to objects.associate { obj ->
                    obj.id to mapOf(
                        "name" to obj.name,
                        "parent" to obj.parent,
                        "type" to obj.type.name,
                        // state is sent in StateUpdate
                    )
                },
                "removed" to removed
            )
        )
    }
}

internal data class StateUpdate(
    val deltas: List<Pair<String, Map<String, Any?>>>,
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "schedulable_state_update",
            "tick" to tick,
            "data" to deltas.flatMap { (id, obj) ->
                obj.map { (field, value) ->
                    mapOf(
                        "id" to id,
                        "field" to field,
                        "value" to value
                    )
                }
            }
        )
    }
}

/**
 * Detailed scheduler debug metrics with rolling averages.
 * All times are in milliseconds.
 */
internal data class SchedulerDebug(
    // Current tick values
    val currentTotal: Double,
    val currentCommands: Double,
    val currentAxiomOverhead: Double,
    val currentQueueProcessing: Double,
    val currentDependencySort: Double,
    val currentSerialization: Double,
    val currentSerializedStates: Double,
    val currentSerializedFields: Double,
    val currentDeltaResolution: Double,
    val currentNetworkSend: Double,
    val currentConnectionCallbacks: Double,
    // Rolling averages
    val avgTotal: Double,
    val avgCommands: Double,
    val avgAxiomOverhead: Double,
    val avgQueueProcessing: Double,
    val avgDependencySort: Double,
    val avgSerialization: Double,
    val avgSerializedStates: Double,
    val avgSerializedFields: Double,
    val avgDeltaResolution: Double,
    val avgNetworkSend: Double,
    val avgConnectionCallbacks: Double,
    // Command count
    val commandCount: Int
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "scheduler_debug",
            "tick" to tick,
            "data" to mapOf(
                "current" to mapOf(
                    "total" to currentTotal,
                    "commands" to currentCommands,
                    "axiomOverhead" to currentAxiomOverhead,
                    "queueProcessing" to currentQueueProcessing,
                    "dependencySort" to currentDependencySort,
                    "serialization" to currentSerialization,
                    "serializedStates" to currentSerializedStates,
                    "serializedFields" to currentSerializedFields,
                    "deltaResolution" to currentDeltaResolution,
                    "networkSend" to currentNetworkSend,
                    "connectionCallbacks" to currentConnectionCallbacks
                ),
                "average" to mapOf(
                    "total" to avgTotal,
                    "commands" to avgCommands,
                    "axiomOverhead" to avgAxiomOverhead,
                    "queueProcessing" to avgQueueProcessing,
                    "dependencySort" to avgDependencySort,
                    "serialization" to avgSerialization,
                    "serializedStates" to avgSerializedStates,
                    "serializedFields" to avgSerializedFields,
                    "deltaResolution" to avgDeltaResolution,
                    "networkSend" to avgNetworkSend,
                    "connectionCallbacks" to avgConnectionCallbacks
                ),
                "commandCount" to commandCount
            )
        )
    }
}