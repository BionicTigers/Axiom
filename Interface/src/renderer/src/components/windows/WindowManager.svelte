<script lang="ts">
  import Window from './Window.svelte'
  import { windows, bringToFront, remove } from '../../lib/stores/windows'

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
      id={w.id}
      bind:x={w.x}
      bind:y={w.y}
      bind:w={w.w}
      bind:h={w.h}
      bind:z={w.z}
      title={w.title}
      Renderer={w.component}
      minW={w.minW}
      minH={w.minH}
      maxW={w.maxW}
      maxH={w.maxH}
      resizable={w.resizable}
      movable={w.movable}
      {containerRect}
      onFocus={() => bringToFront(w.title)}
      onClose={() => remove(w.title)}
    />
  {/each}
</div>

<style>
  .canvas {
    position: relative;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
    z-index: 0;
  }
</style>
