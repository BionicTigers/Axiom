<script lang="ts">
  import Apps from './components/bars/AppBar.svelte'
  import Status from './components/bars/StatusBar.svelte'
  import AnimatedSearch from './components/AnimatedSearch.svelte'
  import { onMount } from 'svelte'
  import type { BaseResponse } from './lib/types'
  import { getNetworkEvent } from './lib/networkRegistry'
  import './lib/stores/schedulableStore'
  import WindowManager from './components/windows/WindowManager.svelte'
  import { schedulableOrderStore, schedulableStore } from './lib/stores/schedulableStore'

  let isConnected = $state(false)

  onMount(() => {
    isConnected = window.axiomAPI.isConnected()

    window.electron.ipcRenderer.on('axiom-connected', () => {
      isConnected = true
    })

    window.electron.ipcRenderer.on('axiom-data', (_, jsonData: BaseResponse) => {
      const { name, tick, data } = jsonData
      const callback = getNetworkEvent(name)
      if (callback) {
        console.log("handled axiom-data", name)
        callback(data, tick)
      } else {
        console.warn(`Renderer: No callback registered for event ${name}`)
      }
    })

    window.electron.ipcRenderer.on('axiom-disconnected', () => {
      schedulableOrderStore.set([])
      schedulableStore.set(new Map())
      isConnected = false
      console.log("Clearing stores")
    })

    // Signal main once all listeners are registered
    try {
      window.electron.ipcRenderer.send('renderer-ready')
    } catch {}

    return () => {
      window.electron.ipcRenderer.removeAllListeners('axiom-data')
      window.electron.ipcRenderer.removeAllListeners('axiom-connected')
      window.electron.ipcRenderer.removeAllListeners('axiom-disconnected')
    }
  })

  let latency = $state(1)
</script>


<Apps disabled={!isConnected} />
<Status {isConnected} {latency} />

<WindowManager />

<AnimatedSearch isSearching={!isConnected} />

<style>
  /* App styles can go here if needed */
</style>
