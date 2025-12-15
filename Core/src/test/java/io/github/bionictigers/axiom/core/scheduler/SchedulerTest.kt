package io.github.bionictigers.axiom.core.scheduler

import io.github.bionictigers.axiom.core.commands.Command
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.isAccessible
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KMutableProperty

class SchedulerTest {

    @BeforeEach
    fun setup() {
        // Reset SchedulerState
        val commandsField = SchedulerState::class.members.find { it.name == "commands" }
        commandsField?.isAccessible = true
        (commandsField?.call(SchedulerState) as? ConcurrentHashMap<*, *>)?.clear()
        
        val addQueueField = SchedulerState::class.members.find { it.name == "addQueue" }
        addQueueField?.isAccessible = true
        (addQueueField?.call(SchedulerState) as? ConcurrentLinkedQueue<*>)?.clear()
        
        val removeQueueField = SchedulerState::class.members.find { it.name == "removeQueue" }
        removeQueueField?.isAccessible = true
        (removeQueueField?.call(SchedulerState) as? ConcurrentLinkedQueue<*>)?.clear()
        
        // Reset Systems
        val systemsField = SchedulerState::class.members.find { it.name == "systems" }
        systemsField?.isAccessible = true
        (systemsField?.call(SchedulerState) as? ConcurrentHashMap<*, *>)?.clear()
        
        // Reset inUpdateCycle
        val inUpdateCycleField = SchedulerState::class.members.find { it.name == "inUpdateCycle" }
        // inUpdateCycle is a var property.
        if (inUpdateCycleField is kotlin.reflect.KMutableProperty<*>) {
             inUpdateCycleField.isAccessible = true
             inUpdateCycleField.setter.call(SchedulerState, false)
        }
        
        // Reset DeltaResolver snapshots to avoid interference
        val snapshotsField = DeltaResolver.javaClass.getDeclaredField("snapshots")
        snapshotsField.isAccessible = true
        (snapshotsField.get(DeltaResolver) as MutableMap<*, *>).clear()
    }

    // Helper to create Command via reflection
    private fun <S> createCommand(name: String, state: S): Command<S> {
        val constructor = Command::class.constructors.find { it.parameters.size == 4 } 
            ?: throw RuntimeException("Could not find Command constructor")
        constructor.isAccessible = true
        return constructor.call(name, state, null, null) as Command<S>
    }

    @Test
    fun `test scheduling a command`() {
        var executed = false
        val cmd = createCommand("TestCmd", null)
            .action { executed = true }
        
        Scheduler.schedule(cmd)
        
        // Command is added to addQueue (if in cycle) or map directly?
        // Scheduler.schedule logic: if inUpdateCycle -> addQueue, else -> add() directly.
        // Initially inUpdateCycle should be false.
        
        // Verify it's in the commands map (via reflection or by running tick)
        // Since we are not in update cycle, it should be in commands map immediately.
        
        val commandsMap = SchedulerState.commands
        assertTrue(commandsMap.containsKey(cmd.id))
        assertTrue(cmd.running) // Added command should be running
        
        // Run tick
        Scheduler.tick()
        
        assertTrue(executed)
    }

    @Test
    fun `test unscheduling a command`() {
        val cmd = createCommand("TestCmd", null)
        Scheduler.schedule(cmd)
        
        assertTrue(SchedulerState.commands.containsKey(cmd.id))
        
        Scheduler.unschedule(cmd)
        // Should be removed immediately since not in update cycle
        assertFalse(SchedulerState.commands.containsKey(cmd.id))
        assertFalse(cmd.running)
    }

    @Test
    fun `test command removal during tick`() {
        // If a command stops itself, it calls Scheduler.unschedule(this)
        // This happens inside tick(), so inUpdateCycle = true.
        // It should go to removeQueue.
        
        var executedCount = 0
        val cmd = createCommand("SelfStopCmd", null)
            .action { 
                executedCount++
                stop() 
            }
            
        Scheduler.schedule(cmd)
        
        Scheduler.tick()
        
        assertEquals(1, executedCount)
        assertFalse(SchedulerState.commands.containsKey(cmd.id))
        assertFalse(cmd.running)
    }
}

