<script lang="ts">
  import MaterialSymbolsSearchRounded from '~icons/material-symbols/search-rounded'

  let { isSearching = true } = $props<{ isSearching?: boolean }>()

  let position = $state({ x: 0, y: 0 })
  let animationId: number | null = null
  let startTime: number | null = null

  const scale = 80
  const speed = 0.0015
  const rotationAmount = 20

  let rotation = $state(0)

  function animate(timestamp: number): void {
    if (!startTime) startTime = timestamp

    const elapsed = timestamp - startTime
    const t = elapsed * speed

    const sinT = Math.sin(t)
    const cosT = Math.cos(t)
    const denominator = 1 + sinT * sinT

    position = {
      x: (scale * cosT) / denominator,
      y: (scale * sinT * cosT) / denominator
    }

    rotation = Math.sin(t * 0.7) * rotationAmount + 45

    if (isSearching) {
      animationId = requestAnimationFrame(animate)
    }
  }

  $effect(() => {
    if (isSearching) {
      startTime = null
      animationId = requestAnimationFrame(animate)
    } else if (animationId) {
      cancelAnimationFrame(animationId)
      animationId = null
    }

    return () => {
      if (animationId) {
        cancelAnimationFrame(animationId)
      }
    }
  })
</script>

{#if isSearching}
  <div class="search-container">
    <MaterialSymbolsSearchRounded
      class="search-icon"
      style="transform: translate({position.x}px, {position.y}px) rotate({rotation}deg)"
    />
    <p class="search-text">Searching for <strong>Axiom</strong></p>
    <!-- <p class="hint">Ensure <strong>Axiom</strong> is installed</p> -->
    <!-- <p class="hint">Not connected to <strong>Network</strong></p> -->
  </div>
{/if}

<style>
  .search-container {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .search-text {
    font-size: 1.5rem;
    font-weight: 600;
    color: var(--ev-c-black-2-soft);
    text-align: center;
  }

  .hint {
    font-size: 1rem;
    color: var(--ev-c-red);
    text-align: center;
  }

  .hint strong {
    color: var(--ev-c-red-soft);
  }

  strong {
    color: var(--ev-c-black-2-mute);
    font-weight: 800;
  }

  :global(.search-icon) {
    width: 100px;
    height: 100px;
    margin-bottom: 40px;
    color: var(--ev-c-black-2-soft);
    transition: transform 0.1s ease-out;
  }
</style>
