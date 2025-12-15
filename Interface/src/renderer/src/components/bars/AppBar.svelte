<script lang="ts">
  import CommandIcon from '~icons/material-symbols/format-list-bulleted'
  import RobotIcon from '~icons/material-symbols/robot-2-rounded'
  import GamepadIcon from '~icons/material-symbols/gamepad-rounded'
  import MapIcon from '~icons/material-symbols/map'
  import EyeIcon from '~icons/solar/eye-bold'
  import AppList from './helpers/AppList.svelte'
  import { onMount } from 'svelte'
  import * as registry from '../windows/registry/index'

  let openKey = $state<string | null>(null)
  let barElement: HTMLElement | null = null

  let { disabled = $bindable(false) } = $props<{ disabled: boolean }>()

  onMount(() => {
    function handleDocClick(event: MouseEvent): void {
      const target = event.target as Node | null
      if (!barElement || !target) return
      if (!barElement.contains(target)) {
        openKey = null
      }
    }
    document.addEventListener('mousedown', handleDocClick, true)
    return () => document.removeEventListener('mousedown', handleDocClick, true)
  })

  const commandOptions = [
    { label: 'States', component: registry.States },
    { label: 'Scheduler', component: registry.Scheduler }
  ]

  const gamepadOptions = [{ label: 'Gamepad Overview', component: registry.GamepadViewer }]

  const hardwareOptions = [
    { label: 'Hardware Viewer', component: registry.HardwareViewer },
    { label: 'Config', component: registry.Config }
  ]

  const robotOptions = [
    { label: 'Control', component: registry.Control },
    { label: 'Telemetry', component: registry.Telemetry }
  ]

  const viewerOptions = [{ label: 'Graph', component: registry.Graph }]
</script>

<ul bind:this={barElement} class:disabled>
  <li>
    <AppList
      title="Command"
      icon={CommandIcon}
      options={commandOptions}
      isOpen={openKey === 'command'}
      onToggle={() => (openKey = openKey === 'command' ? null : 'command')}
    />
  </li>
  <li>
    <AppList
      title="Robot"
      icon={RobotIcon}
      options={robotOptions}
      isOpen={openKey === 'robot'}
      onToggle={() => (openKey = openKey === 'robot' ? null : 'robot')}
    />
  </li>
  <li>
    <AppList
      title="Gamepad"
      icon={GamepadIcon}
      options={gamepadOptions}
      isOpen={openKey === 'gamepad'}
      onToggle={() => (openKey = openKey === 'gamepad' ? null : 'gamepad')}
    />
  </li>
  <li>
    <AppList
      title="Hardware"
      icon={MapIcon}
      options={hardwareOptions}
      isOpen={openKey === 'hardware'}
      onToggle={() => (openKey = openKey === 'hardware' ? null : 'hardware')}
    />
  </li>
  <li>
    <AppList
      title="Chart"
      icon={EyeIcon}
      options={viewerOptions}
      isOpen={openKey === 'chart'}
      onToggle={() => (openKey = openKey === 'chart' ? null : 'chart')}
    />
  </li>
</ul>

<style>
  ul {
    position: absolute;
    top: 30px;
    margin: 0 auto;
    padding: 15px 3px;
    font-family: 'Menlo', 'Lucida Console', monospace;
    display: inline-flex;
    overflow: visible;
    align-items: center;
    border-radius: 22px;
    background-color: #202127;
    color: #e6e6e6;
    backdrop-filter: blur(24px);
    z-index: 1000;
  }

  li {
    display: block;
    float: left;
    border-right: 1px solid var(--ev-c-gray-1);
    padding: 0 20px;
    font-size: 14px;
    line-height: 14px;
    opacity: 0.8;
    position: relative;
    &:last-child {
      border: none;
    }
  }

  :global(.disabled) {
    opacity: 0.5;
    pointer-events: none;
    cursor: not-allowed;
  }
</style>
