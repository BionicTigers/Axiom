package io.github.bionictigers.axiom.core.scheduler

import com.qualcomm.robotcore.util.RobotLog
import io.github.bionictigers.axiom.core.commands.Command
import io.github.bionictigers.axiom.core.commands.GenericCommand
import io.github.bionictigers.axiom.core.commands.System
import io.github.bionictigers.axiom.core.web.Server
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesInitial
import io.github.bionictigers.axiom.core.web.serializable.SchedulablesUpdate
import io.github.bionictigers.axiom.core.web.serializable.SchedulerDebug
import io.github.bionictigers.axiom.core.web.serializable.SchedulerDetails
import io.github.bionictigers.axiom.core.web.serializable.SchedulerOrder
import io.github.bionictigers.axiom.core.web.serializable.StateUpdate
import org.firstinspires.ftc.robotcore.external.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.measureTime

object Scheduler {
    private enum class WarmupType {
        Command,
        System
    }

    private data class WarmupTarget(
        val type: WarmupType,
        val id: String
    )

    private data class StreamSnapshot(
        val snapshot: List<DeltaResolver.SnapshotSchedulable>,
        val snapshotTime: Duration
    )

    private data class StreamMetrics(
        val snapshotTime: Duration,
        val serializationTime: Duration,
        val deltaTime: Duration,
        val networkTime: Duration,
        val stateUpdateCount: Int,
        val fieldUpdateCount: Int
    )

    private val streamScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val streamChannel = Channel<StreamSnapshot>(capacity = Channel.CONFLATED)
    private val streamBusy = AtomicBoolean(false)

    @Volatile
    private var warmupQueue: ArrayDeque<WarmupTarget> = ArrayDeque()

    @Volatile
    private var warmupActive = false

    @Volatile
    private var lastStreamMetrics: StreamMetrics? = null

    @Volatile
    private var lastSnapshot: List<DeltaResolver.SnapshotSchedulable>? = null

    val tick: Long
        get() = SchedulerState.tick

    var telemetry: Telemetry?
        set(value) {
            SchedulerState.telemetry = value
        }
        get() = SchedulerState.telemetry
    
    init {
        streamScope.launch {
            streamChannel.consumeEach { request ->
                try {
                    val deltaReport: DeltaResolver.DeltaReport
                    val deltaTime = measureTime {
                        deltaReport = DeltaResolver.resolveSnapshot(
                            request.snapshot,
                            allowRemovals = !warmupActive
                        )
                    }

                    val networkTime = measureTime {
                        if (deltaReport.structureUpdates.isNotEmpty() || deltaReport.removals.isNotEmpty()) {
                            try {
                                Server.send(
                                    SchedulablesUpdate(
                                        deltaReport.structureUpdates,
                                        deltaReport.removals
                                    )
                                )
                            } catch (_: Exception) {
                                // Ignore server errors
                            }
                        }

                        if (deltaReport.stateUpdates.isNotEmpty()) {
                            try {
                                Server.send(StateUpdate(deltaReport.stateUpdates))
                            } catch (_: Exception) {
                                // Ignore server errors
                            }
                        }
                    }

                    lastStreamMetrics = StreamMetrics(
                        request.snapshotTime,
                        Duration.ZERO,
                        deltaTime,
                        networkTime,
                        deltaReport.stateUpdateCount,
                        deltaReport.fieldUpdateCount
                    )
                } catch (_: Exception) {
                    // Ignore streaming errors
                } finally {
                    streamBusy.set(false)
                }
            }
        }

        Server.onNewConnection { send ->
            val commands = SchedulerState.commands
            val systems = SchedulerState.systems

            // Seed snapshot nodes so warmup doesn't emit structure updates.
            DeltaResolver.ensureSnapshotNodes(commands, systems)

            // Send a lightweight initial payload without reflective state serialization.
            val initial = ArrayList<io.github.bionictigers.axiom.core.web.serializable.Schedulable>(
                commands.size + systems.size
            )
            commands.values.forEach { cmd ->
                initial.add(
                    io.github.bionictigers.axiom.core.web.serializable.Schedulable(
                        cmd.name,
                        cmd.id,
                        emptyMap(),
                        cmd.parent?.id,
                        io.github.bionictigers.axiom.core.web.serializable.ObjectType.Command
                    )
                )
            }
            systems.values.forEach { sys ->
                initial.add(
                    io.github.bionictigers.axiom.core.web.serializable.Schedulable(
                        sys.name,
                        sys.id,
                        emptyMap(),
                        null,
                        io.github.bionictigers.axiom.core.web.serializable.ObjectType.System
                    )
                )
            }
            send(SchedulablesInitial(initial))
            send(SchedulerOrder(SchedulerState.sortedCommands.map { it.id }))

            warmupQueue = ArrayDeque(
                commands.keys.map { WarmupTarget(WarmupType.Command, it) } +
                    systems.keys.map { WarmupTarget(WarmupType.System, it) }
            )
            warmupActive = warmupQueue.isNotEmpty()
        }

//        Server.start()
    }

