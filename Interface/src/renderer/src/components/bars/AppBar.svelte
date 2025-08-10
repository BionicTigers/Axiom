<script lang="ts">
  import CommandIcon from '~icons/material-symbols/keyboard-command-key'
  import GamepadIcon from '~icons/material-symbols/gamepad-rounded'
  import MapIcon from '~icons/material-symbols/map'
  import EyeIcon from '~icons/solar/eye-bold'
  import AppList from './helpers/AppList.svelte'
  import { onMount } from 'svelte'

  let openKey = $state<string | null>(null)
  let barElement: HTMLElement | null = null

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

  const commandOptions = [{ label: 'Command 1' }, { label: 'Command 2' }, { label: 'Command 3' }]

  const gamepadOptions = [{ label: 'Gamepad 1' }, { label: 'Gamepad 2' }, { label: 'Gamepad 3' }]

  const hardwareOptions = [
    { label: 'Hardware 1' },
    { label: 'Hardware 2' },
    { label: 'Hardware 3' }
  ]

  const viewerOptions = [{ label: 'Graph' }, { label: 'Table' }, { label: 'Chart' }]
</script>

<ul bind:this={barElement}>
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
  }

  ul li {
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
</style>
