package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.bt.Blackboard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

class MapSerializationTest {

    @Test
    fun `deltaResolver serializes simple map`() {
        val map = mapOf("key1" to "value1", "key2" to 42, "key3" to true)
        
        val result = serializeViaReflection(map)
        
        assertNotNull(result)
        assertTrue(result is Map<*, *>)
        val resultMap = result as Map<*, *>
        
        // Each value should be wrapped in a Value object
        assertTrue(resultMap.containsKey("key1"))
        assertTrue(resultMap.containsKey("key2"))
        assertTrue(resultMap.containsKey("key3"))
    }

    @Test
    fun `deltaResolver serializes nested map`() {
        val map = mapOf(
            "outer" to mapOf(
                "inner" to "value"
            )
        )
        
        val result = serializeViaReflection(map)
        
        assertNotNull(result)
        assertTrue(result is Map<*, *>)
        val resultMap = result as Map<*, *>
        
        assertTrue(resultMap.containsKey("outer"))
        val outerValue = resultMap["outer"]
        assertTrue(outerValue is Map<*, *>)
    }

    @Test
    fun `deltaResolver serializes map with list values`() {
        val map = mapOf(
            "numbers" to listOf(1, 2, 3),
            "strings" to listOf("a", "b", "c")
        )
        
        val result = serializeViaReflection(map)
        
        assertNotNull(result)
        assertTrue(result is Map<*, *>)
        val resultMap = result as Map<*, *>
        
        assertTrue(resultMap.containsKey("numbers"))
        assertTrue(resultMap.containsKey("strings"))
        
        val numbers = resultMap["numbers"]
        assertTrue(numbers is List<*>)
    }

    @Test
    fun `deltaResolver handles empty map`() {
        val map = emptyMap<String, Any>()
        
        val result = serializeViaReflection(map)
        
        assertNotNull(result)
        assertTrue(result is Map<*, *>)
        assertEquals(0, (result as Map<*, *>).size)
    }

    @Test
    fun `blackboard data serializes correctly`() {
        val blackboard = Blackboard()
        blackboard["string"] = "hello"
        blackboard["number"] = 123
        blackboard["boolean"] = true
        blackboard["list"] = listOf(1, 2, 3)
        
        val data = blackboard.data
        val result = serializeViaReflection(data)
        
        assertNotNull(result)
        assertTrue(result is Map<*, *>)
        val resultMap = result as Map<*, *>
        
        assertEquals(4, resultMap.size)
        assertTrue(resultMap.containsKey("string"))
        assertTrue(resultMap.containsKey("number"))
        assertTrue(resultMap.containsKey("boolean"))
        assertTrue(resultMap.containsKey("list"))
    }

    // Helper to call DeltaResolver.serializeVariable via reflection
    private fun serializeViaReflection(value: Any?): Any? {
        val deltaResolver = DeltaResolver
        val method = DeltaResolver::class.declaredMemberFunctions
            .find { it.name == "serializeVariable" }
            ?: throw RuntimeException("Could not find serializeVariable method")
        
        method.isAccessible = true
        
        // Create a ValueMetadata for the call
        val valueMetadataClass = Class.forName("io.github.bionictigers.axiom.core.web.ValueMetadata")
        val metadataConstructor = valueMetadataClass.constructors.first()
        val metadata = metadataConstructor.newInstance(true, 0, false)
        
        return method.call(deltaResolver, value, metadata)
    }
}
