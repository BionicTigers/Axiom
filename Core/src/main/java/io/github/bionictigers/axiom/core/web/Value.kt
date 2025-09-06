package io.github.bionictigers.axiom.core.web

data class ValueMetadata(
    val readOnly: Boolean = false,
    val priority: Int = 0,
    val hidden: Boolean? = null
)

data class Value(
    val value: Any?,
    val metadata: ValueMetadata
) {
    fun toJson(): Map<String, Any?> =
        with(metadata) {
            buildMap {
                put("value", value)
                put("readOnly", readOnly)
                put("priority", priority)
                putNotNull("hidden", hidden)
            }
        }
}

private fun MutableMap<String, Any?>.putNotNull(key: String, value: Any?) {
    if (value != null) put(key, value)
}