<script lang="ts">
  import { update } from '../../../lib/stores/windows'
  import {
    behaviorTreeIds,
    selectedTreeId,
    selectedTreeState,
    displayedTrace,
    selectTree,
    getStatusColor,
    getStatusIcon,
    type NodeSnapshot
  } from '../../../lib/stores/behaviorTreeStore'
  import {
    isPaused,
    isViewingHistorical,
    historyBounds
  } from '../../../lib/stores/historyStore'

  let { id }: { id: string } = $props()
  update(id, { maxW: 500, minW: 350, minH: 400 })

  // Build tree structure from flat nodes
  function buildTree(nodes: NodeSnapshot[]): Map<string | null, NodeSnapshot[]> {
    const childrenMap = new Map<string | null, NodeSnapshot[]>()
    for (const node of nodes) {
      const parentId = node.parentId
      if (!childrenMap.has(parentId)) {
        childrenMap.set(parentId, [])
      }
      childrenMap.get(parentId)!.push(node)
    }
    return childrenMap
  }

  // Get tree depth for indentation
  function getNodeDepth(
    nodeId: string,
    nodes: NodeSnapshot[],
    cache: Map<string, number> = new Map()
  ): number {
    if (cache.has(nodeId)) return cache.get(nodeId)!
    const node = nodes.find((n) => n.id === nodeId)
    if (!node || !node.parentId) {
      cache.set(nodeId, 0)
      return 0
    }
    const depth = 1 + getNodeDepth(node.parentId, nodes, cache)
    cache.set(nodeId, depth)
    return depth
  }

  // Flatten tree for display with proper ordering
  function flattenTree(
    childrenMap: Map<string | null, NodeSnapshot[]>,
    parentId: string | null = null
  ): NodeSnapshot[] {
    const children = childrenMap.get(parentId) || []
    const result: NodeSnapshot[] = []
    for (const child of children) {
      result.push(child)
      result.push(...flattenTree(childrenMap, child.id))
    }
    return result
  }

  let treeNodes = $derived.by(() => {
    const trace = $displayedTrace
    if (!trace) return []
    const childrenMap = buildTree(trace.nodes)
    return flattenTree(childrenMap)
  })

  let depthCache = $derived.by(() => {
    const trace = $displayedTrace
    if (!trace) return new Map<string, number>()
    const cache = new Map<string, number>()
    for (const node of trace.nodes) {
      getNodeDepth(node.id, trace.nodes, cache)
    }
    return cache
  })

  let blackboardEntries = $derived.by(() => {
    const trace = $displayedTrace
    if (!trace) return []
    return Object.entries(trace.blackboard)
  })

  let activePathSet = $derived.by(() => {
    const trace = $displayedTrace
    if (!trace) return new Set<string>()
    return new Set(trace.activeNodePath)
  })
</script>

