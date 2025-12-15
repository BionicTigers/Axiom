<script lang="ts">
  import { update } from '../../../lib/stores/windows'
  import { schedulableStore } from '../../../lib/stores/schedulableStore'
  import {
    windowSeconds,
    selectedSeries,
    graphData,
    seriesMeta,
    paused,
    toggleSeries,
    isSeriesSelected,
    clearAllSeries,
    setWindowSeconds,
    togglePause,
    getNumericFields,
    seriesKey
  } from '../../../lib/stores/graphStore'
  import UPlotChart from '../../graph/UPlotChart.svelte'

  let { id }: { id: string } = $props()
  update(id, { minW: 500, minH: 300 })

  let search = $state('')
  let localWindowSeconds = $state($windowSeconds)

  // Group schedulables with their numeric fields
  let groupedFields = $derived(
    Array.from($schedulableStore)
      .map(([schedId, schedulable]) => ({
        id: schedId,
        name: schedulable.name,
        type: schedulable.type,
        fields: getNumericFields(schedulable)
      }))
      .filter((g) => g.fields.length > 0)
      .filter((g) => {
        if (search === '') return true
        const searchLower = search.toLowerCase()
        if (g.name.toLowerCase().includes(searchLower)) return true
        return g.fields.some((f) => f.toLowerCase().includes(searchLower))
      })
  )

  function handleWindowSecondsChange() {
    const val = Math.max(1, Math.min(120, localWindowSeconds))
    localWindowSeconds = val
    setWindowSeconds(val)
  }

  function handleClear() {
    clearAllSeries()
  }

  function handleToggle(schedulableId: string, field: string) {
    toggleSeries({ schedulableId, field })
  }

  function isChecked(schedulableId: string, field: string): boolean {
    return isSeriesSelected({ schedulableId, field })
  }

  // Get series color for legend
  function getSeriesColor(schedulableId: string, field: string): string {
    const key = seriesKey({ schedulableId, field })
    const meta = $seriesMeta.find((m) => m.key === key)
    return meta?.color ?? '#ffffff'
  }
</script>

