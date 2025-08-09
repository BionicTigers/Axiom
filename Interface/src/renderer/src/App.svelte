<script lang="ts">
  import Apps from './components/Apps.svelte'
  import Status from './components/Status.svelte'
  import AnimatedSearch from './components/AnimatedSearch.svelte'
  import { onMount } from 'svelte'
  import type { BaseResponse } from './lib/types'
  import { getNetworkEvent } from './lib/networkRegistry'
  import { schedulableStore } from './lib/stores/schedulableStore'

  let isConnected = $state(false)

  onMount(() => {
    isConnected = window.axiomAPI.isConnected()

    window.electron.ipcRenderer.on('axiom-connected', () => {
      console.log('Renderer: Axiom connected')
      isConnected = true
    })

    window.electron.ipcRenderer.on('axiom-data', (_, jsonData: BaseResponse<any>) => {
      const { name, tick, data } = jsonData
      const callback = getNetworkEvent(name)
      console.log('Renderer: Axiom event', name)
      if (callback) {
        callback(data, tick)
      } else {
        console.warn(`Renderer: No callback registered for event ${name}`)
      }
    })

    window.electron.ipcRenderer.on('axiom-disconnected', () => {
      console.log('Renderer: Axiom disconnected')
      isConnected = false
    })
  })

  let latency = $state(1)
</script>

<AnimatedSearch isSearching={!isConnected} />

<Apps />
<Status {isConnected} {latency} />

<style>
  /* App styles can go here if needed */
</style>
