package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.System
import io.github.bionictigers.axiom.core.web.Display
import io.github.bionictigers.axiom.core.web.Editable
import io.github.bionictigers.axiom.core.web.Hidden
import io.github.bionictigers.axiom.core.web.Value
import io.github.bionictigers.axiom.core.web.serializable.ObjectType
import io.github.bionictigers.axiom.core.web.serializable.Schedulable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import java.util.concurrent.ConcurrentHashMap

class DeltaResolverTest {

    @BeforeEach
    fun setup() {
        // Clear snapshots between tests
        val field = DeltaResolver.javaClass.getDeclaredField("snapshots")
        field.isAccessible = true
        val snapshots = field.get(DeltaResolver) as MutableMap<*, *>
        snapshots.clear()
    }

    data class TestState(
        @Display(priority = 0) @Editable var counter: Int = 0,
        @Display(priority = 1) var message: String = "Hello",
        @Hidden var secret: String = "Secret",
        @Display(priority = 2) val fixed: Double = 3.14
    )

    data class NestedState(
        @Editable var inner: TestState = TestState()
    )

    // Helper to bypass internal constructor visibility if needed, 
    // or just assume it works because tests are in the same module.
    // If Command constructor is internal, we might need to use a subclass in the same package 
    // or rely on the fact that unit tests usually see internals.
    // For this test, let's try to mock or use reflection to create Commands if direct instantiation fails.
    // But since we are in `io.github.bionictigers.axiom.core.scheduler`, and Command is in `commands`,
    // we rely on Kotlin's "internal visible to tests" or just use a helper.
    
    // Actually, to make things easier and robust, let's rely on the public API or reflection.
    // However, for this specific test of DeltaResolver, we can just pass Mock objects or 
    // construct Schedulables directly to test `resolve`. 
    // `serialize` requires Commands.

    // Let's assume we can create a simplified stub of Command using reflection if needed,
    // or just try to instantiate it.

    @Test
    fun `test serialization of command state`() {
        val state = TestState()
        val command = createCommand("TestCmd", state)
        
        val map = mapOf(command.id to command)
        val serializedList = DeltaResolver.serialize(map, emptyMap())
        
        assertEquals(1, serializedList.size)
        val schedulable = serializedList[0]
        
        assertEquals("TestCmd", schedulable.name)
        assertEquals(ObjectType.Command, schedulable.type)
        
        val stateMap = schedulable.state
        assertTrue(stateMap.containsKey("counter"))
        assertTrue(stateMap.containsKey("message"))
        assertTrue(stateMap.containsKey("fixed"))
        assertFalse(stateMap.containsKey("secret")) // Hidden
        
        val counterVal = stateMap["counter"] as Value
        assertEquals(0, counterVal.value)
        assertFalse(counterVal.metadata!!.readOnly) // Editable defaults to true for vars unless marked otherwise? 
        // Actually code says: prop.findAnnotation<Editable>() == null -> readOnly = true?
        // Let's check logic: readOnly = metadata.readOnly || (prop.findAnnotation<Editable>() == null)
        // Wait, prop.findAnnotation<Editable>() == null means it IS readOnly (logic: readOnly = true if Editable missing?)
        // Let's re-read DeltaResolver logic.
        
        // Logic:
        // val metadata = ValueMetadata(
        //     prop.findAnnotation<Editable>() == null, // readOnly
        //     ...
        // )
        // So if Editable is missing, readOnly is true.
        // My TestState has @Editable on counter, so readOnly should be false.
    }

    @Test
    fun `test caching mechanism`() {
        val state = TestState()
        val command1 = createCommand("Cmd1", state)
        val command2 = createCommand("Cmd2", state)
        
        // Run serialization twice
        val map = mapOf(command1.id to command1, command2.id to command2)
        
        val run1 = DeltaResolver.serialize(map, emptyMap())
        val run2 = DeltaResolver.serialize(map, emptyMap())
        
        // If caching works, the second run should be identical and fast
        // We can't easily measure "fast" reliably in unit test, but we can ensure correctness.
        assertEquals(run1.size, run2.size)
        assertEquals(run1[0].state.keys, run2[0].state.keys)
    }

    @Test
    fun `test delta resolution - structure updates`() {
        val cmd1 = Schedulable("Cmd1", "id1", emptyMap(), null, ObjectType.Command)
        val cmd2 = Schedulable("Cmd2", "id2", emptyMap(), null, ObjectType.Command)
        
        // Initial: Add Cmd1
        val report1 = DeltaResolver.resolve(listOf(cmd1))
        assertEquals(1, report1.structureUpdates.size)
        assertEquals("id1", report1.structureUpdates[0].id)
        
        // Update: Add Cmd2, Keep Cmd1
        val report2 = DeltaResolver.resolve(listOf(cmd1, cmd2))
        assertEquals(1, report2.structureUpdates.size)
        assertEquals("id2", report2.structureUpdates[0].id)
        assertTrue(report2.removals.isEmpty())
        
        // Update: Remove Cmd1
        val report3 = DeltaResolver.resolve(listOf(cmd2))
        assertEquals(0, report3.structureUpdates.size) // Cmd2 already known
        assertEquals(1, report3.removals.size)
        assertTrue(report3.removals.contains("id1"))
    }

    @Test
    fun `test delta resolution - state updates`() {
        // Initial
        val state1 = mapOf("val" to 1)
        val cmd1 = Schedulable("Cmd1", "id1", state1, null, ObjectType.Command)
        
        DeltaResolver.resolve(listOf(cmd1)) // prime the snapshot
        
        // Change state
        val state2 = mapOf("val" to 2)
        val cmd1New = Schedulable("Cmd1", "id1", state2, null, ObjectType.Command)
        
        val report = DeltaResolver.resolve(listOf(cmd1New))
        
        assertEquals(1, report.stateUpdates.size)
        val update = report.stateUpdates[0]
        assertEquals("id1", update.first)
        assertEquals(2, update.second["val"])
    }

    @Test
    fun `test nested object serialization`() {
        val nested = NestedState()
        val cmd = createCommand("Nested", nested)
        
        val serialized = DeltaResolver.serialize(mapOf(cmd.id to cmd), emptyMap())
        val state = serialized[0].state
        
        assertTrue(state.containsKey("inner"))
        val innerMap = state["inner"] as Map<String, Any>
        
        // Inner object properties should be serialized
        // "inner" in NestedState has @Editable.
        // TestState properties: counter (@Editable), message (@Display), secret (@Hidden), fixed (val)
        
        assertTrue(innerMap.containsKey("counter"))
        assertTrue(innerMap.containsKey("message"))
        assertFalse(innerMap.containsKey("secret"))
        
        // Check metadata inheritance/calculation if relevant
        // Logic: newMetadata.readOnly = metadata.readOnly || !isEditable
        // Parent "inner" is @Editable, so readOnly=false.
        // Child "counter" is @Editable, so it should be editable.
        // Child "fixed" is not @Editable, so it should be read-only.
        
        val counterVal = innerMap["counter"] as Value
        assertFalse(counterVal.metadata!!.readOnly)
        
        val fixedVal = innerMap["fixed"] as Value
        assertTrue(fixedVal.metadata!!.readOnly)
    }
    
    // Helper to create Command via reflection since constructor is internal
    private fun <S> createCommand(name: String, state: S): Command<S> {
        val constructor = Command::class.constructors.find { it.parameters.size == 4 } 
            ?: throw RuntimeException("Could not find Command constructor")
        constructor.isAccessible = true
        return constructor.call(name, state, null, null) as Command<S>
    }
}

