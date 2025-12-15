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
import { writable } from 'svelte/store';

import { registerNetworkEvent } from '../networkRegistry';

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
})
