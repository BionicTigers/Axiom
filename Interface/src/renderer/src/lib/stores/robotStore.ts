import { writable } from 'svelte/store'

export const robotOpMode = writable<{
  name: string | undefined
  inInit: boolean
  inRun: boolean
  isAuto: boolean
}>({
  name: undefined,
  inInit: false,
  inRun: false,
  isAuto: false
})

export const robotTelemetry = writable<string>()

export const robotState = writable<{
  voltage: number
}>()
