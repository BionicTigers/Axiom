import { get, writable, derived, type Writable } from 'svelte/store'

import { registerNetworkEvent } from '../networkRegistry'
import type { UUID } from '../types'
import { displayedSnapshot, isPaused, type SchedulableSnapshot } from './historyStore'

// Types now sourced from ../types

type SchedulableType = 'Command' | 'System'

type SchedulableInitial = Array<{
  name: string
  id: UUID
  type: SchedulableType
  parent: UUID | null
  // JSON payloads carry plain objects, not Maps
  state: Record<string, CommandState>
}>

export type StateMetadata = {
  readonly: boolean
  priority: number
  hidden: boolean
}

export const defaultStateMetadata: StateMetadata = {
  readonly: true,
  priority: 0,
  hidden: false
}

export type CommandStateValueBase = string | number | boolean | null
export type CommandStateValue = { value: CommandStateValueBase; metadata: StateMetadata }
export type CommandState = CommandStateValue | CommandState[] | { [key: string]: CommandState }

type SchedulableUpdate = {
  updated: {
    [id: UUID]: {
      name: string
      type: SchedulableType
      parent: UUID | null
    }
  }
  removed: UUID[]
}

type SchedulableStateUpdate = Array<{
  id: UUID
  field: string
  value: CommandState
}>

type SchedulableOrder = UUID[]

export class StateMap extends Map<string, CommandState> {
  constructor(initial: Map<string, CommandState> | Record<string, CommandState>) {
    if (initial instanceof Map) {
      super(initial)
    } else {
      super(Object.entries(initial))
    }
  }

  getMetadata(field: string): StateMetadata {
    const raw = (this.get(field) as CommandStateValue | undefined)?.metadata as
      | Partial<StateMetadata>
      | undefined
    return { ...defaultStateMetadata, ...(raw ?? {}) }
  }

  getValue(field: string): CommandStateValueBase {
    return (this.get(field) as CommandStateValue | undefined)?.value ?? null
  }

  getState(field: string): [CommandStateValueBase, StateMetadata] {
    const value = this.get(field) as CommandStateValue | undefined
    const metadata = (value?.metadata as Partial<StateMetadata> | undefined) ?? {}
    return [value?.value ?? null, { ...defaultStateMetadata, ...metadata }]
  }
}

export type Schedulable = {
  name: string
  type: SchedulableType
  parent: UUID | null
  state: Writable<StateMap>
}

export function updateSchedulablesFromInitial(data: SchedulableInitial) {
  const schedulables = new Map<UUID, Schedulable>()

  data.forEach(({ id, name, type, parent, state }) => {
    schedulables.set(id, {
      name,
      type,
      parent,
      state: writable(new StateMap(state))
    })
  })

  schedulableStore.set(new Map(schedulables))
}

export function updateSchedulables({ updated, removed }: SchedulableUpdate) {
  schedulableStore.update((schedulables) => {
    const next = new Map(schedulables)
    removed.forEach((id) => {
      next.delete(id)
    })

    Object.entries(updated).forEach(([id, schedulable]) => {
      const existingSchedulable = next.get(id)
      next.set(id, {
        name: schedulable.name,
        type: schedulable.type,
        parent: schedulable.parent,
        state: writable(new StateMap(get(existingSchedulable?.state) ?? new Map()))
      })
    })

    return next
  })
}

export function updateStates(data: SchedulableStateUpdate) {
  schedulableStore.update((next) => {
    data.forEach(({ id, field, value }) => {
      const schedulable = next.get(id)
      if (schedulable) {
        schedulable.state.update((prev) => {
          const next = new StateMap(prev)
          next.set(field, value)
          return next
        })
      }
    })

    return next
  })
}

export function setOrder(data: SchedulableOrder) {
  schedulableOrderStore.set(data)
}

export function editState(path: string, value: CommandStateValueBase) {
  console.log('editState', path, value)
  window.axiomAPI.send(
    JSON.stringify({
      type: 'edit',
      path: path,
      value: value
    })
  )
}

// ============================================================================
// Live Stores (current real-time data)
// ============================================================================

export const schedulableStore = writable<Map<UUID, Schedulable>>(new Map())
export const schedulableOrderStore = writable<Array<UUID>>([])

// ============================================================================
// Displayed Stores (historical or live based on pause state)
// ============================================================================

// Convert a SchedulableSnapshot to a Schedulable
function snapshotToSchedulable(snapshot: SchedulableSnapshot): Schedulable {
  return {
    name: snapshot.name,
    type: snapshot.type,
    parent: snapshot.parent,
    state: writable(new StateMap(snapshot.state))
  }
}

// Displayed schedulables - shows historical data when paused, live data otherwise
export const displayedSchedulableStore = derived(
  [displayedSnapshot, schedulableStore, isPaused],
  ([$snapshot, $live, $isPaused]) => {
    // Only use snapshot when actually paused
    if ($isPaused && $snapshot) {
      // Convert snapshot schedulables to the expected format
      const displayed = new Map<UUID, Schedulable>()
      for (const [id, snap] of $snapshot.schedulables) {
        displayed.set(id, snapshotToSchedulable(snap))
      }
      return displayed
    }
    return $live
  }
)

// Displayed order - shows historical data when paused, live data otherwise
export const displayedSchedulableOrderStore = derived(
  [displayedSnapshot, schedulableOrderStore, isPaused],
  ([$snapshot, $live, $isPaused]) => {
    // Only use snapshot when actually paused
    if ($isPaused && $snapshot) {
      return $snapshot.schedulableOrder
    }
    return $live
  }
)

// ============================================================================
// Snapshot Creation Helper
// ============================================================================

/**
 * Create a deep copy snapshot of current schedulable state
 */
export function createSchedulablesSnapshot(): Map<UUID, SchedulableSnapshot> {
  const current = get(schedulableStore)
  const snapshot = new Map<UUID, SchedulableSnapshot>()

  for (const [id, schedulable] of current) {
    const stateMap = get(schedulable.state)
    const stateCopy: Record<string, CommandState> = {}

    for (const [key, value] of stateMap) {
      // Deep copy the state value
      stateCopy[key] = JSON.parse(JSON.stringify(value))
    }

    snapshot.set(id, {
      name: schedulable.name,
      type: schedulable.type,
      parent: schedulable.parent,
      state: stateCopy
    })
  }

  return snapshot
}

// ============================================================================
// Network Event Registration
// ============================================================================

registerNetworkEvent('schedulable_initial', updateSchedulablesFromInitial)
registerNetworkEvent('schedulable_update', updateSchedulables)
registerNetworkEvent('schedulable_state_update', updateStates)
registerNetworkEvent('schedulable_order', setOrder)
