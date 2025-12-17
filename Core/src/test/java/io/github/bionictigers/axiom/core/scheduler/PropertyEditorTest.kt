package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.web.Editable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.isAccessible
import java.util.concurrent.ConcurrentHashMap

class PropertyEditorTest {

    data class EditState(
        @Editable var number: Int = 10,
        @Editable var text: String = "Start",
        var readOnly: Int = 5,
        @Editable var list: MutableList<Int> = mutableListOf(1, 2, 3)
    )

    private lateinit var command: Command<EditState>

    // Helper to create Command via reflection
    private fun <S> createCommand(name: String, state: S): Command<S> {
        val constructor = Command::class.constructors.find { it.parameters.size == 4 } 
            ?: throw RuntimeException("Could not find Command constructor")
        constructor.isAccessible = true
        return constructor.call(name, state, null, null) as Command<S>
    }

    @BeforeEach
    fun setup() {
        // Clear SchedulerState
        val commandsField = SchedulerState::class.members.find { it.name == "commands" }
        commandsField?.isAccessible = true
        (commandsField?.call(SchedulerState) as? ConcurrentHashMap<*, *>)?.clear()
        
        // Add a test command
        val state = EditState()
        command = createCommand("EditCmd", state)
        
        // Add to SchedulerState
        SchedulerState.add(command)
    }

    @Test
    fun `test valid property edit`() {
        // Path format: "command.{id}.{property}"
        val path = "command.${command.id}.number"
        PropertyEditor.edit(path, "42")
        
        assertEquals(42, command.state?.number)
    }

    @Test
    fun `test valid list index edit`() {
        val path = "command.${command.id}.list.1"
        PropertyEditor.edit(path, "99")
        
        assertEquals(99, command.state?.list?.get(1))
    }

    @Test
    fun `test invalid path crash safety`() {
        // This should log an error but NOT crash the test (because we fixed it in Scheduler, wait...)
        // PropertyEditor.edit() throws exceptions. 
        // Scheduler.tick() catches them.
        // If I call PropertyEditor.edit() directly here, it WILL throw.
        // So I should assertThrows or call via Scheduler.edit() (which adds to queue) and then tick().
        
        val invalidPath = "command.${command.id}.nonexistent"
        
        assertThrows(NoSuchFieldException::class.java) {
            PropertyEditor.edit(invalidPath, "123")
        }
    }
    
    @Test
    fun `test scheduler handles invalid edit gracefully`() {
        // Scheduler.edit adds to queue if in cycle, or calls directly if not.
        // To test the try-catch block in processEditQueue, we need to be in update cycle or force queue processing.
        
        // Let's manually add to editQueue and call processEditQueue
        val editQueueField = SchedulerState::class.members.find { it.name == "editQueue" }
        editQueueField?.isAccessible = true
        val queue = editQueueField?.call(SchedulerState) as java.util.Queue<Pair<String, String>>
        
        queue.add("command.${command.id}.nonexistent" to "123")
        
        // Run processEditQueue
        // This should NOT throw
        assertDoesNotThrow {
             SchedulerState.processEditQueue()
        }
    }
}





