export const networkRegistry: { [key: string]: (data: any, tick: number) => void } = {}

export function registerNetworkEvent(name: string, callback: (data: any, tick: number) => void) {
  networkRegistry[name] = callback
}

export function unregisterNetworkEvent(name: string) {
  delete networkRegistry[name]
}

export function getNetworkEvent(name: string) {
  return networkRegistry[name]
}