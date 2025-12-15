<script lang="ts">
  import Apps from './components/bars/AppBar.svelte'
  import Status from './components/bars/StatusBar.svelte'
  import AnimatedSearch from './components/AnimatedSearch.svelte'
  import { onMount } from 'svelte'
  import type { BaseResponse } from './lib/types'
  import { getNetworkEvent } from './lib/networkRegistry'
  import './lib/stores/schedulableStore'
  import './lib/stores/schedulerDetails'
  import './lib/stores/graphStore'
  import WindowManager from './components/windows/WindowManager.svelte'
  import NotificationContainer from './components/notifications/NotificationContainer.svelte'
  import { schedulableOrderStore, schedulableStore } from './lib/stores/schedulableStore'
  import { clearAllSeries } from './lib/stores/graphStore'

  let isConnected = $state(false)

  onMount(() => {
    isConnected = window.axiomAPI.isConnected()

    const offConnected = window.axiomAPI.onConnected(() => {
      isConnected = true
    })

    const offData = window.axiomAPI.onData((jsonData: BaseResponse) => {
      const { name, tick, data } = jsonData
      const callback = getNetworkEvent(name)
      if (callback) {
        callback(data, tick)
      } else {
        console.warn(`Renderer: No callback registered for event ${name}`)
      }
    })

    const offDisconnected = window.axiomAPI.onDisconnected(() => {
      schedulableOrderStore.set([])
      schedulableStore.set(new Map())
      clearAllSeries()
      isConnected = false
    })

    try {
      window.electron.ipcRenderer.send('renderer-ready')
    } catch {
      console.error('Failed to send renderer-ready signal')
    }

    return () => {
      offConnected()
      offData()
      offDisconnected()
    }
  })

  let latency = $state(1)
</script>

<Apps disabled={!isConnected} />
<Status {isConnected} {latency} />

<WindowManager />

<NotificationContainer />

<AnimatedSearch isSearching={!isConnected} />
