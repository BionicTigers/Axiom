import { get, writable, derived } from 'svelte/store'

import {
  schedulableStore,
  type CommandStateValue,
  type Schedulable
} from './schedulableStore'
import { schedulerDetails } from './schedulerDetails'
import type { UUID } from '../types'

export type SeriesId = {
  schedulableId: UUID
  field: string
}

export type SeriesData = {
  id: SeriesId
  label: string
  color: string
  y: (number | null)[]
}

// Graph state
export const windowSeconds = writable(10)
export const selectedSeries = writable<SeriesId[]>([])
export const paused = writable(false)

// Data buffers
export const xData = writable<number[]>([])
export const seriesDataMap = writable<Map<string, SeriesData>>(new Map())

// Track start time for relative timestamps
let startTime: number | null = null

// Helper to create a unique key for a series
export function seriesKey(id: SeriesId): string {
  return `${id.schedulableId}:${id.field}`
}

// Stable color palette for series
const SERIES_COLORS = [
  '#4dabf7', // blue
  '#69db7c', // green
  '#ffa94d', // orange
  '#ff6b6b', // red
  '#da77f2', // purple
  '#ffd43b', // yellow
  '#38d9a9', // teal
  '#f783ac', // pink
  '#748ffc', // indigo
  '#a9e34b' // lime
]

// Get a stable color based on series key hash
function getSeriesColor(key: string): string {
  let hash = 0
  for (let i = 0; i < key.length; i++) {
    hash = (hash << 5) - hash + key.charCodeAt(i)
    hash |= 0
  }
  return SERIES_COLORS[Math.abs(hash) % SERIES_COLORS.length]
}

// Toggle a series on/off
export function toggleSeries(id: SeriesId): void {
  const key = seriesKey(id)
  const currentX = get(xData)

  selectedSeries.update((list) => {
    const idx = list.findIndex((s) => seriesKey(s) === key)
    if (idx >= 0) {
      // Remove series
      seriesDataMap.update((map) => {
        map.delete(key)
        return map
      })
      return [...list.slice(0, idx), ...list.slice(idx + 1)]
    } else {
      // Add series - pad y-array with nulls to match current x length
      const schedulables = get(schedulableStore)
      const schedulable = schedulables.get(id.schedulableId)
      const label = schedulable ? `${schedulable.name}.${id.field}` : id.field

      seriesDataMap.update((map) => {
        // Fill with nulls for existing time points
        const y: (number | null)[] = new Array(currentX.length).fill(null)
        map.set(key, {
          id,
          label,
          color: getSeriesColor(key),
          y
        })
        return map
      })
      return [...list, id]
    }
  })
}

// Check if a series is selected
export function isSeriesSelected(id: SeriesId): boolean {
  const key = seriesKey(id)
  return get(selectedSeries).some((s) => seriesKey(s) === key)
}

// Clear all series
export function clearAllSeries(): void {
  selectedSeries.set([])
  seriesDataMap.set(new Map())
  xData.set([])
  startTime = null
}

// Toggle pause
export function togglePause(): void {
  paused.update((p) => !p)
}

// Set window seconds
export function setWindowSeconds(seconds: number): void {
  windowSeconds.set(Math.max(1, Math.min(120, seconds)))
  pruneData()
}

// Prune data to fit within window
function pruneData(): void {
  const x = get(xData)
  if (x.length === 0) return

  const window = get(windowSeconds)
  const latestTime = x[x.length - 1]
  const cutoff = latestTime - window

  let cutIndex = 0
  while (cutIndex < x.length && x[cutIndex] < cutoff) {
    cutIndex++
  }

  if (cutIndex > 0) {
    xData.update((arr) => arr.slice(cutIndex))
    seriesDataMap.update((map) => {
      for (const series of map.values()) {
        series.y = series.y.slice(cutIndex)
      }
      return map
    })
  }
}

// Get numeric fields from a schedulable
export function getNumericFields(schedulable: Schedulable): string[] {
  const stateMap = get(schedulable.state)
  const fields: string[] = []

  for (const [field, state] of stateMap) {
    if (
      typeof state === 'object' &&
      state !== null &&
      'value' in state &&
      !Array.isArray(state)
    ) {
      const val = (state as CommandStateValue).value
      if (typeof val === 'number') {
        fields.push(field)
      }
    }
  }

  return fields
}

// Derived store for uPlot-compatible data format
export const graphData = derived(
  [xData, selectedSeries, seriesDataMap],
  ([$x, $selected, $dataMap]) => {
    const data: (number | null)[][] = [$x]

    for (const id of $selected) {
      const key = seriesKey(id)
      const series = $dataMap.get(key)
      if (series) {
        // Ensure y-array matches x-array length
        const y = series.y
        if (y.length < $x.length) {
          // Pad with nulls if needed
          const padded = [...y, ...new Array($x.length - y.length).fill(null)]
          data.push(padded)
        } else if (y.length > $x.length) {
          // Trim if needed
          data.push(y.slice(0, $x.length))
        } else {
          data.push(y)
        }
      } else {
        // No data for this series, fill with nulls
        data.push(new Array($x.length).fill(null))
      }
    }

    return data
  }
)

// Derived store for series metadata (for uPlot options)
export const seriesMeta = derived([selectedSeries, seriesDataMap], ([$selected, $dataMap]) => {
  return $selected.map((id) => {
    const key = seriesKey(id)
    const series = $dataMap.get(key)
    return {
      id,
      key,
      label: series?.label ?? id.field,
      color: series?.color ?? '#ffffff'
    }
  })
})

// Sample current values on each scheduler tick by subscribing to schedulerDetails
let lastTick = -1

schedulerDetails.subscribe((data) => {
  // Skip if paused
  if (get(paused)) return

  // Skip if we've already processed this tick
  if (data.tick === lastTick || data.tick === 0) return
  lastTick = data.tick

  const selected = get(selectedSeries)
  if (selected.length === 0) return

  const schedulables = get(schedulableStore)
  const currentTime = data.currentTime

  // Initialize start time on first sample
  if (startTime === null) {
    startTime = currentTime
  }

  // Calculate relative time
  const relativeTime = currentTime - startTime

  // Add time point
  xData.update((arr) => [...arr, relativeTime])

  // Sample each selected series
  seriesDataMap.update((map) => {
    for (const id of selected) {
      const key = seriesKey(id)
      const series = map.get(key)
      if (!series) continue

      const schedulable = schedulables.get(id.schedulableId)
      let value: number | null = null

      if (schedulable) {
        const stateMap = get(schedulable.state)
        const state = stateMap.get(id.field)
        if (
          state &&
          typeof state === 'object' &&
          'value' in state &&
          !Array.isArray(state)
        ) {
          const v = (state as CommandStateValue).value
          if (typeof v === 'number') {
            value = v
          }
        }
      }

      series.y.push(value)
    }
    return map
  })

  // Prune old data
  pruneData()
})
