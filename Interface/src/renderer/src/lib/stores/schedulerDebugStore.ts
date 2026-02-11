import { writable, derived } from 'svelte/store'
import { registerNetworkEvent } from '../networkRegistry'
import { isPaused, displayedSnapshot } from './historyStore'

export type SchedulerDebugMetrics = {
  current: {
    total: number
    commands: number
    axiomOverhead: number
    queueProcessing: number
    dependencySort: number
    serialization: number
    serializedStates: number
    serializedFields: number
    deltaResolution: number
    networkSend: number
    connectionCallbacks: number
  }
  average: {
    total: number
    commands: number
    axiomOverhead: number
    queueProcessing: number
    dependencySort: number
    serialization: number
    serializedStates: number
    serializedFields: number
    deltaResolution: number
    networkSend: number
    connectionCallbacks: number
  }
  commandCount: number
}

const defaultMetrics: SchedulerDebugMetrics = {
  current: {
    total: 0,
    commands: 0,
    axiomOverhead: 0,
    queueProcessing: 0,
    dependencySort: 0,
    serialization: 0,
    serializedStates: 0,
    serializedFields: 0,
    deltaResolution: 0,
    networkSend: 0,
    connectionCallbacks: 0
  },
  average: {
    total: 0,
    commands: 0,
    axiomOverhead: 0,
    queueProcessing: 0,
    dependencySort: 0,
    serialization: 0,
    serializedStates: 0,
    serializedFields: 0,
    deltaResolution: 0,
    networkSend: 0,
    connectionCallbacks: 0
  },
  commandCount: 0
}

export const schedulerDebugStore = writable<SchedulerDebugMetrics>(defaultMetrics)

// Displayed metrics (respects pause state)
export const displayedSchedulerDebug = derived(
  [schedulerDebugStore, isPaused],
  ([$live, $isPaused]) => {
    // For now, always show live data (debug metrics aren't recorded in history)
    // Could be extended to record in snapshots if needed
    return $live
  }
)

// Register the network event handler
registerNetworkEvent('scheduler_debug', (data: SchedulerDebugMetrics) => {
  schedulerDebugStore.set(data)
})