<div class="graph-container">
  <div class="header">
    <div class="window-control">
      <label for="window-seconds">Window:</label>
      <input
        id="window-seconds"
        type="number"
        min="1"
        max="120"
        bind:value={localWindowSeconds}
        onchange={handleWindowSecondsChange}
      />
      <span>s</span>
    </div>
    <div class="header-buttons">
      <button class="pause-btn" class:paused={$paused} onclick={togglePause}>
        {$paused ? '▶ Resume' : '⏸ Pause'}
      </button>
      <button class="clear-btn" onclick={handleClear}>Clear All</button>
    </div>
  </div>

  <div class="main">
    <div class="sidebar">
      <div class="search-box">
        <input type="text" placeholder="Search fields..." bind:value={search} />
      </div>

      <div class="field-list">
        {#each groupedFields as group (group.id)}
          <div class="group">
            <div class="group-header">
              <span class="group-name">{group.name}</span>
              <span class="group-type">{group.type}</span>
            </div>
            <ul class="fields">
              {#each group.fields as field (field)}
                {@const checked = isChecked(group.id, field)}
                <li class:selected={checked}>
                  <label>
                    <input
                      type="checkbox"
                      {checked}
                      onchange={() => handleToggle(group.id, field)}
                    />
                    {#if checked}
                      <span
                        class="color-dot"
                        style="background-color: {getSeriesColor(group.id, field)}"
                      ></span>
                    {/if}
                    <span class="field-name">{field}</span>
                  </label>
                </li>
              {/each}
            </ul>
          </div>
        {/each}

        {#if groupedFields.length === 0}
          <div class="empty">
            {#if search}
              No matching fields found.
            {:else}
              No numeric fields available.
            {/if}
          </div>
        {/if}
      </div>
    </div>

    <div class="chart-panel">
      {#if $selectedSeries.length === 0}
        <div class="chart-empty">
          <p>Select fields to graph</p>
        </div>
      {:else}
        <UPlotChart data={$graphData} series={$seriesMeta} windowSeconds={$windowSeconds} />
      {/if}
    </div>
  </div>

  {#if $selectedSeries.length > 0}
    <div class="legend">
      {#each $seriesMeta as meta (meta.key)}
        <div class="legend-item">
          <span class="legend-color" style="background-color: {meta.color}"></span>
          <span class="legend-label">{meta.label}</span>
        </div>
      {/each}
    </div>
  {/if}
</div>

<style>
  .graph-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 10px;
  }

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 10px;
    border-bottom: 1px solid rgb(100, 100, 100);
  }

  .window-control {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .window-control label {
    color: rgb(200, 200, 200);
    font-size: 0.9rem;
  }

  .window-control input {
    width: 60px;
    padding: 4px 8px;
    border-radius: 6px;
    border: 1px solid rgb(43, 52, 70);
    background-color: rgb(33, 42, 60);
    color: rgb(230, 233, 239);
    font-size: 0.9rem;
  }

  .window-control span {
    color: rgb(200, 200, 200);
    font-size: 0.9rem;
  }

  .header-buttons {
    display: flex;
    gap: 8px;
  }

  .pause-btn,
  .clear-btn {
    padding: 6px 12px;
    border-radius: 6px;
    border: 1px solid rgb(43, 52, 70);
    background-color: rgb(33, 42, 60);
    color: rgb(230, 233, 239);
    cursor: pointer;
    font-size: 0.85rem;
    transition: background-color 0.15s;
  }

  .pause-btn:hover,
  .clear-btn:hover {
    background-color: rgb(43, 52, 70);
  }

  .pause-btn.paused {
    background-color: rgba(77, 171, 247, 0.2);
    border-color: rgba(77, 171, 247, 0.4);
  }

  .main {
    display: flex;
    flex: 1;
    gap: 10px;
    min-height: 0;
  }

  .sidebar {
    width: 200px;
    min-width: 150px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .search-box input {
    width: 100%;
    padding: 8px 10px;
    border-radius: 8px;
    border: 1px solid rgb(43, 52, 70);
    background-color: rgb(33, 42, 60);
    color: rgb(230, 233, 239);
    font-size: 0.85rem;
  }

  .field-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .group-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0;
  }

  .group-name {
    font-weight: 600;
    font-size: 0.9rem;
    color: rgb(230, 233, 239);
  }

  .group-type {
    font-size: 0.7rem;
    color: rgb(150, 150, 150);
    background-color: rgb(43, 52, 70);
    padding: 2px 6px;
    border-radius: 4px;
    margin-left: auto;
  }

  .fields {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .fields li {
    padding: 4px 8px;
    border-radius: 6px;
    transition: background-color 0.1s;
  }

  .fields li:hover {
    background-color: rgb(33, 42, 60);
  }

  .fields li.selected {
    background-color: rgba(77, 171, 247, 0.15);
  }

  .fields label {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    font-size: 0.85rem;
    color: rgb(200, 200, 200);
  }

  .fields input[type='checkbox'] {
    cursor: pointer;
  }

  .color-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .field-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .empty {
    color: rgb(150, 150, 150);
    font-size: 0.85rem;
    padding: 10px;
    text-align: center;
  }

  .chart-panel {
    flex: 1;
    min-width: 0;
    background-color: rgb(25, 30, 40);
    border-radius: 8px;
    border: 1px solid rgb(43, 52, 70);
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .chart-empty {
    color: rgb(150, 150, 150);
    font-size: 0.9rem;
  }

  .legend {
    display: flex;
    flex-wrap: nowrap;
    gap: 12px;
    padding-top: 8px;
    border-top: 1px solid rgb(100, 100, 100);
    overflow-x: auto;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
  }

  .legend-color {
    width: 12px;
    height: 3px;
    border-radius: 2px;
  }

  .legend-label {
    font-size: 0.8rem;
    color: rgb(200, 200, 200);
  }
</style>
