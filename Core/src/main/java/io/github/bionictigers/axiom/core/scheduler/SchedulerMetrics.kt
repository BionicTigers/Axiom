package io.github.bionictigers.axiom.core.scheduler

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tracks detailed timing metrics for the scheduler with rolling averages.
 */
internal object SchedulerMetrics {
    private const val ROLLING_WINDOW_SIZE = 50
    
    // Rolling average buffers
    private val totalTimeBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val commandTimeBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val queueProcessingBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val dependencySortBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val serializationBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val serializedStatesBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val serializedFieldsBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val deltaResolutionBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val networkSendBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    private val connectionCallbackBuffer = RollingAverage(ROLLING_WINDOW_SIZE)
    
    // Current tick measurements (in milliseconds)
    var currentTotalTime: Double = 0.0
        private set
    var currentCommandTime: Double = 0.0
        private set
    var currentQueueProcessing: Double = 0.0
        private set
    var currentDependencySort: Double = 0.0
        private set
    var currentSerialization: Double = 0.0
        private set
    var currentSerializedStates: Double = 0.0
        private set
    var currentSerializedFields: Double = 0.0
        private set
    var currentDeltaResolution: Double = 0.0
        private set
    var currentNetworkSend: Double = 0.0
        private set
    var currentConnectionCallbacks: Double = 0.0
        private set
    
    // Rolling averages (in milliseconds)
    val avgTotalTime: Double get() = totalTimeBuffer.average
    val avgCommandTime: Double get() = commandTimeBuffer.average
    val avgQueueProcessing: Double get() = queueProcessingBuffer.average
    val avgDependencySort: Double get() = dependencySortBuffer.average
    val avgSerialization: Double get() = serializationBuffer.average
    val avgSerializedStates: Double get() = serializedStatesBuffer.average
    val avgSerializedFields: Double get() = serializedFieldsBuffer.average
    val avgDeltaResolution: Double get() = deltaResolutionBuffer.average
    val avgNetworkSend: Double get() = networkSendBuffer.average
    val avgConnectionCallbacks: Double get() = connectionCallbackBuffer.average
    
    // Computed metrics
    val avgAxiomOverhead: Double 
        get() = avgTotalTime - avgCommandTime
    
    val currentAxiomOverhead: Double 
        get() = currentTotalTime - currentCommandTime
    
    fun recordTotalTime(duration: Duration) {
        currentTotalTime = duration.inWholeNanoseconds / 1_000_000.0
        totalTimeBuffer.add(currentTotalTime)
    }
    
    fun recordCommandTime(duration: Duration) {
        currentCommandTime = duration.inWholeNanoseconds / 1_000_000.0
        commandTimeBuffer.add(currentCommandTime)
    }
    
    fun recordQueueProcessing(duration: Duration) {
        currentQueueProcessing = duration.inWholeNanoseconds / 1_000_000.0
        queueProcessingBuffer.add(currentQueueProcessing)
    }
    
    fun recordDependencySort(duration: Duration) {
        currentDependencySort = duration.inWholeNanoseconds / 1_000_000.0
        dependencySortBuffer.add(currentDependencySort)
    }
    
    fun recordSerialization(duration: Duration) {
        currentSerialization = duration.inWholeNanoseconds / 1_000_000.0
        serializationBuffer.add(currentSerialization)
    }

    fun recordSerializedCounts(states: Int, fields: Int) {
        currentSerializedStates = states.toDouble()
        currentSerializedFields = fields.toDouble()
        serializedStatesBuffer.add(currentSerializedStates)
        serializedFieldsBuffer.add(currentSerializedFields)
    }
    
    fun recordDeltaResolution(duration: Duration) {
        currentDeltaResolution = duration.inWholeNanoseconds / 1_000_000.0
        deltaResolutionBuffer.add(currentDeltaResolution)
    }
    
    fun recordNetworkSend(duration: Duration) {
        currentNetworkSend = duration.inWholeNanoseconds / 1_000_000.0
        networkSendBuffer.add(currentNetworkSend)
    }
    
    fun recordConnectionCallbacks(duration: Duration) {
        currentConnectionCallbacks = duration.inWholeNanoseconds / 1_000_000.0
        connectionCallbackBuffer.add(currentConnectionCallbacks)
    }
    
    fun clear() {
        totalTimeBuffer.clear()
        commandTimeBuffer.clear()
        queueProcessingBuffer.clear()
        dependencySortBuffer.clear()
        serializationBuffer.clear()
        serializedStatesBuffer.clear()
        serializedFieldsBuffer.clear()
        deltaResolutionBuffer.clear()
        networkSendBuffer.clear()
        connectionCallbackBuffer.clear()
        
        currentTotalTime = 0.0
        currentCommandTime = 0.0
        currentQueueProcessing = 0.0
        currentDependencySort = 0.0
        currentSerialization = 0.0
        currentSerializedStates = 0.0
        currentSerializedFields = 0.0
        currentDeltaResolution = 0.0
        currentNetworkSend = 0.0
        currentConnectionCallbacks = 0.0
    }
    
    /**
     * Simple rolling average implementation using a circular buffer.
     */
    private class RollingAverage(private val size: Int) {
        private val buffer = DoubleArray(size)
        private var index = 0
        private var count = 0
        private var sum = 0.0
        
        val average: Double
            get() = if (count == 0) 0.0 else sum / count
        
        fun add(value: Double) {
            if (count == size) {
                sum -= buffer[index]
            } else {
                count++
            }
            
            buffer[index] = value
            sum += value
            index = (index + 1) % size
        }
        
        fun clear() {
            index = 0
            count = 0
            sum = 0.0
            buffer.fill(0.0)
        }
    }
}
