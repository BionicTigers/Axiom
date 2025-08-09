import { writable } from 'svelte/store'
import type { UUID } from '../types'
import { registerNetworkEvent } from '../networkRegistry'

type CommandStateValue = string | number | boolean | null
type CommandState = CommandStateValue | CommandState[] | { [key: string]: CommandState } 

type SchedulableType = 'Command' | 'System'

type SchedulableInitial = [
  {
    name: string,
    id: UUID,
    type: SchedulableType,
    parent: UUID | null,
    state: Map<string, CommandState>
  }
]

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

type SchedulableStateUpdate = [
  {
    id: UUID,
    field: string,
    value: CommandState
  }
]

export type Schedulable = {
  name: string
  type: SchedulableType
  parent: UUID | null
  state: Map<string, CommandState>
}

export function updateSchedulablesFromInitial(data: SchedulableInitial) {
  const schedulables = new Map<UUID, Schedulable>()

  data.forEach(({ id, name, type, parent, state }) => {
    schedulables.set(id, {
      name,
      type,
      parent,
      state: new Map(state)
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
        state: new Map(existingSchedulable?.state ?? new Map())
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

export const schedulableStore = writable<Map<UUID, Schedulable>>(new Map());

registerNetworkEvent('schedulable_initial', updateSchedulablesFromInitial)
registerNetworkEvent('schedulable_update', updateSchedulables)
registerNetworkEvent('schedulable_state_update', updateStates)