    fun schedule(vararg command: Command<*>) = command.forEach(::schedule)
    @JvmName("scheduleCommands")
    fun schedule(commands: Collection<Command<*>>) = commands.forEach(::schedule)
    fun schedule(command: Command<*>) {
        if (SchedulerState.inUpdateCycle) {
            SchedulerState.addQueue.add(command)
        } else {
            SchedulerState.add(command)
        }
    }

    fun schedule(vararg system: System) = system.forEach(::schedule)
    @JvmName("scheduleSystems")
    fun schedule(systems: Collection<System>) = systems.forEach(::schedule)
    fun schedule(system: System) {
        SchedulerState.addSystem(system)
        system.update?.parent = system
        system.apply?.parent = system
        schedule(listOfNotNull(system.update, system.apply))
    }

    fun unschedule(vararg command: Command<*>) = command.forEach(::unschedule)
    fun unschedule(commands: Collection<Command<*>>) = commands.forEach(::unschedule)
    fun unschedule(command: Command<*>) {
        if (SchedulerState.inUpdateCycle) {
            SchedulerState.removeQueue.add(command)
        } else {
            SchedulerState.remove(command)
        }
    }

    fun edit(path: String, value: String) {
        RobotLog.dd("Axiom", "Edit: $path = $value")
        if (SchedulerState.inUpdateCycle) {
            SchedulerState.editQueue.add(path to value)
        } else {
            PropertyEditor.edit(path, value)
        }
    }

    /**
     * Clear the scheduler state.
     */
    fun clear() {
        SchedulerState.clear()
        SchedulerMetrics.clear()
    }

