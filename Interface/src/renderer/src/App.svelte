<script lang="ts">
  import Apps from './components/Apps.svelte'
  import Status from './components/Status.svelte'
  import AnimatedSearch from './components/AnimatedSearch.svelte'
  import { onMount } from 'svelte'

  let isConnected = $state(false)

  onMount(() => {
    isConnected = window.axiomAPI.isConnected()

    window.electron.ipcRenderer.on('axiom-connected', () => {
      console.log('Renderer: Axiom connected')
      isConnected = true
    })

    window.electron.ipcRenderer.on('axiom-disconnected', () => {
      console.log('Renderer: Axiom disconnected')
      isConnected = false
    })
  })

  let latency = $state(1)

  // const ipcHandle = (): void => window.electron.ipcRenderer.send('ping')
</script>

<AnimatedSearch isSearching={!isConnected} />

<Apps />
<Status {isConnected} {latency} />

<style>
  /* App styles can go here if needed */
</style>
