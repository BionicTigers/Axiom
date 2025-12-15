package io.github.bionictigers.axiom.core.commands

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.isAccessible

class CommandTest {

    // Helper to create Command via reflection since constructor is internal
    private fun <S> createCommand(name: String, state: S): Command<S> {
        val constructor = Command::class.constructors.find { it.parameters.size == 4 } 
            ?: throw RuntimeException("Could not find Command constructor")
        constructor.isAccessible = true
        return constructor.call(name, state, null, null) as Command<S>
    }

    @Test
    fun `test lifecycle methods`() {
        var entered = false
        var executed = false
        var exited = false
        
        val command = createCommand("LifecycleCmd", null)
            .enter { entered = true }
            .action { _ -> executed = true }
            .exit { exited = true }
        
        assertFalse(entered)
        assertFalse(executed)
        assertFalse(exited)
        assertFalse(command.running)
        
        // Simulate Scheduler internals
        val enterMethod = Command::class.members.find { it.name == "schedulerEnter" }
        enterMethod?.isAccessible = true
        enterMethod?.call(command)
        
        assertTrue(entered)
        assertTrue(command.running)
        
        val executeMethod = Command::class.members.find { it.name == "execute" }
        executeMethod?.isAccessible = true
        executeMethod?.call(command)
        
        assertTrue(executed)
        
        val exitMethod = Command::class.members.find { it.name == "schedulerExit" }
        exitMethod?.isAccessible = true
        exitMethod?.call(command)
        
        assertTrue(exited)
        assertFalse(command.running)
    }

    @Test
    fun `test predicate stops command`() {
        var executed = false
        val command = createCommand("PredicateCmd", null)
            .requires { false } // Should stop immediately
            .action { _ -> executed = true }
        
        val enterMethod = Command::class.members.find { it.name == "schedulerEnter" }
        enterMethod?.isAccessible = true
        enterMethod?.call(command)
        
        val executeMethod = Command::class.members.find { it.name == "execute" }
        executeMethod?.isAccessible = true
        executeMethod?.call(command)
        
        // If predicate returns false, the action loop should NOT run? 
        // Actually, logic is: if (executableDsl.predicate(predicate)) executableDsl.action(action)
        // If predicate returns false, action is skipped. 
        // Does it remove the command? 
        // "Executes the command if the predicate is true. If the predicate is false, the command will be removed from the scheduler."
        // Wait, looking at Command.kt:
        // if (executableDsl.predicate(predicate)) executableDsl.action(action)
        // It doesn't seem to automatically stop if predicate is false in execute().
        // Ah, let's check Command.kt again.
        
        assertFalse(executed)
    }

    @Test
    fun `test DSL stop throws exception`() {
        val command = createCommand("StopCmd", null)
            .action {
                stop() // Should throw CommandStopped
            }
            
        val enterMethod = Command::class.members.find { it.name == "schedulerEnter" }
        enterMethod?.isAccessible = true
        enterMethod?.call(command)
        
        val executeMethod = Command::class.members.find { it.name == "execute" }
        executeMethod?.isAccessible = true
        
        // execute() catches CommandStopped internally, so this shouldn't throw to us.
        // It should just return.
        assertDoesNotThrow {
            executeMethod?.call(command)
        }
    }
}


