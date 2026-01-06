package io.github.bionictigers.axiom.core.commands.bt

import io.github.bionictigers.axiom.core.commands.bt.composites.Parallel
import io.github.bionictigers.axiom.core.commands.bt.composites.ParallelPolicy
import io.github.bionictigers.axiom.core.commands.bt.composites.Selector
import io.github.bionictigers.axiom.core.commands.bt.composites.Sequence
import io.github.bionictigers.axiom.core.commands.bt.decorators.Inverter
import io.github.bionictigers.axiom.core.commands.bt.decorators.Repeater
import io.github.bionictigers.axiom.core.commands.bt.decorators.Succeeder
import io.github.bionictigers.axiom.core.commands.bt.decorators.UntilFail
import io.github.bionictigers.axiom.core.commands.bt.leaves.BtAction
import io.github.bionictigers.axiom.core.commands.bt.leaves.Condition
import io.github.bionictigers.axiom.core.commands.bt.leaves.Wait
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class BehaviorTreeTest {

    // ===== BLACKBOARD TESTS =====

    @Test
    fun `blackboard stores and retrieves values`() {
        val blackboard = Blackboard()
        
        blackboard["intValue"] = 42
        blackboard["stringValue"] = "hello"
        blackboard["boolValue"] = true
        blackboard["nullValue"] = null
        
        assertEquals(42, blackboard.get<Int>("intValue"))
        assertEquals("hello", blackboard.get<String>("stringValue"))
        assertEquals(true, blackboard.get<Boolean>("boolValue"))
        assertNull(blackboard.get<Any>("nullValue"))
        assertNull(blackboard.get<Any>("nonexistent"))
    }

    @Test
    fun `blackboard data property returns copy`() {
        val blackboard = Blackboard()
        blackboard["key"] = "value"
        
        val data = blackboard.data
        assertEquals(mapOf("key" to "value"), data)
        
        // Verify it's a snapshot
        blackboard["key2"] = "value2"
        assertFalse(data.containsKey("key2"))
    }

    @Test
    fun `blackboard getOrDefault works`() {
        val blackboard = Blackboard()
        blackboard["existing"] = 10
        
        assertEquals(10, blackboard.getOrDefault("existing", 0))
        assertEquals(0, blackboard.getOrDefault("nonexistent", 0))
    }

    @Test
    fun `blackboard getOrPut computes missing values`() {
        val blackboard = Blackboard()
        
        var computeCount = 0
        val result1 = blackboard.getOrPut("key") { 
            computeCount++
            "computed" 
        }
        val result2 = blackboard.getOrPut("key") { 
            computeCount++
            "computed again" 
        }
        
        assertEquals("computed", result1)
        assertEquals("computed", result2)
        assertEquals(1, computeCount) // Should only compute once
    }

    // ===== CONDITION TESTS =====

    @Test
    fun `condition succeeds when predicate is true`() {
        val blackboard = Blackboard()
        blackboard["flag"] = true
        
        val condition = Condition("CheckFlag") { bb -> bb?.get<Boolean>("flag") == true }
        condition.blackboard = blackboard
        
        // Simulate scheduler lifecycle
        simulateEnter(condition)
        simulateExecute(condition)
        
        assertEquals(BtStatus.SUCCESS, condition.status)
    }

    @Test
    fun `condition fails when predicate is false`() {
        val blackboard = Blackboard()
        blackboard["flag"] = false
        
        val condition = Condition("CheckFlag") { bb -> bb?.get<Boolean>("flag") == true }
        condition.blackboard = blackboard
        
        simulateEnter(condition)
        simulateExecute(condition)
        
        assertEquals(BtStatus.FAILURE, condition.status)
    }

    // ===== BT ACTION TESTS =====

    @Test
    fun `btAction succeeds when succeed is called`() {
        var workDone = false
        val action = BtAction("DoWork") {
            workDone = true
            succeed()
        }
        
        simulateEnter(action)
        simulateExecute(action)
        
        assertTrue(workDone)
        assertEquals(BtStatus.SUCCESS, action.status)
    }

    @Test
    fun `btAction fails when fail is called`() {
        val action = BtAction("FailingAction") {
            fail()
        }
        
        simulateEnter(action)
        simulateExecute(action)
        
        assertEquals(BtStatus.FAILURE, action.status)
    }

    @Test
    fun `btAction can access blackboard`() {
        val blackboard = Blackboard()
        val action = BtAction("BlackboardAction") {
            blackboard?.set("result", "success")
            succeed()
        }
        action.blackboard = blackboard
        
        simulateEnter(action)
        simulateExecute(action)
        
        assertEquals("success", blackboard.get<String>("result"))
    }

    // ===== INVERTER TESTS =====

    @Test
    fun `inverter flips success to failure`() {
        val successCondition = Condition("AlwaysTrue") { true }
        val inverter = Inverter("Invert", successCondition)
        
        // Mock the child completing with SUCCESS
        successCondition.blackboard = Blackboard()
        simulateEnter(successCondition)
        simulateExecute(successCondition)
        
        // Now simulate inverter seeing the completed child
        simulateEnter(inverter)
        // Child is already done, so inverter should react
        simulateExecute(inverter)
        
        assertEquals(BtStatus.FAILURE, inverter.status)
    }

    @Test
    fun `inverter flips failure to success`() {
        val failCondition = Condition("AlwaysFalse") { false }
        val inverter = Inverter("Invert", failCondition)
        
        failCondition.blackboard = Blackboard()
        simulateEnter(failCondition)
        simulateExecute(failCondition)
        
        simulateEnter(inverter)
        simulateExecute(inverter)
        
        assertEquals(BtStatus.SUCCESS, inverter.status)
    }

    // ===== DSL TESTS =====

    @Test
    fun `behaviorTree DSL creates tree with blackboard`() {
        val tree = behaviorTree("TestTree") {
            selector("Root") {
                condition("Check") { true }
            }
        }
        
        assertEquals("TestTree", tree.name)
        assertNotNull(tree.root)
        assertNotNull(tree.blackboard)
        assertEquals(tree.blackboard, tree.root.blackboard)
    }

    @Test
    fun `behaviorTree DSL supports nested structures`() {
        val tree = behaviorTree("NestedTree") {
            selector("Root") {
                sequence("Branch1") {
                    condition("C1") { true }
                    action("A1") { succeed() }
                }
                sequence("Branch2") {
                    inverter {
                        condition("C2") { false }
                    }
                    action("A2") { succeed() }
                }
            }
        }
        
        assertNotNull(tree.root)
        assertTrue(tree.root is Selector)
    }

    @Test
    fun `behaviorTree with existing blackboard`() {
        val existingBlackboard = Blackboard()
        existingBlackboard["preset"] = "value"
        
        val tree = behaviorTree("Tree", existingBlackboard) {
            condition("Check") { bb -> bb?.get<String>("preset") == "value" }
        }
        
        assertEquals(existingBlackboard, tree.blackboard)
        assertEquals("value", tree.blackboard.get<String>("preset"))
    }

    // ===== TRACER TESTS =====

    @Test
    fun `btTracer collects node snapshots`() {
        val tree = behaviorTree("TraceTest") {
            selector("Root") {
                condition("C1") { true }
                condition("C2") { false }
            }
        }
        
        val snapshots = BtTracer.collectNodeSnapshots(tree.root)
        
        // Should have 3 nodes: Root selector + 2 conditions
        assertEquals(3, snapshots.size)
        
        val rootSnapshot = snapshots.find { it.name == "Root" }
        assertNotNull(rootSnapshot)
        assertEquals("Selector", rootSnapshot?.type)
        assertNull(rootSnapshot?.parentId)
        
        val c1Snapshot = snapshots.find { it.name == "C1" }
        assertNotNull(c1Snapshot)
        assertEquals(tree.root.id, c1Snapshot?.parentId)
    }

    // ===== HELPER FUNCTIONS =====

    private fun simulateEnter(command: BtCommand<*>) {
        val method = command::class.java.superclass.getDeclaredMethod("schedulerEnter")
        method.isAccessible = true
        method.invoke(command)
    }

    private fun simulateExecute(command: BtCommand<*>) {
        val method = command::class.java.superclass.getDeclaredMethod("execute")
        method.isAccessible = true
        method.invoke(command)
    }
}
