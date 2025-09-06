<!-- TODO: CLEAN THIS UP A LOT -->
<script lang="ts">
  import {
    defaultStateMetadata,
    editState,
    type CommandState,
    type CommandStateValue,
    type CommandStateValueBase,
    type StateMetadata
  } from '../lib/stores/schedulableStore'
  import Self from './StateValue.svelte'

  let {
    label,
    value,
    stateMap,
    path = label,
    isRoot = true,
    seen = [] as object[]
  }: {
    label: string
    value: CommandState
    stateMap?: { getState: (field: string) => [CommandStateValueBase, StateMetadata] }
    path?: string
    isRoot?: boolean
    seen?: object[]
  } = $props()

  let expanded = $state(false)
  function toggle() {
    expanded = !expanded
  }

  function isLeaf(v: CommandState): v is CommandStateValue {
    return typeof v === 'object' && v !== null && 'value' in v && !Array.isArray(v)
  }

  // no-op: previously used for object iteration, now we inline JSON formatting

  const isArrayNode = $derived(Array.isArray(value))
  const isLeafNode = $derived(isLeaf(value))
  const statePair = $derived(isRoot && isLeafNode && stateMap ? stateMap.getState(label) : null)
  const [_, meta] = $derived(statePair ?? [value, defaultStateMetadata])
  const leafDisplayValue = $derived(
    isLeafNode ? (statePair ? statePair[0] : (value as CommandStateValue).value) : null
  )
  const leafDisplayMeta = $derived(
    isLeafNode ? (statePair ? statePair[1] : (value as CommandStateValue).metadata) : null
  )
  const isGroup = $derived(isArrayNode || (!isLeafNode && typeof value === 'object'))
  const isCircular = $derived(
    isGroup &&
      typeof value === 'object' &&
      value !== null &&
      seen.includes(value as unknown as object)
  )

  // Pass a cloned set with current node added to children to avoid mutating parent set
  const childSeen: object[] = $derived(
    (() => {
      if (isGroup && typeof value === 'object' && value !== null) {
        return [...seen, value as unknown as object]
      }
      return seen
    })()
  )

  function priorityOf(node: CommandState): number {
    if (Array.isArray(node)) return 0
    if (isLeaf(node)) return typeof node.value === 'number' ? Number(node.value) : 0
    if (typeof node === 'object' && node !== null) {
      const pr = (node as Record<string, CommandState>)['priority']
      if (pr && typeof pr === 'object' && 'value' in pr) {
        const pv = (pr as CommandStateValue).value
        return typeof pv === 'number' ? Number(pv) : 0
      }
    }
    return 0
  }

  const sortedArray = $derived(
    Array.isArray(value) ? [...value].sort((a, b) => priorityOf(b) - priorityOf(a)) : null
  )
  // sortedEntries removed: objects render inline JSON when not arrays/leaves
</script>

{#if !meta.hidden}
  <li class={isGroup ? 'group' : 'leaf'}>
    {#if Array.isArray(value)}
      <button class="row group-header" onclick={toggle} aria-expanded={expanded} title="Toggle">
        <span class="chevron">{expanded ? '▾' : '▸'}</span>
        <span class="key">{label}</span>
        <span class="meta">[{value.length}]</span>
      </button>
      {#if isCircular}
        <div class="row circular">
          <span class="chevron">⟲</span><span class="key">circular reference</span>
        </div>
      {:else if expanded}
        <ul class="children">
          {#each sortedArray ?? value as child, idx (idx)}
            <Self
              label={`${idx}`}
              value={child}
              {stateMap}
              path={`${path}.${idx}`}
              isRoot={false}
              seen={childSeen}
            />
          {/each}
        </ul>
      {/if}
    {:else if isLeafNode}
      <div class="row leaf-row">
        <span class="key">{label}</span>
        <span class="spacer"></span>
        {#if leafDisplayMeta?.readonly}
          <span class="readonly">read-only</span>
          <span class="val">{leafDisplayValue}</span>
        {:else}
          <input
            class="input"
            type="text"
            value={String(leafDisplayValue ?? '')}
            oninput={(e) => (value.value = (e.currentTarget as HTMLInputElement).value)}
            onkeydown={(e) => {
              if (e.key === 'Enter') {
                editState(path, value.value as CommandStateValueBase)
              }
            }}
          />
        {/if}
      </div>
    {:else}
      <div class="row leaf-row">
        <span class="key">{label}</span>
        <span class="spacer"></span>
        {#if isCircular}
          <span class="readonly">circular</span>
        {:else}
          <span class="val">{JSON.stringify(value)}</span>
        {/if}
      </div>
    {/if}
  </li>
{/if}

<style>
  .row {
    display: flex;
    align-items: center;
    gap: 8px;
    background-color: rgb(33, 42, 60);
    border: 1px solid rgb(43, 52, 70);
    padding: 10px;
    border-radius: 10px;
    color: rgb(230, 233, 239);
    transition: background-color 0.12s ease-in-out;
  }

  .group-header {
    width: 100%;
    text-align: left;
    cursor: pointer;
    background: transparent;
    border: none;
  }

  .group {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .children {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin-left: 10px;
  }

  .children.hidden {
    display: none;
  }

  .chevron {
    width: 1rem;
    display: inline-block;
  }

  .key {
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .meta {
    opacity: 0.8;
    font-size: 0.9rem;
    margin-left: auto;
  }

  .spacer {
    flex: 1;
  }

  .val {
    font-family:
      ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
      monospace;
    background-color: rgb(43, 52, 70);
    padding: 2px 6px;
    border-radius: 6px;
  }

  .readonly {
    font-size: 0.75rem;
    opacity: 0.7;
    margin-right: 8px;
  }

  .circular {
    background-color: rgba(200, 80, 80, 0.12);
    border-color: rgba(200, 80, 80, 0.35);
  }

  .input {
    background-color: rgb(43, 52, 70);
    color: rgb(230, 233, 239);
    border: 1px solid rgb(43, 52, 70);
    border-radius: 6px;
    padding: 4px 6px;
    min-width: 100px;
    width: auto;
  }
</style>