    fun tick() {
        SchedulerState.inUpdateCycle = true
        
        var queueTime = Duration.ZERO
        var sortTime = Duration.ZERO
        var commandTime = Duration.ZERO
        var serializationTime = Duration.ZERO
        var deltaTime = Duration.ZERO
        var networkTime = Duration.ZERO
        var connectionTime = Duration.ZERO
        var snapshotTime = Duration.ZERO
        var serializedStateCount = 0
        var serializedFieldCount = 0

        try {
            SchedulerState.deltaTime = measureTime {
                // Queue processing
                queueTime = measureTime {
                    SchedulerState.processEditQueue()
                    SchedulerState.processAddQueue()
                }

                // Dependency sorting
                if (SchedulerState.changed) {
                    sortTime = measureTime {
                        SchedulerState.sortedCommands = DependencyResolver.sort(SchedulerState.commands)
                    }
                    SchedulerState.changed = false

                    if (Server.hasConnections) {
                        try {
                            Server.send(SchedulerOrder(SchedulerState.sortedCommands.map { it.id }))
                        } catch (e: Exception) {
                            // Ignore server errors
                        }
                    }
                }

                // Command execution
                commandTime = measureTime {
                    SchedulerState.sortedCommands.forEach(GenericCommand::execute)
                }

                // Remove queue processing (part of queue time)
                queueTime += measureTime {
                    SchedulerState.processRemoveQueue()
                }
            }

            // Only do serialization/delta/network work if at least one client is connected
            // and the stream interval has elapsed (throttling).
            val nowMs = java.lang.System.nanoTime() / 1_000_000
            val shouldStream = Server.hasConnections &&
                (nowMs - SchedulerState.lastStreamTimeMs >= SchedulerState.streamIntervalMs)

            if (shouldStream && streamBusy.compareAndSet(false, true)) {
                SchedulerState.lastStreamTimeMs = nowMs

                var snapshot = ArrayList<DeltaResolver.SnapshotSchedulable>()
                snapshotTime = measureTime {
                    snapshot = if (warmupActive && warmupQueue.isNotEmpty()) {
                        val commands = HashMap<String, Command<*>>()
                        val systems = HashMap<String, System>()
                        val batchSize = SchedulerState.warmupBatchSize.coerceAtLeast(1)
                        repeat(batchSize) {
                            val target = warmupQueue.removeFirstOrNull() ?: return@repeat
                            when (target.type) {
                                WarmupType.Command -> {
                                    SchedulerState.commands[target.id]?.let { commands[target.id] = it }
                                }
                                WarmupType.System -> {
                                    SchedulerState.systems[target.id]?.let { systems[target.id] = it }
                                }
                            }
                        }
                        if (warmupQueue.isEmpty()) {
                            warmupActive = false
                        }
                        DeltaResolver.snapshot(commands, systems)
                    } else {
                        DeltaResolver.snapshot(SchedulerState.commands, SchedulerState.systems)
                    }
                }
                serializedStateCount = snapshot.count { it.state.isNotEmpty() }
                serializedFieldCount = snapshot.sumOf { it.state.size }
                lastSnapshot = snapshot

                if (!streamChannel.trySend(StreamSnapshot(snapshot, snapshotTime)).isSuccess) {
                    streamBusy.set(false)
                }
            }
        } finally {
            SchedulerState.tick++
            SchedulerState.inUpdateCycle = false

            val streamMetrics = lastStreamMetrics
            val effectiveSnapshotTime = if (snapshotTime > Duration.ZERO) {
                snapshotTime
            } else {
                streamMetrics?.snapshotTime ?: Duration.ZERO
            }
            serializationTime = (streamMetrics?.serializationTime ?: Duration.ZERO) + effectiveSnapshotTime
            deltaTime = streamMetrics?.deltaTime ?: Duration.ZERO
            networkTime = streamMetrics?.networkTime ?: Duration.ZERO
            if (serializedStateCount == 0 && serializedFieldCount == 0 && streamMetrics != null) {
                serializedStateCount = streamMetrics.stateUpdateCount
                serializedFieldCount = streamMetrics.fieldUpdateCount
            }
            
            // Record all metrics
            val totalTime = SchedulerState.deltaTime + serializationTime + deltaTime + networkTime + connectionTime
            SchedulerMetrics.recordTotalTime(totalTime)
            SchedulerMetrics.recordCommandTime(commandTime)
            SchedulerMetrics.recordQueueProcessing(queueTime)
            SchedulerMetrics.recordDependencySort(sortTime)
            SchedulerMetrics.recordSerialization(serializationTime)
            SchedulerMetrics.recordSerializedCounts(serializedStateCount, serializedFieldCount)
            SchedulerMetrics.recordDeltaResolution(deltaTime)
            SchedulerMetrics.recordNetworkSend(networkTime)
            SchedulerMetrics.recordConnectionCallbacks(connectionTime)

            // Only send metrics if clients are connected (always send these, not throttled,
            // so Seek can show scheduler tick rate even when state streaming is throttled)
            if (Server.hasConnections) {
                // Send scheduler details (with rolling average for execution time)
                Server.send(
                    SchedulerDetails(
                        SchedulerState.tick,
                        SchedulerMetrics.avgTotalTime,
                        java.lang.System.currentTimeMillis() / 1000.0
                    )
                )
                
                // Send detailed debug metrics
                Server.send(
                    SchedulerDebug(
                        currentTotal = SchedulerMetrics.currentTotalTime,
                        currentCommands = SchedulerMetrics.currentCommandTime,
                        currentAxiomOverhead = SchedulerMetrics.currentAxiomOverhead,
                        currentQueueProcessing = SchedulerMetrics.currentQueueProcessing,
                        currentDependencySort = SchedulerMetrics.currentDependencySort,
                        currentSerialization = SchedulerMetrics.currentSerialization,
                        currentSerializedStates = SchedulerMetrics.currentSerializedStates,
                        currentSerializedFields = SchedulerMetrics.currentSerializedFields,
                        currentDeltaResolution = SchedulerMetrics.currentDeltaResolution,
                        currentNetworkSend = SchedulerMetrics.currentNetworkSend,
                        currentConnectionCallbacks = SchedulerMetrics.currentConnectionCallbacks,
                        avgTotal = SchedulerMetrics.avgTotalTime,
                        avgCommands = SchedulerMetrics.avgCommandTime,
                        avgAxiomOverhead = SchedulerMetrics.avgAxiomOverhead,
                        avgQueueProcessing = SchedulerMetrics.avgQueueProcessing,
                        avgDependencySort = SchedulerMetrics.avgDependencySort,
                        avgSerialization = SchedulerMetrics.avgSerialization,
                        avgSerializedStates = SchedulerMetrics.avgSerializedStates,
                        avgSerializedFields = SchedulerMetrics.avgSerializedFields,
                        avgDeltaResolution = SchedulerMetrics.avgDeltaResolution,
                        avgNetworkSend = SchedulerMetrics.avgNetworkSend,
                        avgConnectionCallbacks = SchedulerMetrics.avgConnectionCallbacks,
                        commandCount = SchedulerState.sortedCommands.size
                    )
                )
            }
        }
    }
}
