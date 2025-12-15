<script lang="ts">
  import { onMount, onDestroy } from 'svelte'
  import uPlot from 'uplot'
  import 'uplot/dist/uPlot.min.css'

  type SeriesMeta = {
    label: string
    color: string
  }

  let {
    data = [[]],
    series = [] as SeriesMeta[],
    windowSeconds: _windowSeconds = 10
  }: {
    data: (number | null)[][]
    series: SeriesMeta[]
    windowSeconds: number
  } = $props()

  let containerEl: HTMLDivElement
  let plot: uPlot | null = null

  // Theme colors matching the app
  const GRID_COLOR = 'rgba(100, 100, 100, 0.3)'
  const AXIS_COLOR = 'rgb(150, 150, 150)'

  function buildOptions(width: number, height: number): uPlot.Options {
    const seriesOpts: uPlot.Series[] = [
      {
        label: 'Time'
      },
      ...series.map((s) => ({
        label: s.label,
        stroke: s.color,
        width: 2,
        spanGaps: false
      }))
    ]

    return {
      width,
      height,
      series: seriesOpts,
      scales: {
        x: {
          time: false
        }
      },
      axes: [
        {
          stroke: AXIS_COLOR,
          grid: { stroke: GRID_COLOR },
          ticks: { stroke: GRID_COLOR },
          font: '11px sans-serif',
          labelFont: '12px sans-serif'
        },
        {
          stroke: AXIS_COLOR,
          grid: { stroke: GRID_COLOR },
          ticks: { stroke: GRID_COLOR },
          font: '11px sans-serif',
          labelFont: '12px sans-serif'
        }
      ],
      cursor: {
        show: true,
        drag: { x: false, y: false }
      },
      legend: {
        show: false
      }
    }
  }

  function createPlot(): void {
    if (!containerEl) return
    destroyPlot()

    const rect = containerEl.getBoundingClientRect()
    const width = Math.max(100, rect.width)
    const height = Math.max(100, rect.height)

    const opts = buildOptions(width, height)
    plot = new uPlot(opts, data as uPlot.AlignedData, containerEl)
  }

  function destroyPlot(): void {
    if (plot) {
      plot.destroy()
      plot = null
    }
  }

  function handleResize(): void {
    if (!containerEl || !plot) return
    const rect = containerEl.getBoundingClientRect()
    const width = Math.max(100, rect.width)
    const height = Math.max(100, rect.height)
    plot.setSize({ width, height })
  }

  let resizeObserver: ResizeObserver | null = null

  onMount(() => {
    createPlot()

    resizeObserver = new ResizeObserver(() => {
      handleResize()
    })
    resizeObserver.observe(containerEl)
  })

  onDestroy(() => {
    resizeObserver?.disconnect()
    destroyPlot()
  })

  // Track series config to detect changes
  let lastSeriesKey = ''

  // Effect for series config changes - recreate plot
  $effect(() => {
    const newKey = series.map((s) => `${s.label}:${s.color}`).join('|')
    if (newKey !== lastSeriesKey) {
      lastSeriesKey = newKey
      if (containerEl) {
        createPlot()
      }
    }
  })

  // Effect for data updates - just update data
  $effect(() => {
    // Track the data array and its first element (x values length)
    const xLen = data[0]?.length ?? 0
    void xLen // ensure tracking

    if (plot && data.length > 0 && xLen > 0) {
      plot.setData(data as uPlot.AlignedData)
    }
  })
</script>

<div class="chart-container" bind:this={containerEl}></div>

<style>
  .chart-container {
    width: 100%;
    height: 100%;
    min-height: 150px;
  }

  .chart-container :global(.u-wrap) {
    background: transparent !important;
  }

  .chart-container :global(.u-over) {
    cursor: crosshair;
  }
</style>
