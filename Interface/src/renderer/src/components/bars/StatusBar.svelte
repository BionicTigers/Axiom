<script lang="ts">
  import { onMount } from 'svelte'
  import DownloadIcon from '~icons/material-symbols/download-rounded'
  import InstallIcon from '~icons/material-symbols/install-desktop-rounded'
  import RefreshIcon from '~icons/material-symbols/refresh-rounded'

  let { isConnected, latency }: { isConnected: boolean; latency: number } = $props()
  let status = $derived(latency < 10 ? 'stable' : 'unstable')
  let version = $state<string>('')
  
  type ConnectionMethod = 'usb' | 'wifi' | 'unknown'
  let connectionMethod = $state<ConnectionMethod>('unknown')

  // Update state
  type UpdateState = 'none' | 'available' | 'downloading' | 'ready'
  let updateState = $state<UpdateState>('none')
  let updateVersion = $state<string>('')
  let downloadProgress = $state<number>(0)

  // Load version from main via preload
  window.axiomAPI
    .getVersion()
    .then((v) => (version = v))
    .catch(() => (version = ''))

  onMount(() => {
    const offAvailable = window.axiomAPI.onUpdateAvailable((info) => {
      updateState = 'available'
      updateVersion = info.version
    })

    const offProgress = window.axiomAPI.onUpdateProgress((progress) => {
      updateState = 'downloading'
      downloadProgress = progress.percent
    })

    const offDownloaded = window.axiomAPI.onUpdateDownloaded((info) => {
      updateState = 'ready'
      updateVersion = info.version
    })

    const offError = window.axiomAPI.onUpdateError(() => {
      updateState = 'none'
    })

    // ADB connection method tracking
    const offAdbSuccess = window.axiomAPI.onAdbForwardingSuccess(() => {
      connectionMethod = 'usb'
    })

    const offAdbNotAvailable = window.axiomAPI.onAdbNotAvailable(() => {
      connectionMethod = 'wifi'
    })

    const offAdbNoDevice = window.axiomAPI.onAdbNoDevice(() => {
      connectionMethod = 'wifi'
    })

    return () => {
      offAvailable()
      offProgress()
      offDownloaded()
      offError()
      offAdbSuccess()
      offAdbNotAvailable()
      offAdbNoDevice()
    }
  })

  function handleUpdateClick() {
    if (updateState === 'available') {
      updateState = 'downloading'
      window.axiomAPI.downloadUpdate()
    } else if (updateState === 'ready') {
      window.axiomAPI.installUpdate()
    }
  }
</script>

<ul>
  {#if isConnected}
    <li>
      Axiom - <span class="axiom-status {status}">{status}</span>
      <span class="latency">({latency}ms)</span>
      {#if connectionMethod !== 'unknown'}
        <span class="connection-method {connectionMethod}" title="{connectionMethod === 'usb' ? 'Connected via USB' : 'Connected via WiFi'}">
          {connectionMethod === 'usb' ? '🔌' : '📡'}
        </span>
      {/if}
    </li>
  {:else}
    <li>
      Axiom - <span class="not-connected axiom-status">not connected</span>
      {#if connectionMethod !== 'unknown'}
        <span class="connection-method-hint">({connectionMethod === 'usb' ? 'USB ready' : 'WiFi mode'})</span>
        {#if connectionMethod === 'wifi'}
          <button
            class="refresh-btn"
            onclick={() => window.axiomAPI.refreshAdb()}
            title="Check for USB device"
          >
            <RefreshIcon />
          </button>
        {/if}
      {/if}
    </li>
  {/if}
  <li class="version-item">
    Seek {version}
    {#if updateState === 'available'}
      <button
        class="update-btn"
        onclick={handleUpdateClick}
        title="Update to {updateVersion} available - click to download"
      >
        <DownloadIcon />
      </button>
    {:else if updateState === 'downloading'}
      <span class="update-progress" title="Downloading update...">
        {Math.round(downloadProgress)}%
      </span>
    {:else if updateState === 'ready'}
      <button
        class="update-btn ready"
        onclick={handleUpdateClick}
        title="Update {updateVersion} ready - click to install and restart"
      >
        <InstallIcon />
      </button>
    {/if}
  </li>
</ul>

<style>
  ul {
    position: absolute;
    bottom: 30px;
    margin: 0 auto;
    padding: 15px 0;
    font-family: 'Menlo', 'Lucida Console', monospace;
    display: inline-flex;
    overflow: hidden;
    align-items: center;
    border-radius: 22px;
    background-color: #202127;
    backdrop-filter: blur(24px);
    z-index: 1000;
  }

  ul li {
    display: block;
    float: left;
    border-right: 1px solid var(--ev-c-gray-1);
    padding: 0 20px;
    font-size: 14px;
    line-height: 14px;
    opacity: 0.8;
    &:last-child {
      border: none;
    }
  }

  .axiom-status {
    text-transform: uppercase;
  }

  .stable {
    color: var(--ev-c-green);
  }

  .unstable {
    color: var(--ev-c-yellow);
  }

  .latency {
    color: var(--ev-c-gray-1);
    font-size: 12px;
  }

  .not-connected {
    color: var(--ev-c-red);
  }

  .connection-method {
    margin-left: 8px;
    font-size: 12px;
    opacity: 0.8;
  }

  .connection-method.usb {
    filter: hue-rotate(120deg);
  }

  .connection-method-hint {
    color: var(--ev-c-gray-1);
    font-size: 11px;
    margin-left: 4px;
  }

  .refresh-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: none;
    border: none;
    padding: 2px;
    margin-left: 4px;
    cursor: pointer;
    color: var(--ev-c-gray-1);
    font-size: 14px;
    transition: color 0.15s, transform 0.15s;
    vertical-align: middle;
  }

  .refresh-btn:hover {
    color: var(--ev-c-cyan, #56d4dd);
    transform: rotate(90deg);
  }

  .version-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .update-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    background: none;
    border: none;
    padding: 2px;
    cursor: pointer;
    color: var(--ev-c-cyan, #56d4dd);
    font-size: 16px;
    transition:
      transform 0.15s ease,
      color 0.15s ease;
    border-radius: 4px;
  }

  .update-btn:hover {
    transform: scale(1.15);
    color: #7ae8f0;
  }

  .update-btn.ready {
    color: var(--ev-c-green, #4ade80);
    animation: pulse 1.5s ease-in-out infinite;
  }

  .update-btn.ready:hover {
    color: #6ee7a0;
  }

  .update-progress {
    font-size: 11px;
    color: var(--ev-c-cyan, #56d4dd);
    min-width: 32px;
    text-align: center;
  }

  @keyframes pulse {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.6;
    }
  }
</style>
