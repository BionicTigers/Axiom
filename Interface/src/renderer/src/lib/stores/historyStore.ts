/**
 * Universal History Store
 *
 * Provides global pause/resume and tick history for viewing previous states.
 * All network data flows through this store, which records snapshots per tick.
 * Components can then display either live data or historical data based on pause state.
 */

import { writable, derived, get } from 'svelte/store'
import type { UUID } from '../types'
import type { CommandState } from './schedulableStore'

// ============================================================================
// Types
// ============================================================================

export interface SchedulerSnapshot {
  tick: number
  executionTime: number
  currentTime: number
}

export interface SchedulableSnapshot {
  name: string
  type: 'Command' | 'System'
  parent: UUID | null
  state: Record<string, CommandState>
}

export interface BehaviorTreeSnapshot {
  treeId: string
  treeName: string
  activeNodePath: string[]
  nodes: Array<{
    id: string
    name: string
    type: string
    status: 'SUCCESS' | 'FAILURE' | 'RUNNING'
    active: boolean
    parentId: string | null
  }>
  blackboard: Record<string, unknown>
}

export interface TickSnapshot {
  tick: number
  timestamp: number
  scheduler: SchedulerSnapshot
  schedulables: Map<UUID, SchedulableSnapshot>
  schedulableOrder: UUID[]
  behaviorTrees: Map<string, BehaviorTreeSnapshot>
}

// ============================================================================
// Configuration
// ============================================================================

const MAX_HISTORY_SIZE = 500

// ============================================================================
// Stores
// ============================================================================

// Core history storage
export const history = writable<TickSnapshot[]>([])

// Playback state
export const isPaused = writable<boolean>(false)

// Currently viewing tick index (null = live/latest)
export const viewingIndex = writable<number | null>(null)

// Latest tick number received (for display purposes)
export const latestTick = writable<number>(0)

// ============================================================================
// Derived Stores
// ============================================================================

// Get the currently displayed snapshot (live or historical)
export const displayedSnapshot = derived(
  [history, viewingIndex, isPaused],
  ([$history, $viewingIndex, $isPaused]) => {
    if ($history.length === 0) return null

    // If paused with a specific index, show that
    if ($isPaused && $viewingIndex !== null && $history[$viewingIndex]) {
      return $history[$viewingIndex]
    }

    // Otherwise show latest
    return $history[$history.length - 1]
  }
)

// Whether we're viewing historical data vs live
export const isViewingHistorical = derived(
  [isPaused, viewingIndex, history],
  ([$isPaused, $viewingIndex, $history]) => {
    if (!$isPaused) return false
    if ($viewingIndex === null) return false
    return $viewingIndex < $history.length - 1
  }
)

// Current history bounds for slider
export const historyBounds = derived(history, ($history) => ({
  min: 0,
  max: Math.max(0, $history.length - 1),
  firstTick: $history[0]?.tick ?? 0,
  lastTick: $history[$history.length - 1]?.tick ?? 0
}))

// ============================================================================
// Actions
// ============================================================================

/**
 * Record a new tick snapshot to history
 * Does NOT record while paused - history is frozen during pause
 */
export function recordSnapshot(snapshot: TickSnapshot): void {
  const currentlyPaused = get(isPaused)

  // Don't record history while paused - we want to freeze the view
  if (currentlyPaused) {
    // Still update latestTick so we know what's happening live
    latestTick.set(snapshot.tick)
    return
  }

  history.update((h) => {
    const newHistory = [...h, snapshot]

    // Trim history if too large
    if (newHistory.length > MAX_HISTORY_SIZE) {
      const removeCount = newHistory.length - MAX_HISTORY_SIZE
      newHistory.splice(0, removeCount)
    }

    return newHistory
  })

  latestTick.set(snapshot.tick)
}

/**
 * Toggle pause state
 */
export function togglePause(): void {
  isPaused.update((paused) => {
    if (!paused) {
      // Pausing: set viewing index to latest
      const h = get(history)
      if (h.length > 0) {
        viewingIndex.set(h.length - 1)
      }
    } else {
      // Resuming: clear viewing index to follow live
      viewingIndex.set(null)
    }
    return !paused
  })
}

/**
 * Set pause state explicitly
 */
export function setPaused(paused: boolean): void {
  const currentlyPaused = get(isPaused)

  if (paused && !currentlyPaused) {
    // Pausing
    const h = get(history)
    if (h.length > 0) {
      viewingIndex.set(h.length - 1)
    }
  } else if (!paused && currentlyPaused) {
    // Resuming
    viewingIndex.set(null)
  }

  isPaused.set(paused)
}

/**
 * Resume live view (unpause and follow latest)
 */
export function resumeLive(): void {
  isPaused.set(false)
  viewingIndex.set(null)
}

/**
 * Step backward in history
 */
export function stepBackward(): void {
  const h = get(history)
  if (h.length === 0) return

  // Auto-pause if not already
  if (!get(isPaused)) {
    isPaused.set(true)
    viewingIndex.set(h.length - 1)
  }

  viewingIndex.update((idx) => {
    if (idx === null) return Math.max(0, h.length - 2)
    return Math.max(0, idx - 1)
  })
}

/**
 * Step forward in history
 */
export function stepForward(): void {
  const h = get(history)
  if (h.length === 0) return

  if (!get(isPaused)) return // Can't step forward when live

  viewingIndex.update((idx) => {
    if (idx === null) return null
    const newIdx = idx + 1
    if (newIdx >= h.length) {
      return h.length - 1
    }
    return newIdx
  })
}

/**
 * Jump to start of history
 */
export function jumpToStart(): void {
  const h = get(history)
  if (h.length === 0) return

  if (!get(isPaused)) {
    isPaused.set(true)
  }
  viewingIndex.set(0)
}

/**
 * Jump to end of history (latest)
 */
export function jumpToEnd(): void {
  const h = get(history)
  if (h.length === 0) return

  viewingIndex.set(h.length - 1)
}

/**
 * Set viewing index directly (for slider)
 */
export function setViewingIndex(index: number): void {
  const h = get(history)
  if (h.length === 0) return

  if (!get(isPaused)) {
    isPaused.set(true)
  }

  viewingIndex.set(Math.max(0, Math.min(index, h.length - 1)))
}

/**
 * Clear all history
 */
export function clearHistory(): void {
  history.set([])
  viewingIndex.set(null)
  latestTick.set(0)
}

/**
 * Get display info for current state
 */
export const displayInfo = derived(
  [displayedSnapshot, isPaused, isViewingHistorical, history],
  ([$snapshot, $isPaused, $isHistorical, $history]) => ({
    tick: $snapshot?.tick ?? 0,
    isPaused: $isPaused,
    isHistorical: $isHistorical,
    historyLength: $history.length
  })
)
