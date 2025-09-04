<script lang="ts">
  import UpArrowIcon from '~icons/material-symbols/arrow-drop-up-rounded'
  import DownArrowIcon from '~icons/material-symbols/arrow-drop-down-rounded'
  import { onMount } from 'svelte'
  import type { Component } from 'svelte'
  import type { Win } from '../../lib/stores/windows'

  let {
    id = crypto.randomUUID(),
    title = 'Untitled',
    x = $bindable(),
    y = $bindable(),
    w = $bindable(),
    h = $bindable(),
    z = $bindable(),
    minW = 160,
    minH = 120,
    maxW = Infinity,
    maxH = Infinity,
    resizable = true,
    movable = true,
    Renderer,
    containerRect,
    onFocus,
    onClose,
  } = $props<{
    id?: string
    title?: string
    x: number
    y: number
    w: number
    h: number
    z: number
    minW?: number
    minH?: number
    maxW?: number
    maxH?: number
    resizable?: boolean
    movable?: boolean
    Renderer: Component<{ win: Win }>
    containerRect: DOMRect | null
    onFocus?: () => void
    onClose?: () => void
  }>()

  // internal drag/resize state
  let dragging = $state(false)
  let resizing = $state<null | { edge: string }>(null)

  let startX = 0,
    startY = 0,
    startW = 0,
    startH = 0,
    startXpos = 0,
    startYpos = 0

  const GRID = 1 // set to 8/10 for snap
  const snap = (v: number): number => (GRID > 1 ? Math.round(v / GRID) * GRID : v)
  const clamp = (n: number, a: number, b: number): number => Math.max(a, Math.min(b, n))

  const HANDLE_SPECS: ReadonlyArray<
    readonly [edge: 'n' | 'e' | 's' | 'w' | 'ne' | 'se' | 'sw' | 'nw', cursor: string]
  > = [
    ['n', 'ns-resize'],
    ['e', 'ew-resize'],
    ['s', 'ns-resize'],
    ['w', 'ew-resize'],
    ['ne', 'nesw-resize'],
    ['se', 'nwse-resize'],
    ['sw', 'nesw-resize'],
    ['nw', 'nwse-resize']
  ]

  function focus(): void {
    onFocus?.()
  }

  // Windows-like drag: start when movement exceeds a small threshold
  const DRAG_SLOP_PX = 4
  let pendingDrag = false
  let lastPointerTarget: HTMLElement | null = null
  let lastPointerId = 0

  let minimized = $state(false)

  function beginDrag(): void {
    if (!pendingDrag || !lastPointerTarget) return
    pendingDrag = false
    try {
      lastPointerTarget.setPointerCapture(lastPointerId)
    } catch {
      // ignore
    }
    dragging = true
  }

  function onTitlePointerDown(e: PointerEvent): void {
    if (!movable) return
    if (e.button !== 0) return // primary button only
    focus()
    pendingDrag = true
    lastPointerTarget = e.currentTarget as HTMLElement
    lastPointerId = e.pointerId
    startX = e.clientX
    startY = e.clientY
    startXpos = x
    startYpos = y
  }

  function onResizeStart(edge: string, e: PointerEvent): void {
    if (!resizable) return
    if (e.button !== 0) return
    focus()
    ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
    resizing = { edge }
    startX = e.clientX
    startY = e.clientY
    startW = w
    startH = h
    startXpos = x
    startYpos = y
  }

  // Throttle heavy move math to animation frames
  let rafId: number | null = null
  let queuedEvent: PointerEvent | null = null

  function scheduleUpdate(e: PointerEvent): void {
    queuedEvent = e
    if (rafId != null) return
    rafId = requestAnimationFrame(() => {
      rafId = null
      if (queuedEvent) {
        performMove(queuedEvent)
        queuedEvent = null
      }
    })
  }

  function performMove(e: PointerEvent): void {
    if (!containerRect) return

    // Promote to dragging if moved beyond slop while pending
    if (pendingDrag && !dragging) {
      const dx0 = Math.abs(e.clientX - startX)
      const dy0 = Math.abs(e.clientY - startY)
      if (dx0 >= DRAG_SLOP_PX || dy0 >= DRAG_SLOP_PX) {
        beginDrag()
      }
    }

    if (dragging) {
      updatePosition(e)
    }

    if (resizing) {
      updateResize(e)
    }
  }

  function updatePosition(e: PointerEvent): void {
    if (!containerRect) return
    const dx = e.clientX - startX
    const dy = e.clientY - startY
    const maxX = Math.max(0, containerRect.width - w)
    const maxY = Math.max(0, containerRect.height - h)
    x = snap(clamp(startXpos + dx, 0, maxX))
    y = snap(clamp(startYpos + dy, 0, maxY))
  }

  function updateResize(e: PointerEvent): void {
    if (!containerRect || !resizing) return
    const edge = resizing.edge
    let nx = x,
      ny = y,
      nw = w,
      nh = h
    if (/e/.test(edge)) {
      nw = clamp(startW + (e.clientX - startX), minW, Math.min(maxW, containerRect.width - x))
    }
    if (/s/.test(edge)) {
      nh = clamp(startH + (e.clientY - startY), minH, Math.min(maxH, containerRect.height - y))
    }
    if (/w/.test(edge)) {
      const newW = clamp(startW - (e.clientX - startX), minW, maxW)
      const delta = newW - startW
      nx = clamp(startXpos - delta, 0, startXpos + startW)
      nw = Math.min(newW, containerRect.width - nx)
    }
    if (/n/.test(edge)) {
      const newH = clamp(startH - (e.clientY - startY), minH, maxH)
      const delta = newH - startH
      ny = clamp(startYpos - delta, 0, startYpos + startH)
      nh = Math.min(newH, containerRect.height - ny)
    }
    x = snap(nx)
    y = snap(ny)
    w = snap(nw)
    h = snap(nh)
  }

  function onPointerMove(e: PointerEvent): void {
    scheduleUpdate(e)
  }

  function onPointerUp(e: PointerEvent): void {
    onGlobalPointerUp(e)
  }

  function close(): void {
    onClose?.()
  }

  function onGlobalPointerMove(e: PointerEvent): void {
    if (!(pendingDrag || dragging || resizing)) return
    // Require primary button to stay held during drag/resize
    if ((e.buttons & 1) === 0) {
      onGlobalPointerUp(e)
      return
    }
    scheduleUpdate(e)
  }

  function onGlobalPointerUp(_: PointerEvent): void {
    pendingDrag = false
    try {
      lastPointerTarget?.releasePointerCapture(lastPointerId)
    } catch {
      // ignore
    }
    dragging = false
    resizing = null
  }

  onMount(() => {
    const move = (e: PointerEvent) => onGlobalPointerMove(e)
    const up = (e: PointerEvent) => onGlobalPointerUp(e)
    window.addEventListener('pointermove', move, true)
    window.addEventListener('pointerup', up, true)
    window.addEventListener('pointercancel', up, true)
    window.addEventListener('blur', up, true)
    return () => {
      window.removeEventListener('pointermove', move, true)
      window.removeEventListener('pointerup', up, true)
      window.removeEventListener('pointercancel', up, true)
      window.removeEventListener('blur', up, true)
    }
  })
