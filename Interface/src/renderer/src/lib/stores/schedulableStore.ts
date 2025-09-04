import { writable } from 'svelte/store'
import type { UUID } from '../types'
import { registerNetworkEvent } from '../networkRegistry'

type CommandStateValueBase = string | number | boolean | null
type CommandStateValue = { value: CommandStateValueBase; readonly: boolean }
type CommandState = CommandStateValue | CommandState[] | { [key: string]: CommandState }

type SchedulableType = 'Command' | 'System'

type SchedulableInitial = Array<{
  name: string
  id: UUID
  type: SchedulableType
  parent: UUID | null
  // JSON payloads carry plain objects, not Maps
  state: Record<string, CommandState>
}>

type SchedulableUpdate = {
    updated: {
      [id: UUID]: {
        name: string,
        type: SchedulableType,
        parent: UUID | null,
      }
    },
    removed: UUID[]
}

type SchedulableStateUpdate = Array<{
  id: UUID
  field: string
  value: CommandState
}>

type SchedulableOrder = UUID[]

class StateMap extends Map<string, CommandState> {
  constructor(initial: Map<string, CommandState> | Record<string, CommandState>) {
    if (initial instanceof Map) {
      super(initial)
    } else {
      super(Object.entries(initial))
    }
  }

  getValue(field: string): CommandStateValueBase {
    return (this.get(field) as CommandStateValue | undefined)?.value ?? null
  }
}

export type Schedulable = {
  name: string
  type: SchedulableType
  parent: UUID | null
  state: StateMap
}

export function updateSchedulablesFromInitial(data: SchedulableInitial) {
  const schedulables = new Map<UUID, Schedulable>()

  data.forEach(({ id, name, type, parent, state }) => {
    schedulables.set(id, {
      name,
      type,
      parent,
      state: new StateMap(state)
    })
  })

  schedulableStore.set(new Map(schedulables))
}

export function updateSchedulables({ updated, removed }: SchedulableUpdate) {
  schedulableStore.update((schedulables) => {
    removed.forEach((id) => {
      schedulables.delete(id)
    })

    Object.entries(updated).forEach(([id, schedulable]) => {
      const existingSchedulable = schedulables.get(id)
      schedulables.set(id, {
        name: schedulable.name,
        type: schedulable.type,
        parent: schedulable.parent,
        state: new StateMap(existingSchedulable?.state ?? new Map())
      })
    })

    return new Map(schedulables)
  })
}

export function updateStates(data: SchedulableStateUpdate) {
  schedulableStore.update((schedulables) => {
    data.forEach(({ id, field, value }) => {
      const schedulable = schedulables.get(id)
      if (schedulable) {
        schedulable.state.set(field, value)
      }
    })
    return new Map(schedulables)
  })
}

export function setOrder(data: SchedulableOrder) {
  schedulableOrderStore.set(data)
}

export const schedulableStore = writable<Map<UUID, Schedulable>>(new Map());
export const schedulableOrderStore = writable<Array<UUID>>(new Array());

registerNetworkEvent('schedulable_initial', updateSchedulablesFromInitial)
registerNetworkEvent('schedulable_update', updateSchedulables)
registerNetworkEvent('schedulable_state_update', updateStates)
registerNetworkEvent('schedulable_order', setOrder)