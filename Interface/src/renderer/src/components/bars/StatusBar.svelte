<script lang="ts">
  import { onMount } from 'svelte'
  import DownloadIcon from '~icons/material-symbols/download-rounded'
  import InstallIcon from '~icons/material-symbols/install-desktop-rounded'
  import RefreshIcon from '~icons/material-symbols/refresh-rounded'
  import USBIcon from '~icons/material-symbols/usb-rounded'
  import WIFIIcon from '~icons/material-symbols/wifi-rounded'
  import PlayIcon from '~icons/material-symbols/play-arrow-rounded'
  import PauseIcon from '~icons/material-symbols/pause-rounded'
  import SkipPrevIcon from '~icons/material-symbols/skip-previous-rounded'
  import SkipNextIcon from '~icons/material-symbols/skip-next-rounded'
  import FirstPageIcon from '~icons/material-symbols/first-page-rounded'
  import LastPageIcon from '~icons/material-symbols/last-page-rounded'
  import {
    isPaused,
    viewingIndex,
    historyBounds,
    displayInfo,
    togglePause,
    stepBackward,
    stepForward,
    jumpToStart,
    jumpToEnd,
    setViewingIndex,
    resumeLive
  } from '../../lib/stores/historyStore'

  let { isConnected, latency }: { isConnected: boolean; latency: number } = $props()
  let status = $derived(latency < 10 ? 'stable' : 'unstable')
  let version = $state<string>('')

  type ConnectionMethod = 'usb' | 'wifi' | 'unknown'
  let connectionMethod = $state<ConnectionMethod>('unknown')

  // Update state
  type UpdateState = 'none' | 'available' | 'downloading' | 'ready' | 'error'
  let updateState = $state<UpdateState>('none')
  let updateVersion = $state<string>('')
  let downloadProgress = $state<number>(0)
  let updateError = $state<string>('')

  // Timeline slider
  let sliderValue = $derived($viewingIndex ?? $historyBounds.max)

  function handleSliderInput(e: Event) {
    const target = e.target as HTMLInputElement
    setViewingIndex(parseInt(target.value, 10))
  }

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

    const offError = window.axiomAPI.onUpdateError((error) => {
      console.error('[StatusBar] Update error:', error.message)
      updateError = error.message
      updateState = 'error'
      // Reset to none after 5 seconds so user can retry
      setTimeout(() => {
        if (updateState === 'error') {
          updateState = 'none'
        }
      }, 5000)
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
        <span
          class="connection-method {connectionMethod}"
          title={connectionMethod === 'usb' ? 'Connected via USB' : 'Connected via WiFi'}
        >
          {#if connectionMethod === 'usb'}
            <USBIcon />
          {:else}
            <WIFIIcon />
          {/if}
        </span>
      {/if}
    </li>
    <!-- Timeline Controls -->
    <li class="timeline-controls">
      <button
        class="timeline-btn"
        onclick={jumpToStart}
        disabled={$historyBounds.max === 0}
        title="Jump to start"
      >
        <FirstPageIcon />
      </button>
      <button
        class="timeline-btn"
        onclick={stepBackward}
        disabled={$historyBounds.max === 0}
        title="Step backward"
      >
        <SkipPrevIcon />
      </button>
      <button
        class="timeline-btn play-pause"
        class:paused={$isPaused}
        onclick={togglePause}
        title={$isPaused ? 'Resume' : 'Pause'}
      >
        {#if $isPaused}
          <PlayIcon />
        {:else}
          <PauseIcon />
        {/if}
      </button>
      <button
        class="timeline-btn"
        onclick={stepForward}
        disabled={!$isPaused || $viewingIndex === null || $viewingIndex >= $historyBounds.max}
        title="Step forward"
      >
        <SkipNextIcon />
      </button>
      <button
        class="timeline-btn"
        onclick={jumpToEnd}
        disabled={$historyBounds.max === 0}
        title="Jump to end"
      >
        <LastPageIcon />
      </button>
      <input
        type="range"
        class="timeline-slider"
        min="0"
        max={$historyBounds.max}
        value={sliderValue}
        oninput={handleSliderInput}
        disabled={$historyBounds.max === 0}
        title="Tick {$displayInfo.tick}"
      />
      <span
        class="tick-display"
        class:paused={$isPaused}
        class:historical={$displayInfo.isHistorical}
      >
        {#if $isPaused}
          {#if $displayInfo.isHistorical}
            T{$displayInfo.tick}
          {:else}
            ⏸ T{$displayInfo.tick}
          {/if}
        {:else}
          ● T{$displayInfo.tick}
        {/if}
      </span>
      {#if $isPaused}
        <button class="live-btn" onclick={resumeLive} title="Resume live view"> LIVE </button>
      {/if}
    </li>
  {:else}
    <li>
      Axiom - <span class="not-connected axiom-status">not connected</span>
      {#if connectionMethod !== 'unknown'}
        <span class="connection-method-hint"
          >({connectionMethod === 'usb' ? 'USB ready' : 'WiFi mode'})</span
        >
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
    {:else if updateState === 'error'}
      <span class="update-error" title={updateError}> Update failed </span>
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
    transition:
      color 0.15s,
      transform 0.15s;
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

  .update-error {
    font-size: 11px;
    color: var(--ev-c-red, #f87171);
    cursor: help;
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

  /* Timeline Controls */
  .timeline-controls {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .timeline-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.05);
    border: none;
    border-radius: 4px;
    padding: 4px;
    cursor: pointer;
    color: var(--ev-c-gray-1);
    font-size: 16px;
    transition: all 0.15s;
  }

  .timeline-btn:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.12);
    color: #fff;
  }

  .timeline-btn:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }

  .timeline-btn.play-pause {
    background: rgba(86, 212, 221, 0.15);
    color: var(--ev-c-cyan, #56d4dd);
    padding: 4px 6px;
  }

  .timeline-btn.play-pause:hover {
    background: rgba(86, 212, 221, 0.25);
  }

  .timeline-btn.play-pause.paused {
    background: rgba(74, 222, 128, 0.15);
    color: var(--ev-c-green, #4ade80);
  }

  .timeline-btn.play-pause.paused:hover {
    background: rgba(74, 222, 128, 0.25);
  }

  .timeline-slider {
    width: 80px;
    height: 4px;
    border-radius: 2px;
    background: rgba(255, 255, 255, 0.1);
    outline: none;
    cursor: pointer;
    -webkit-appearance: none;
    margin: 0 4px;
  }

  .timeline-slider::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: var(--ev-c-cyan, #56d4dd);
    border: none;
    cursor: pointer;
    transition: transform 0.1s;
  }

  .timeline-slider::-webkit-slider-thumb:hover {
    transform: scale(1.2);
  }

  .timeline-slider:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }

  .tick-display {
    font-size: 11px;
    color: var(--ev-c-cyan, #56d4dd);
    min-width: 50px;
    text-align: center;
    font-weight: 500;
  }

  .tick-display.paused {
    color: var(--ev-c-yellow, #fbbf24);
  }

  .tick-display.historical {
    color: var(--ev-c-orange, #fb923c);
  }

  .live-btn {
    background: rgba(248, 113, 113, 0.2);
    border: none;
    border-radius: 4px;
    padding: 3px 8px;
    cursor: pointer;
    color: var(--ev-c-red, #f87171);
    font-size: 10px;
    font-weight: 600;
    transition: all 0.15s;
    animation: pulse 1.5s ease-in-out infinite;
  }

  .live-btn:hover {
    background: rgba(248, 113, 113, 0.3);
  }
</style>
