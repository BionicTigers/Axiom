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
  let cachedType = $state('string')

  const isArrayNode = $derived(Array.isArray(value))
  const isLeafNode = $derived(
    typeof value === 'object' && value !== null && 'value' in value && !Array.isArray(value)
  )
  const isGroup = $derived(isArrayNode || (!isLeafNode && typeof value === 'object'))

  const [displayValue, meta] = $derived.by(() => {
    if (!isLeafNode) return [null, defaultStateMetadata]

    if (isRoot && stateMap) {
      return stateMap.getState(label)
    }

    const v = value as CommandStateValue
    const m = v.metadata

    if (m) {
      return [v.value, { ...defaultStateMetadata, ...m }]
    }

    return [v.value, { ...defaultStateMetadata, readonly: false }]
  })

  $effect(() => {
    if (isLeafNode) {
      const v = (value as CommandStateValue).value
      if (typeof v === 'boolean') {
        cachedType = 'boolean'
      } else if (typeof v === 'number') {
        cachedType = 'number'
      } else if (typeof v === 'string') {
        // If we are currently number mode, and value is a valid numeric string (being typed), stay number
        const isNumericString = v === '' || v === '-' || !isNaN(Number(v))
        if (cachedType === 'number' && isNumericString) {
          // preserve number type
        } else {
          cachedType = 'string'
        }
      }
    }
  })

  function priorityOf(node: CommandState): number {
    if (Array.isArray(node)) return 0
    if (typeof node === 'object' && node !== null) {
      if ('value' in node && !Array.isArray(node)) {
        const v = (node as CommandStateValue).value
        return typeof v === 'number' ? Number(v) : 0
      }
      const pr = (node as Record<string, CommandState>)['priority']
      if (pr && typeof pr === 'object' && 'value' in pr) {
        const pv = (pr as CommandStateValue).value
        return typeof pv === 'number' ? Number(pv) : 0
      }
    }
    return 0
  }

  const sortedArray = $derived(
    isArrayNode
      ? [...(value as CommandState[])].sort((a, b) => priorityOf(b) - priorityOf(a))
      : null
  )

  const isCircular = $derived(
    isGroup &&
      typeof value === 'object' &&
      value !== null &&
      seen.includes(value as unknown as object)
  )

  const childSeen = $derived(
    isGroup && typeof value === 'object' && value !== null
      ? [...seen, value as unknown as object]
      : seen
  )

  function toggle() {
    expanded = !expanded
  }
</script>

{#snippet arrayGroup(items: CommandState[])}
  <button class="row group-header" onclick={toggle} aria-expanded={expanded} title="Toggle">
    <span class="chevron">{expanded ? '▾' : '▸'}</span>
    <span class="key">{label}</span>
    <span class="meta">[{items.length}]</span>
  </button>

  {#if isCircular}
    <div class="row circular">
      <span class="chevron">⟲</span><span class="key">circular reference</span>
    </div>
  {:else if expanded}
    <ul class="children">
      {#each items as child, idx (idx)}
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
{/snippet}

{#snippet leaf(val: CommandStateValueBase, m: StateMetadata)}
  <div class="row leaf-row">
    <span class="key">{label}</span>
    <span class="spacer"></span>
    <div class="value-container">
      {#if m.readonly}
        <span class="readonly-icon" title="Read-only">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
            />
          </svg>
        </span>
        <span class="val">{String(val)}</span>
      {:else if cachedType === 'boolean'}
        <input
          class="checkbox"
          type="checkbox"
          checked={Boolean(val)}
          onchange={(e) => {
            const checked = e.currentTarget.checked
            ;(value as CommandStateValue).value = checked
            editState(path, checked)
          }}
        />
      {:else if cachedType === 'number'}
        <input
          class="input"
          type="number"
          value={val === null ? '' : val}
          oninput={(e) => {
            ;(value as CommandStateValue).value = e.currentTarget.value
          }}
          onkeydown={(e) => {
            if (e.key === 'Enter') {
              const n = Number((value as CommandStateValue).value)
              editState(path, isNaN(n) ? 0 : n)
            }
          }}
        />
      {:else}
        <input
          class="input"
          type="text"
          value={String(val ?? '')}
          oninput={(e) => {
            ;(value as CommandStateValue).value = e.currentTarget.value
          }}
          onkeydown={(e) => {
            if (e.key === 'Enter') {
              editState(path, (value as CommandStateValue).value as string)
            }
          }}
        />
      {/if}
    </div>
  </div>
{/snippet}

{#snippet genericObject(obj: unknown)}
  <div class="row leaf-row">
    <span class="key">{label}</span>
    <span class="spacer"></span>
    {#if isCircular}
      <span class="readonly">circular</span>
    {:else}
      <span class="val">{JSON.stringify(obj)}</span>
    {/if}
  </div>
{/snippet}

{#if !meta.hidden}
  <li class={isGroup ? 'group' : 'leaf'}>
    {#if isArrayNode && sortedArray}
      {@render arrayGroup(sortedArray)}
    {:else if isLeafNode}
      {@render leaf(displayValue, meta)}
    {:else}
      {@render genericObject(value)}
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
    padding: 10px 12px;
    border-radius: 8px;
    color: rgb(230, 233, 239);
    transition:
      background-color 0.15s,
      border-color 0.15s;
  }

  .row:hover {
    background-color: rgb(38, 47, 65);
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
    margin-left: 12px;
    padding-left: 12px;
    border-left: 2px solid rgb(50, 60, 80);
  }

  .group-header {
    width: 100%;
    text-align: left;
    cursor: pointer;
    background: transparent;
    border: none;
    color: inherit;
  }

  .group-header:hover {
    background-color: rgb(43, 52, 70);
  }

  .chevron {
    width: 1rem;
    display: inline-block;
    color: rgb(150, 160, 180);
  }

  .key {
    font-weight: 500;
    font-size: 0.9rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .meta {
    font-size: 0.8rem;
    color: rgb(150, 160, 180);
    margin-left: auto;
    background-color: rgb(50, 60, 80);
    padding: 2px 8px;
    border-radius: 4px;
  }

  .spacer {
    flex: 1;
  }

  .value-container {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    min-width: 120px;
    justify-content: flex-end;
  }

  .val {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 0.85rem;
    background-color: rgb(43, 52, 70);
    opacity: 0.8;
    padding: 4px 8px;
    border-radius: 4px;
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .readonly-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    color: rgb(120, 130, 150);
    flex-shrink: 0;
  }

  .circular {
    background-color: rgba(200, 80, 80, 0.12);
    border-color: rgba(200, 80, 80, 0.35);
  }

  .input {
    background-color: rgb(43, 52, 70);
    color: rgb(230, 233, 239);
    border: 1px solid rgb(53, 62, 80);
    border-radius: 6px;
    padding: 6px 10px;
    width: 120px;
    font-size: 0.9rem;
    transition: border-color 0.15s;
    flex-shrink: 0;
  }

  .input:focus {
    outline: none;
    border-color: rgb(77, 171, 247);
  }

  .checkbox {
    width: 1.1rem;
    height: 1.1rem;
    cursor: pointer;
    accent-color: rgb(77, 171, 247);
    flex-shrink: 0;
  }
</style>
