package io.github.bionictigers.axiom.core.web.serializable

import io.github.bionictigers.axiom.core.web.Serializable
import io.github.bionictigers.axiom.core.web.Server

data class Notification(
    val title: String,
    val message: String,
    val type: Type,
    val isModal: Boolean = false
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        check(title.length < 26) { "Notification title too long (max 25 characters)" }
        check(message.length < 201) { "Notification message too long (max 200 characters)" }

        return mapOf(
            "name" to "notification",
            "tick" to tick,
            "data" to mapOf(
                "title" to title,
                "message" to message,
                "type" to type.name,
                "isModal" to isModal
            )
        )
    }

    enum class Type {
        INFO,
        WARNING,
        ERROR
    }

    companion object {
        fun send(title: String, message: String, type: Type, isModal: Boolean = false) {
            Server.send(Notification(title, message, type, isModal))
        }
    }
}