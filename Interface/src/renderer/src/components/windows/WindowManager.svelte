<script lang="ts">
  import Window from './Window.svelte'
  import { windows, add, bringToFront, remove } from '../../lib/stores/windows'

  // demo
  $effect(() => {
    if ($windows.length === 0) {
      add({ title: 'Notes', x: 48, y: 48, w: 380, h: 240 })
      add({ title: 'Logs', x: 220, y: 160, w: 420, h: 260 })
    }
  })

  let containerElement = $state<HTMLDivElement | null>(null)
  let containerRect = $state<DOMRect | null>(null)

  $effect(() => {
    if (!containerElement) return () => {}
    const ro = new ResizeObserver(([e]) => {
      containerRect = e.contentRect as unknown as DOMRect
    })
    ro.observe(containerElement)
    return () => ro.disconnect()
  })
</script>

<div class="canvas" bind:this={containerElement}>
  {#each $windows as w (w.id)}
    <Window
      bind:x={w.x}
      bind:y={w.y}
      bind:w={w.w}
      bind:h={w.h}
      bind:z={w.z}
      id={w.id}
      title={w.title}
      minW={w.minW}
      minH={w.minH}
      maxW={w.maxW}
      maxH={w.maxH}
      resizable={w.resizable}
      movable={w.movable}
      {containerRect}
      onFocus={() => bringToFront(w.id)}
      onClose={() => remove(w.id)}
    />
  {/each}
</div>

<style>
  .canvas {
    position: relative;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
  }
</style>
