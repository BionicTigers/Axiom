/**
 * internal data class SchedulerDetails(
    val tick: Long,
    val executionTime: Double,
    val currentTime: Double
) : Serializable {
    override fun serialize(tick: Long): Map<String, Any?> {
        return mapOf(
            "name" to "scheduler_details",
            "tick" to tick,
            "data" to mapOf(
                "tick" to this.tick,
                "executionTime" to executionTime,
                "currentTime" to currentTime
            )
        )
    }
}
 */
import { writable, derived, get } from 'svelte/store'

import { registerNetworkEvent } from '../networkRegistry'
import {
  recordSnapshot,
  displayedSnapshot,
  isPaused,
  type TickSnapshot
} from './historyStore'
import {
  schedulableOrderStore,
  createSchedulablesSnapshot
} from './schedulableStore'
import { getCurrentBehaviorTreeSnapshots } from './behaviorTreeStore'

export type SchedulerDetails = {
  tick: number
  executionTime: number
  currentTime: number
}

export const schedulerDetails = writable<SchedulerDetails>({
  tick: 0,
  executionTime: 0,
  currentTime: 0
})

// Max history in seconds (covers max graph window)
const MAX_HISTORY_SECONDS = 120

export const tickToTime = writable<Map<number, number>>(new Map())

// Keep track of ticks in order for efficient pruning
const tickOrder: number[] = []

// ============================================================================
// Displayed Scheduler Details (historical or live based on pause state)
// ============================================================================

export const displayedSchedulerDetails = derived(
  [displayedSnapshot, schedulerDetails, isPaused],
  ([$snapshot, $live, $isPaused]) => {
    // Only use snapshot when actually paused
    if ($isPaused && $snapshot) {
      return $snapshot.scheduler
    }
    return $live
  }
)

// ============================================================================
// Network Event Handler
// ============================================================================

registerNetworkEvent('scheduler_details', (data: SchedulerDetails) => {
  schedulerDetails.set(data)

  tickToTime.update((map) => {
    map.set(data.tick, data.currentTime)
    tickOrder.push(data.tick)

    // Prune old entries beyond MAX_HISTORY_SECONDS
    const cutoffTime = data.currentTime - MAX_HISTORY_SECONDS
    while (tickOrder.length > 0) {
      const oldestTick = tickOrder[0]
      const oldestTime = map.get(oldestTick)
      if (oldestTime !== undefined && oldestTime < cutoffTime) {
        map.delete(oldestTick)
        tickOrder.shift()
      } else {
        break
      }
    }

    return map
  })

  // Record snapshot for history
  // This is called at the end of each tick after all other updates
  const snapshot: TickSnapshot = {
    tick: data.tick,
    timestamp: Date.now(),
    scheduler: {
      tick: data.tick,
      executionTime: data.executionTime,
      currentTime: data.currentTime
    },
    schedulables: createSchedulablesSnapshot(),
    schedulableOrder: [...get(schedulableOrderStore)],
    behaviorTrees: getCurrentBehaviorTreeSnapshots()
  }

  recordSnapshot(snapshot)
})