<div class="bt-debugger">
  <!-- Header with tree selector -->
  <div class="header">
    <div class="tree-selector">
      <label for="tree-select">Tree:</label>
      <select
        id="tree-select"
        value={$selectedTreeId || ''}
        onchange={(e) => selectTree(e.currentTarget.value || null)}
      >
        {#if $behaviorTreeIds.length === 0}
          <option value="">No trees connected</option>
        {:else}
          {#each $behaviorTreeIds as treeId (treeId)}
            {@const state = $selectedTreeState}
            <option value={treeId}>{state?.treeName || treeId}</option>
          {/each}
        {/if}
      </select>
    </div>
  </div>

  {#if !$selectedTreeState}
    <div class="empty-state">
      <p>No behavior tree selected</p>
      <p class="hint">Run a traced behavior tree to see it here</p>
    </div>
  {:else}
    <!-- Status indicator -->
    {#if $isPaused}
      <div class="pause-indicator" class:historical={$isViewingHistorical}>
        {#if $isViewingHistorical}
          Viewing historical tick {$displayedTrace?.tick ?? '-'}
        {:else}
          Paused at tick {$displayedTrace?.tick ?? '-'}
        {/if}
      </div>
    {/if}

    <!-- Stats -->
    <div class="stats">
      <div class="stat">
        <span class="stat-label">Tick</span>
        <span class="stat-value">{$displayedTrace?.tick ?? '-'}</span>
      </div>
      <div class="stat">
        <span class="stat-label">Nodes</span>
        <span class="stat-value">{$displayedTrace?.nodes.length ?? 0}</span>
      </div>
      <div class="stat">
        <span class="stat-label">History</span>
        <span class="stat-value">{$historyBounds.max + 1}</span>
      </div>
    </div>

    <!-- Active Path -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">Active Path</span>
      </div>
      <div class="active-path">
        {#if $displayedTrace?.activeNodePath.length}
          {#each $displayedTrace.activeNodePath as nodeName, i (i)}
            <span class="path-segment">{nodeName}</span>
            {#if i < $displayedTrace.activeNodePath.length - 1}
              <span class="path-arrow">→</span>
            {/if}
          {/each}
        {:else}
          <span class="empty-text">No active path</span>
        {/if}
      </div>
    </div>

    <!-- Node Tree -->
    <div class="section tree-section">
      <div class="section-header">
        <span class="section-title">Node Tree</span>
      </div>
      <div class="tree-container">
        {#each treeNodes as node (node.id)}
          {@const depth = depthCache.get(node.id) ?? 0}
          {@const isActive = activePathSet.has(node.name)}
          <div
            class="tree-node"
            class:active={isActive}
            style="--depth: {depth}"
          >
            <span
              class="status-indicator"
              style="color: {getStatusColor(node.status)}"
              title={node.status}
            >
              {getStatusIcon(node.status)}
            </span>
            <span class="node-name">{node.name}</span>
            <span class="node-type">{node.type}</span>
          </div>
        {:else}
          <div class="empty-text">No nodes</div>
        {/each}
      </div>
    </div>

    <!-- Blackboard -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">Blackboard</span>
        <span class="count">{blackboardEntries.length}</span>
      </div>
      <div class="blackboard-container">
        {#each blackboardEntries as [key, value] (key)}
          <div class="blackboard-entry">
            <span class="bb-key">{key}</span>
            <span class="bb-value">{JSON.stringify(value)}</span>
          </div>
        {:else}
          <div class="empty-text">Blackboard is empty</div>
        {/each}
      </div>
    </div>
  {/if}
</div>

<style>
  .bt-debugger {
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 12px;
    font-family: 'Menlo', 'Monaco', monospace;
  }

  .header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid rgb(60, 70, 90);
    flex-shrink: 0;
  }

  .tree-selector {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
  }

  .tree-selector label {
    font-size: 0.85rem;
    color: rgb(150, 160, 180);
  }

  .tree-selector select {
    flex: 1;
    padding: 6px 10px;
    background-color: rgb(33, 42, 60);
    border: 1px solid rgb(60, 70, 90);
    border-radius: 6px;
    color: rgb(230, 233, 239);
    font-size: 0.85rem;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
    color: rgb(120, 130, 150);
    text-align: center;
  }

  .empty-state .hint {
    font-size: 0.8rem;
    color: rgb(100, 110, 130);
  }

  .pause-indicator {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 8px 12px;
    background-color: rgb(60, 80, 50);
    border: 1px solid rgb(80, 110, 70);
    border-radius: 6px;
    color: rgb(180, 220, 160);
    font-size: 0.8rem;
    font-weight: 500;
    flex-shrink: 0;
  }

  .pause-indicator.historical {
    background-color: rgb(80, 60, 40);
    border-color: rgb(120, 90, 60);
    color: rgb(220, 180, 140);
  }

  .stats {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }

  .stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    padding: 8px;
    background-color: rgb(33, 42, 60);
    border-radius: 6px;
    border: 1px solid rgb(43, 52, 70);
    min-height: 48px;
  }

  .stat-label {
    font-size: 0.65rem;
    color: rgb(150, 160, 180);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .stat-value {
    font-size: 1rem;
    font-weight: 600;
    color: rgb(230, 233, 239);
  }

  .section {
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex-shrink: 0;
  }

  .tree-section {
    flex: 1;
    min-height: 100px;
    flex-shrink: 1;
  }

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .section-title {
    font-weight: 600;
    font-size: 0.85rem;
    color: rgb(180, 190, 210);
  }

  .count {
    margin-left: auto;
    font-size: 0.7rem;
    color: rgb(150, 160, 180);
    background-color: rgb(43, 52, 70);
    padding: 2px 6px;
    border-radius: 8px;
  }

  .active-path {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    align-content: flex-start;
    gap: 4px;
    padding: 8px;
    background-color: rgb(33, 42, 60);
    border-radius: 6px;
    font-size: 0.8rem;
    min-height: 32px;
    max-height: 60px;
    overflow-y: auto;
  }

  .path-segment {
    padding: 2px 6px;
    background-color: rgb(50, 70, 100);
    border-radius: 4px;
    color: rgb(180, 200, 230);
  }

  .path-arrow {
    color: rgb(100, 120, 150);
  }

  .tree-container {
    display: flex;
    flex-direction: column;
    gap: 2px;
    overflow-y: auto;
    flex: 1;
    padding: 4px;
    background-color: rgb(25, 32, 45);
    border-radius: 6px;
  }

  .tree-node {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 8px;
    padding-left: calc(8px + var(--depth) * 16px);
    border-radius: 4px;
    font-size: 0.8rem;
    transition: background-color 0.15s;
  }

  .tree-node:hover {
    background-color: rgb(40, 50, 70);
  }

  .tree-node.active {
    background-color: rgb(40, 60, 90);
  }

  .status-indicator {
    font-size: 0.9rem;
    min-width: 16px;
    text-align: center;
  }

  .node-name {
    color: rgb(220, 225, 235);
    flex: 1;
  }

  .node-type {
    font-size: 0.7rem;
    color: rgb(120, 140, 170);
    padding: 1px 4px;
    background-color: rgb(40, 50, 70);
    border-radius: 3px;
  }

  .blackboard-container {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-height: 150px;
    overflow-y: auto;
    padding: 8px;
    background-color: rgb(25, 32, 45);
    border-radius: 6px;
  }

  .blackboard-entry {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.75rem;
  }

  .bb-key {
    color: rgb(150, 180, 220);
    min-width: 100px;
  }

  .bb-value {
    color: rgb(180, 200, 150);
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .empty-text {
    color: rgb(100, 110, 130);
    font-size: 0.8rem;
    font-style: italic;
    padding: 8px;
    text-align: center;
  }
</style>
