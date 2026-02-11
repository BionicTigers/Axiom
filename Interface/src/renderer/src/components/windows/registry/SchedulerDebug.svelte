<script lang="ts">
  import { displayedSchedulerDebug } from '../../../lib/stores/schedulerDebugStore'
  import { update } from '../../../lib/stores/windows'

  let { id }: { id: string } = $props()
  update(id, { maxW: 420, minW: 340, maxH: 600, minH: 400 })

  // Format milliseconds with appropriate precision
  function formatMs(ms: number): string {
    if (ms < 0.01) return '<0.01'
    if (ms < 1) return ms.toFixed(2)
    if (ms < 10) return ms.toFixed(1)
    return ms.toFixed(0)
  }

  // Calculate percentage of total
  function calcPercent(value: number, total: number): number {
    if (total === 0) return 0
    return (value / total) * 100
  }

  function formatCount(count: number): string {
    if (count < 10) return count.toFixed(1)
    return count.toFixed(0)
  }

  $effect(() => {
    // Force reactivity
    $displayedSchedulerDebug
  })
</script>

<div class="debug-container">
  <div class="summary-section">
    <div class="summary-card">
      <span class="summary-label">Total (avg)</span>
      <span class="summary-value">{formatMs($displayedSchedulerDebug.average.total)}ms</span>
    </div>
    <div class="summary-card commands">
      <span class="summary-label">Commands</span>
      <span class="summary-value">{formatMs($displayedSchedulerDebug.average.commands)}ms</span>
    </div>
    <div class="summary-card overhead">
      <span class="summary-label">Axiom Overhead</span>
      <span class="summary-value">{formatMs($displayedSchedulerDebug.average.axiomOverhead)}ms</span>
    </div>
  </div>

  <div class="info-row">
    <span class="info-label">Command Count</span>
    <span class="info-value">{$displayedSchedulerDebug.commandCount}</span>
  </div>
  <div class="info-row">
    <span class="info-label">Serialized States (cur/avg)</span>
    <span class="info-value">
      {formatCount($displayedSchedulerDebug.current.serializedStates)} /
      {formatCount($displayedSchedulerDebug.average.serializedStates)}
    </span>
  </div>
  <div class="info-row">
    <span class="info-label">Serialized Fields (cur/avg)</span>
    <span class="info-value">
      {formatCount($displayedSchedulerDebug.current.serializedFields)} /
      {formatCount($displayedSchedulerDebug.average.serializedFields)}
    </span>
  </div>

  <div class="breakdown-section">
    <div class="section-header">
      <span class="section-title">Command Time</span>
    </div>
    <div class="breakdown-item">
      <div class="breakdown-header">
        <span class="breakdown-label">User Commands</span>
        <span class="breakdown-values">
          <span class="current">{formatMs($displayedSchedulerDebug.current.commands)}ms</span>
          <span class="separator">/</span>
          <span class="average">{formatMs($displayedSchedulerDebug.average.commands)}ms avg</span>
        </span>
      </div>
      <div class="progress-bar">
        <div
          class="progress-fill commands"
          style="width: {calcPercent($displayedSchedulerDebug.average.commands, $displayedSchedulerDebug.average.total)}%"
        ></div>
      </div>
    </div>
  </div>

  <div class="breakdown-section">
    <div class="section-header">
      <span class="section-title">Axiom Overhead Breakdown</span>
    </div>

    <div class="breakdown-item">
      <div class="breakdown-header">
        <span class="breakdown-label">Queue Processing</span>
        <span class="breakdown-values">
          <span class="current">{formatMs($displayedSchedulerDebug.current.queueProcessing)}ms</span>
          <span class="separator">/</span>
          <span class="average">{formatMs($displayedSchedulerDebug.average.queueProcessing)}ms avg</span>
        </span>
      </div>
      <div class="progress-bar">
        <div
          class="progress-fill queue"
          style="width: {calcPercent($displayedSchedulerDebug.average.queueProcessing, $displayedSchedulerDebug.average.total)}%"
        ></div>
      </div>
    </div>

    <div class="breakdown-item">
      <div class="breakdown-header">
        <span class="breakdown-label">Dependency Sort</span>
        <span class="breakdown-values">
          <span class="current">{formatMs($displayedSchedulerDebug.current.dependencySort)}ms</span>
          <span class="separator">/</span>
          <span class="average">{formatMs($displayedSchedulerDebug.average.dependencySort)}ms avg</span>
        </span>
      </div>
      <div class="progress-bar">
        <div
          class="progress-fill sort"
          style="width: {calcPercent($displayedSchedulerDebug.average.dependencySort, $displayedSchedulerDebug.average.total)}%"
        ></div>
      </div>
    </div>

    <div class="breakdown-item">
      <div class="breakdown-header">
        <span class="breakdown-label">Serialization</span>
        <span class="breakdown-values">
          <span class="current">{formatMs($displayedSchedulerDebug.current.serialization)}ms</span>
          <span class="separator">/</span>
          <span class="average">{formatMs($displayedSchedulerDebug.average.serialization)}ms avg</span>
        </span>
      </div>
      <div class="progress-bar">
        <div
          class="progress-fill serialization"
          style="width: {calcPercent($displayedSchedulerDebug.average.serialization, $displayedSchedulerDebug.average.total)}%"
        ></div>
      </div>
    </div>

    <div class="breakdown-item">
      <div class="breakdown-header">
        <span class="breakdown-label">Delta Resolution</span>
        <span class="breakdown-values">
          <span class="current">{formatMs($displayedSchedulerDebug.current.deltaResolution)}ms</span>
          <span class="separator">/</span>
          <span class="average">{formatMs($displayedSchedulerDebug.average.deltaResolution)}ms avg</span>
        </span>
      </div>
      <div class="progress-bar">
        <div
          class="progress-fill delta"
          style="width: {calcPercent($displayedSchedulerDebug.average.deltaResolution, $displayedSchedulerDebug.average.total)}%"
        ></div>
      </div>
    </div>

    <div class="breakdown-item">
      <div class="breakdown-header">
        <span class="breakdown-label">Network Send</span>
        <span class="breakdown-values">
          <span class="current">{formatMs($displayedSchedulerDebug.current.networkSend)}ms</span>
          <span class="separator">/</span>
          <span class="average">{formatMs($displayedSchedulerDebug.average.networkSend)}ms avg</span>
        </span>
      </div>
      <div class="progress-bar">
        <div
          class="progress-fill network"
          style="width: {calcPercent($displayedSchedulerDebug.average.networkSend, $displayedSchedulerDebug.average.total)}%"
        ></div>
      </div>
    </div>

    <div class="breakdown-item">
      <div class="breakdown-header">
        <span class="breakdown-label">Connection Callbacks</span>
        <span class="breakdown-values">
          <span class="current">{formatMs($displayedSchedulerDebug.current.connectionCallbacks)}ms</span>
          <span class="separator">/</span>
          <span class="average">{formatMs($displayedSchedulerDebug.average.connectionCallbacks)}ms avg</span>
        </span>
      </div>
      <div class="progress-bar">
        <div
          class="progress-fill connection"
          style="width: {calcPercent($displayedSchedulerDebug.average.connectionCallbacks, $displayedSchedulerDebug.average.total)}%"
        ></div>
      </div>
    </div>
  </div>

  <div class="totals-section">
    <div class="total-row">
      <span class="total-label">Current Total</span>
      <span class="total-value">{formatMs($displayedSchedulerDebug.current.total)}ms</span>
    </div>
    <div class="total-row highlight">
      <span class="total-label">Average Total (50 ticks)</span>
      <span class="total-value">{formatMs($displayedSchedulerDebug.average.total)}ms</span>
    </div>
  </div>
