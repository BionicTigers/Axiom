import { writable, derived, get } from 'svelte/store'
import { registerNetworkEvent } from '../networkRegistry'
import { displayedSnapshot, isPaused, type BehaviorTreeSnapshot } from './historyStore'

/**
 * Status of a BT node
 */
export type BtStatus = 'SUCCESS' | 'FAILURE' | 'RUNNING'

/**
 * Snapshot of a single BT node
 */
export interface NodeSnapshot {
  id: string
  name: string
  type: string
  status: BtStatus
  active: boolean
  parentId: string | null
}

/**
 * Trace data for a behavior tree (live data)
 */
export interface BehaviorTreeTrace {
  treeId: string
  treeName: string
  activeNodePath: string[]
  nodes: NodeSnapshot[]
  blackboard: Record<string, unknown>
  tick: number
  timestamp: number
}

/**
 * State for a single behavior tree (live only, no local history)
 */
export interface BehaviorTreeState {
  treeId: string
  treeName: string
  latestTrace: BehaviorTreeTrace | null
}

// Store for all behavior trees, keyed by treeId (live data only)
export const behaviorTreeStore = writable<Map<string, BehaviorTreeState>>(new Map())

// Derived store for list of tree IDs (for dropdowns)
export const behaviorTreeIds = derived(behaviorTreeStore, ($store) => Array.from($store.keys()))

// Currently selected tree ID for the debugger UI
export const selectedTreeId = writable<string | null>(null)

// Derived store for the selected tree's live state
export const selectedTreeState = derived(
  [behaviorTreeStore, selectedTreeId],
  ([$store, $selectedId]) => {
    if (!$selectedId) return null
    return $store.get($selectedId) || null
  }
)

// ============================================================================
// Displayed Stores (from universal history or live)
// ============================================================================

// Displayed trace for the selected tree (respects global pause/history)
export const displayedTrace = derived(
  [displayedSnapshot, selectedTreeId, selectedTreeState, isPaused],
  ([$snapshot, $selectedId, $liveState, $isPaused]) => {
    if (!$selectedId) return null

    // If paused and we have a historical snapshot with this tree, use it
    if ($isPaused && $snapshot && $snapshot.behaviorTrees.has($selectedId)) {
      const btSnapshot = $snapshot.behaviorTrees.get($selectedId)!
      return {
        tick: $snapshot.tick,
        timestamp: $snapshot.timestamp,
        activeNodePath: btSnapshot.activeNodePath,
        nodes: btSnapshot.nodes,
        blackboard: btSnapshot.blackboard
      }
    }

    // Otherwise show live data
    if ($liveState?.latestTrace) {
      return {
        tick: $liveState.latestTrace.tick,
        timestamp: $liveState.latestTrace.timestamp,
        activeNodePath: $liveState.latestTrace.activeNodePath,
        nodes: $liveState.latestTrace.nodes,
        blackboard: $liveState.latestTrace.blackboard
      }
    }

    return null
  }
)

// ============================================================================
// Snapshot Creation Helper (for universal history)
// ============================================================================

/**
 * Get current BT snapshots for universal history recording
 */
export function getCurrentBehaviorTreeSnapshots(): Map<string, BehaviorTreeSnapshot> {
  const current = get(behaviorTreeStore)
  const snapshots = new Map<string, BehaviorTreeSnapshot>()

  for (const [treeId, state] of current) {
    if (state.latestTrace) {
      snapshots.set(treeId, {
        treeId: state.treeId,
        treeName: state.treeName,
        activeNodePath: [...state.latestTrace.activeNodePath],
        nodes: state.latestTrace.nodes.map((n) => ({ ...n })),
        blackboard: { ...state.latestTrace.blackboard }
      })
    }
  }

  return snapshots
}

// ============================================================================
// Network Event Handler
// ============================================================================

/**
 * Handle incoming behavior_tree_trace event
 */
function handleBehaviorTreeTrace(data: unknown, tick: number) {
  const trace = data as {
    treeId: string
    treeName: string
    activeNodePath: string[]
    nodes: NodeSnapshot[]
    blackboard: Record<string, unknown>
  }

  const fullTrace: BehaviorTreeTrace = {
    ...trace,
    tick,
    timestamp: Date.now()
  }

  behaviorTreeStore.update((store) => {
    // Always update live data (history is handled by universal historyStore)
    store.set(trace.treeId, {
      treeId: trace.treeId,
      treeName: trace.treeName,
      latestTrace: fullTrace
    })

    // Auto-select if this is the first tree
    if (store.size === 1) {
      selectedTreeId.update((current) => (current === null ? trace.treeId : current))
    }

    return new Map(store)
  })
}

// ============================================================================
// Actions
// ============================================================================

/**
 * Clear all behavior tree data
 */
export function clearAllTrees() {
  behaviorTreeStore.set(new Map())
  selectedTreeId.set(null)
}

/**
 * Select a tree by ID
 */
export function selectTree(treeId: string | null) {
  selectedTreeId.set(treeId)
}

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Get status color for a BT status
 */
export function getStatusColor(status: BtStatus): string {
  switch (status) {
    case 'SUCCESS':
      return 'var(--color-success, #4ade80)'
    case 'FAILURE':
      return 'var(--color-error, #f87171)'
    case 'RUNNING':
      return 'var(--color-warning, #fbbf24)'
    default:
      return 'var(--color-text-muted, #9ca3af)'
  }
}

/**
 * Get status icon for a BT status
 */
export function getStatusIcon(status: BtStatus): string {
  switch (status) {
    case 'SUCCESS':
      return '✓'
    case 'FAILURE':
      return '✗'
    case 'RUNNING':
      return '●'
    default:
      return '?'
  }
}

// Register network event handler
registerNetworkEvent('behavior_tree_trace', handleBehaviorTreeTrace)
