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
    val parent: String?, // Hash of parent system if command
    val type: ObjectType
)

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