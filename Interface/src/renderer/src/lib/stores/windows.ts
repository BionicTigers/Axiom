import type { Component } from "svelte";
import { get, writable } from "svelte/store";

export type Win = {
  id: string
  title: string
  x: number
  y: number
  w: number
  h: number
  z: number
  component: Component<{win: Win}>
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

  const w: Win = {
    id: win.id ?? win.title,
    title: win.title ?? 'Untitled',
    x: win.x ?? 40,
    y: win.y ?? 40,
    w: win.w ?? 320,
    h: win.h ?? 200,
    z: zCounter++,
    component: win.component,
    minW: 160,
    minH: 120,
    resizable: true,
    movable: true,
    ...win
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
  Object.assign(w, patch)
}

export function remove(title: string) {
  const i = get(windows).findIndex((w) => w.title === title)
  if (i >= 0) windows.update((windows) => windows.filter((w) => w.title !== title))
}