</div>

<style>
  .debug-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 16px;
    overflow-y: auto;
  }

  .summary-section {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .summary-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 12px 8px;
    background-color: rgb(33, 42, 60);
    border-radius: 8px;
    border: 1px solid rgb(43, 52, 70);
  }

  .summary-card.commands {
    background-color: rgb(30, 50, 45);
    border-color: rgb(40, 70, 60);
  }

  .summary-card.overhead {
    background-color: rgb(50, 40, 35);
    border-color: rgb(70, 55, 45);
  }

  .summary-label {
    font-size: 0.7rem;
    color: rgb(150, 160, 180);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    text-align: center;
  }

  .summary-value {
    font-size: 1.1rem;
    font-weight: 600;
    color: rgb(230, 233, 239);
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 12px;
    background-color: rgb(28, 35, 50);
    border-radius: 6px;
  }

  .info-label {
    font-size: 0.8rem;
    color: rgb(150, 160, 180);
  }

  .info-value {
    font-size: 0.85rem;
    font-weight: 500;
    color: rgb(200, 210, 220);
  }

  .breakdown-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .section-header {
    padding-bottom: 4px;
    border-bottom: 1px solid rgb(50, 60, 80);
  }

  .section-title {
    font-size: 0.85rem;
    font-weight: 600;
    color: rgb(180, 190, 210);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .breakdown-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .breakdown-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .breakdown-label {
    font-size: 0.8rem;
    color: rgb(180, 190, 205);
  }

  .breakdown-values {
    font-size: 0.75rem;
    display: flex;
    gap: 4px;
    align-items: center;
  }

  .current {
    color: rgb(200, 210, 225);
    font-weight: 500;
  }

  .separator {
    color: rgb(100, 110, 130);
  }

  .average {
    color: rgb(140, 150, 170);
  }

  .progress-bar {
    height: 6px;
    background-color: rgb(35, 42, 55);
    border-radius: 3px;
    overflow: hidden;
  }

  .progress-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.15s ease-out;
    min-width: 2px;
  }

  .progress-fill.commands {
    background: linear-gradient(90deg, rgb(80, 180, 140), rgb(100, 200, 160));
  }

  .progress-fill.queue {
    background: linear-gradient(90deg, rgb(100, 140, 200), rgb(120, 160, 220));
  }

  .progress-fill.sort {
    background: linear-gradient(90deg, rgb(180, 140, 100), rgb(200, 160, 120));
  }

  .progress-fill.serialization {
    background: linear-gradient(90deg, rgb(200, 120, 100), rgb(220, 140, 120));
  }

  .progress-fill.delta {
    background: linear-gradient(90deg, rgb(160, 100, 180), rgb(180, 120, 200));
  }

  .progress-fill.network {
    background: linear-gradient(90deg, rgb(100, 160, 200), rgb(120, 180, 220));
  }

  .progress-fill.connection {
    background: linear-gradient(90deg, rgb(180, 160, 100), rgb(200, 180, 120));
  }

  .totals-section {
    margin-top: auto;
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding-top: 12px;
    border-top: 1px solid rgb(50, 60, 80);
  }

  .total-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background-color: rgb(28, 35, 50);
    border-radius: 6px;
  }

  .total-row.highlight {
    background-color: rgb(35, 50, 70);
    border: 1px solid rgb(50, 70, 100);
  }

  .total-label {
    font-size: 0.8rem;
    color: rgb(160, 170, 190);
  }

  .total-value {
    font-size: 1rem;
    font-weight: 600;
    color: rgb(230, 235, 245);
  }
</style>
