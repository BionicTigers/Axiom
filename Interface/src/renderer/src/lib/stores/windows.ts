import { get, writable } from 'svelte/store'
import type { Component } from 'svelte'

export type Win = {
  id: string
  title: string
  x: number
  y: number
  w: number
  h: number
  z: number
  component: Component<{ win: Win }>
  minW?: number
  minH?: number
  maxW?: number
  maxH?: number
  resizable?: boolean
  movable?: boolean
}

export const windows = writable<Win[]>([])
export let zCounter = 1

export function add(win: Partial<Win>) {
  console.assert(win.component, 'component is required')

  // Determine min dimensions first
  const minW = win.minW ?? 160
  const minH = win.minH ?? 120

  // Ensure initial size respects minimums
  const defaultW = 320
  const defaultH = 200
  const initialW = win.w ?? Math.max(defaultW, minW)
  const initialH = win.h ?? Math.max(defaultH, minH)

  const w: Win = {
    ...win,
    id: win.id ?? win.title ?? 'Untitled',
    title: win.title ?? 'Untitled',
    x: win.x ?? 40,
    y: win.y ?? 40,
    w: initialW,
    h: initialH,
    z: zCounter++,
    component: win.component!,
    minW,
    minH,
    resizable: win.resizable ?? true,
    movable: win.movable ?? true
  }

  windows.update((windows) => {
    const alreadyOpen = windows.some((existing) => existing.title === w.title)
    return alreadyOpen ? windows : [...windows, w]
  })
  return w.title
}

export function bringToFront(title: string) {
  const w = get(windows).find((w) => w.title === title)
  if (!w) return
  w.z = ++zCounter
}

export function update(id: string, patch: Partial<Win>) {
  const w = get(windows).find((w) => w.id === id)
  if (!w) return

  // Apply the patch
  Object.assign(w, patch)

  // Ensure current size respects new min/max constraints
  if (patch.minW !== undefined && w.w < patch.minW) {
    w.w = patch.minW
  }
  if (patch.minH !== undefined && w.h < patch.minH) {
    w.h = patch.minH
  }
  if (patch.maxW !== undefined && w.w > patch.maxW) {
    w.w = patch.maxW
  }
  if (patch.maxH !== undefined && w.h > patch.maxH) {
    w.h = patch.maxH
  }

  // Trigger store update for reactivity
  windows.update((wins) => [...wins])
}

export function remove(title: string) {
  const i = get(windows).findIndex((w) => w.title === title)
  if (i >= 0) windows.update((windows) => windows.filter((w) => w.title !== title))
}
