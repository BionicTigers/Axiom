package io.github.bionictigers.axiom.core

import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.System as AxiomSystem
import io.github.bionictigers.axiom.core.scheduler.Scheduler
import io.github.bionictigers.axiom.core.web.Editable
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.serializable.Notification
import org.junit.jupiter.api.Test
import kotlin.math.sin

class InterfaceSimulationTest {

    data class SimulationState(
        var counter: Int = 0,
        var sineWave: Double = 0.0,
        @Editable
        var message: String = "Hello"
    )

    class SimulatedSystem : AxiomSystem() {
        override val name = "Simulated System"
        
        // System state properties
        var batteryVoltage: Double = 12.0
        @Editable
        var robotEnabled: Boolean = true
    }

    @Test
    fun runSimulation() {
        println("Starting Interface Simulation...")
        Server.start()

        val system = SimulatedSystem()
        
        val state = SimulationState()
        val command = Command("Simulation Command", state)
            .action { s, _ ->
                s.counter++
                s.sineWave = sin(s.counter * 0.1)
                
                // Update system properties too
                system.batteryVoltage = 12.0 + sin(s.counter * 0.05)
            }
            .dependsOn(system)

        Scheduler.schedule(command)
        Scheduler.schedule(system)

        println("Simulation running. Connect the Interface to localhost:10464")
        
        while (true) {
            val start = java.lang.System.currentTimeMillis()
            Scheduler.tick()
            val end = java.lang.System.currentTimeMillis()
            
            val sleep = 50 - (end - start)
            if (sleep > 0) {
                Thread.sleep(sleep)
            }
        }
    }
}

