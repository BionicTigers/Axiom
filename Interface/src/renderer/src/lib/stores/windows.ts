import { get, writable } from "svelte/store";

export type Win = {
  id: string
  title: string
  x: number
  y: number
  w: number
  h: number
  z: number
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
  const w: Win = {
    id: crypto.randomUUID(),
    title: win.title ?? 'Untitled',
    x: win.x ?? 40,
    y: win.y ?? 40,
    w: win.w ?? 320,
    h: win.h ?? 200,
    z: zCounter++,
    minW: 160,
    minH: 120,
    resizable: true,
    movable: true,
    ...win
  }
  windows.update((windows) => [...windows, w])
  return w.id
}

export function bringToFront(id: string) {
  const w = get(windows).find((w) => w.id === id)
  if (!w) return
  w.z = ++zCounter
}

export function update(id: string, patch: Partial<Win>) {
  const w = get(windows).find((w) => w.id === id)
  if (!w) return
  Object.assign(w, patch)
}

export function remove(id: string) {
  const i = get(windows).findIndex((w) => w.id === id)
  if (i >= 0) windows.update((windows) => windows.filter((w) => w.id !== id))
}
