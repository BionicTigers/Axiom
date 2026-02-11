//package io.github.bionictigers.axiom.core
//
//import io.github.bionictigers.axiom.core.commands.Command
//import io.github.bionictigers.axiom.core.commands.System as AxiomSystem
//import io.github.bionictigers.axiom.core.commands.bt.behaviorTree
//import io.github.bionictigers.axiom.core.commands.bt.traced
//import io.github.bionictigers.axiom.core.scheduler.Scheduler
//import io.github.bionictigers.axiom.core.web.Editable
//import io.github.bionictigers.axiom.core.web.Server
//import io.github.bionictigers.axiom.core.web.serializable.Notification
//import org.junit.jupiter.api.Test
//import kotlin.math.sin
//
//class InterfaceSimulationTest {
//
//    data class SimulationState(
//        var counter: Int = 0,
//        var sineWave: Double = 0.0,
//        @Editable
//        var message: String = "Hello"
//    )
//
//    class SimulatedSystem : AxiomSystem() {
//        override val name = "Simulated System"
//
//        // System state properties
//        var batteryVoltage: Double = 12.0
//        @Editable
//        var robotEnabled: Boolean = true
//    }
//
//    @Test
//    fun runSimulation() {
//        println("Starting Interface Simulation...")
//        Server.start()
//
//        val system = SimulatedSystem()
//
//        val state = SimulationState()
//        val command = Command("Simulation Command", state)
//            .action { s, _ ->
//                s.counter++
//                s.sineWave = sin(s.counter * 0.1)
//
//                // Update system properties too
//                system.batteryVoltage = 12.0 + sin(s.counter * 0.05)
//            }
//            .dependsOn(system)
//
//        Scheduler.schedule(command)
//        Scheduler.schedule(system)
//
//        println("Simulation running. Connect the Interface to localhost:10464")
//
//        while (true) {
//            val start = java.lang.System.currentTimeMillis()
//            Scheduler.tick()
//            val end = java.lang.System.currentTimeMillis()
//
//            val sleep = 50 - (end - start)
//            if (sleep > 0) {
//                Thread.sleep(sleep)
//            }
//        }
//    }
//
//    @Test
//    fun runBehaviorTreeSimulation() {
//        println("Starting Behavior Tree Simulation...")
//        Server.start()
//
//        var tickCounter = 0
//        var phase = 0 // 0 = intake, 1 = score, 2 = idle
//
//        val tree = behaviorTree("RobotBehaviorTree") {
//            selector("Root") {
//                // Score sequence: only runs when we have a piece
//                sequence("ScoreSequence") {
//                    condition("HasPiece") { blackboard ->
//                        blackboard?.get<Boolean>("hasPiece") == true
//                    }
//                    action("NavigateToGoal") {
//                        val target = blackboard?.get<Int>("goalPosition") ?: 0
//                        blackboard?.set("currentTarget", target)
//                        println("Navigating to goal $target")
//                        succeed()
//                    }
//                    action("PlacePiece") {
//                        println("Placing piece at goal")
//                        blackboard?.set("hasPiece", false)
//                        blackboard?.set("piecesScored", (blackboard?.get<Int>("piecesScored") ?: 0) + 1)
//                        succeed()
//                    }
//                }
//
//                // Intake sequence: runs when we don't have a piece
//                sequence("IntakeSequence") {
//                    inverter {
//                        condition("HasPiece") { blackboard ->
//                            blackboard?.get<Boolean>("hasPiece") == true
//                        }
//                    }
//                    action("SeekPiece") {
//                        val searchArea = blackboard?.get<Int>("searchArea") ?: 0
//                        println("Seeking piece in area $searchArea")
//                        // Simulate finding a piece sometimes
//                        if (tickCounter % 20 == 0) {
//                            blackboard?.set("hasPiece", true)
//                            succeed()
//                        }
//                    }
//                    action("IntakePiece") {
//                        println("Intaking piece")
//                        blackboard?.set("hasPiece", true)
//                        succeed()
//                    }
//                }
//
//                // Idle fallback
//                action("Idle") {
//                    println("Idle...")
//                    succeed()
//                }
//            }
//        }
//
//        // Initialize blackboard
//        tree.blackboard["hasPiece"] = false
//        tree.blackboard["goalPosition"] = 1
//        tree.blackboard["searchArea"] = 0
//        tree.blackboard["piecesScored"] = 0
//
//        // Schedule the traced tree
//        val tracedTree = tree.traced(traceEveryNTicks = 1)
//        Scheduler.schedule(tracedTree)
//
//        println("Behavior Tree simulation running. Connect Seek to localhost:10464")
//        println("Open the 'Behavior Tree' window in Seek to see the tree visualization")
//        println("Blackboard will show: hasPiece, goalPosition, searchArea, piecesScored")
//
//        while (true) {
//            tickCounter++
//
//            // Simulate some state changes
//            if (tickCounter % 50 == 0) {
//                // Every 50 ticks, toggle between intake and score phases
//                phase = (phase + 1) % 3
//                when (phase) {
//                    0 -> {
//                        tree.blackboard["hasPiece"] = false
//                        println("Phase: Intake")
//                    }
//                    1 -> {
//                        tree.blackboard["hasPiece"] = true
//                        println("Phase: Score")
//                    }
//                    2 -> {
//                        println("Phase: Idle")
//                    }
//                }
//            }
//
//            val start = java.lang.System.currentTimeMillis()
//            Scheduler.tick()
//            val end = java.lang.System.currentTimeMillis()
//
//            val sleep = 50 - (end - start)
//            if (sleep > 0) {
//                Thread.sleep(sleep)
//            }
//
//            // Restart tree if it completes (for continuous operation)
//            if (!tracedTree.running) {
//                println("Tree completed, restarting...")
//                tree.root.reset()
//                Scheduler.schedule(tracedTree)
//            }
//        }
//    }
//}
//
