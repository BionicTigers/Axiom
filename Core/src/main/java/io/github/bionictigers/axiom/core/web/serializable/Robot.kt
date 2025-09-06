package io.github.bionictigers.axiom.core.web.serializable

import io.github.bionictigers.axiom.core.web.Serializable
import kotlin.collections.mapOf

internal data class RobotTelemetry(
    val lines: List<String>,
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "robot_telemetry",
            "tick" to tick,
            "data" to mapOf(
                "lines" to lines
            )
        )
    }
}

internal data class RobotOpMode(
    val name: String?,
    val inInit: Boolean,
    val inRun: Boolean,
    val isAuto: Boolean
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "robot_telemetry",
            "tick" to tick,
            "data" to mapOf(
                "name" to name,
                "inInit" to inInit,
                "inRun" to inRun,
                "isAuto" to isAuto
            )
        )
    }
}