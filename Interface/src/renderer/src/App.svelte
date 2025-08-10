<script lang="ts">
  import Apps from './components/bars/AppBar.svelte'
  import Status from './components/bars/StatusBar.svelte'
  import AnimatedSearch from './components/AnimatedSearch.svelte'
  import { onMount } from 'svelte'
  import type { BaseResponse } from './lib/types'
  import { getNetworkEvent } from './lib/networkRegistry'
  import './lib/stores/schedulableStore'
  import WindowManager from './components/windows/WindowManager.svelte'

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
        callback(data, tick)
      } else {
        console.warn(`Renderer: No callback registered for event ${name}`)
      }
    })

    window.electron.ipcRenderer.on('axiom-disconnected', () => {
      isConnected = false
    })
  })

  let latency = $state(1)
</script>

<AnimatedSearch isSearching={!isConnected} />

<WindowManager />

<Apps />
<Status {isConnected} {latency} />

<style>
  /* App styles can go here if needed */
</style>
