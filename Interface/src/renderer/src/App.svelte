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
  import { addNotification, NotificationType } from './lib/stores/notificationStore'

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

    // ADB status notifications - only show problems, not successes
    const offAdbNotAvailable = window.axiomAPI.onAdbNotAvailable(() => {
      addNotification(
        {
          title: 'ADB Not Available',
          message:
            'Android Debug Bridge (ADB) is not installed or not in PATH. USB connections will not work. Install ADB to enable USB connectivity.',
          type: NotificationType.WARNING,
          isModal: false
        },
        0
      )
    })

    const offAdbNoDevice = window.axiomAPI.onAdbNoDevice(() => {
      // Silent - just show in status bar
    })

    const offAdbSuccess = window.axiomAPI.onAdbForwardingSuccess(() => {
      // Silent - just show in status bar
    })

    const offAdbRetryFailed = window.axiomAPI.onAdbRetryFailed(() => {
      addNotification(
        {
          title: 'No USB Device Found',
          message:
            'Still no USB device detected. Make sure your device is connected via USB and USB debugging is enabled.',
          type: NotificationType.WARNING,
          isModal: false
        },
        0
      )
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
      offAdbNotAvailable()
      offAdbNoDevice()
      offAdbSuccess()
      offAdbRetryFailed()
    }
  })

  let latency = $state(1)
</script>

<Apps disabled={!isConnected} />
<Status {isConnected} {latency} />

<WindowManager />

<NotificationContainer />

<AnimatedSearch isSearching={!isConnected} />