</script>

<div
  class="window"
  data-id={title}
  style={`transform: translate3d(${x}px, ${y}px, 0); width:${w}px; height:${h}px; z-index:${z};`}
  aria-grabbed={dragging}
  data-dragging={dragging}
  data-resizing={!!resizing}
  onpointerdown={focus}
>
  <div
    class="titlebar"
    onpointerdown={onTitlePointerDown}
    style={`border-bottom: ${!minimized ? 'none' : '2px solid #242e40'};`}
  >
    <button class="minimize" onclick={() => (minimized = !minimized)} aria-label="Minimize">
      {#if minimized}
        <DownArrowIcon />
      {:else}
        <UpArrowIcon />
      {/if}
    </button>
    <span class="title">{title}</span>
    <button class="close" onclick={close} aria-label="Close">×</button>
  </div>

  {#if !minimized}
    <div class="content">
      <Renderer id={id} />
    </div>
  {/if}

  {#if resizable && !minimized}
    {#each HANDLE_SPECS as [edge, cursor] (edge)}
      <div
        class={`handle handle-${edge}`}
        style={`cursor:${cursor}`}
        onpointerdown={(e) => onResizeStart(edge, e)}
        onpointermove={onPointerMove}
        onpointerup={onPointerUp}
      ></div>
    {/each}
  {/if}

  <div
    class="overlay"
    aria-hidden="true"
    hidden={!dragging && !resizing}
    onpointermove={onPointerMove}
    onpointerup={onPointerUp}
  ></div>
</div>

<style>
  .window {
    position: absolute;
    will-change: transform;
    color: #e6e9ef;
    border-radius: 4px;
    /* box-shadow: 0 12px 24px rgba(0, 0, 0, 0.4); */
    user-select: none;
  }
  .titlebar {
    height: 36px;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0 0.25rem;
    border-radius: 4px 4px 0 0;
    background: #1c202e;
    border: 2px solid #242e40;
    border-bottom: none;
    z-index: 1;
  }

  .title {
    font-weight: 600;
    flex: 1;
  }
  .minimize {
    appearance: none;
    border: none;
    background: transparent;
    color: #93a1b3;
    font-size: 18px;
    border-radius: 8px;
    width: 28px;
    height: 28px;
    display: grid;
    place-items: center;
    cursor: pointer;
  }
  .close {
    appearance: none;
    border: none;
    background: transparent;
    color: #93a1b3;
    font-size: 18px;
    border-radius: 8px;
    width: 28px;
    height: 28px;
    display: grid;
    place-items: center;
  }
  .close:hover {
    background: rgba(255, 255, 255, 0.06);
    color: #c6cfdd;
  }
  .content {
    padding: 0.75rem;
    padding-bottom: 0;
    height: calc(100% - 35px);
    overflow: auto;
    background: #12141a;
    border: 2px solid #242e40;
    border-top: none;
  }

  .handle {
    position: absolute;
  }
  .handle-n {
    top: -4px;
    left: 8px;
    right: 8px;
    height: 8px;
  }
  .handle-s {
    bottom: -4px;
    left: 8px;
    right: 8px;
    height: 8px;
  }
  .handle-e {
    right: -4px;
    top: 8px;
    bottom: 8px;
    width: 8px;
  }
  .handle-w {
    left: -4px;
    top: 8px;
    bottom: 8px;
    width: 8px;
  }
  .handle-ne {
    right: -4px;
    top: -4px;
    width: 10px;
    height: 10px;
  }
  .handle-se {
    right: -4px;
    bottom: -4px;
    width: 10px;
    height: 10px;
  }
  .handle-sw {
    left: -4px;
    bottom: -4px;
    width: 10px;
    height: 10px;
  }
  .handle-nw {
    left: -4px;
    top: -4px;
    width: 10px;
    height: 10px;
  }

  .overlay {
    position: absolute;
    inset: 0;
  }
</style>
