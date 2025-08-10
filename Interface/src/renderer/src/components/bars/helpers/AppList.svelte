<script lang="ts">
  import type { Component } from 'svelte'

  type Option = { label: string }

  let {
    title,
    icon: Icon,
    options = [],
    isOpen = false,
    onToggle
  }: {
    title: string
    icon: Component
    options: Option[]
    isOpen: boolean
    onToggle: () => void
  } = $props()

  function handleToggle(): void {
    onToggle()
  }

  function handleOptionClick(label: string): void {
    console.log(`Option clicked: ${label}`)
  }
</script>

<div class="app" class:open={isOpen}>
  <button class="app-trigger" type="button" onclick={handleToggle} aria-expanded={isOpen}>
    <div class="app-icon"><Icon /></div>
    <div class="app-title">{title}</div>
  </button>

  <div class="dropdown">
    <ul>
      {#each options as opt (opt.label)}
        <li>
          <button onclick={() => handleOptionClick(opt.label)}>
            {opt.label}
          </button>
        </li>
      {/each}
    </ul>
  </div>
</div>

<style>
  .app {
    position: relative;
    display: inline-flex;
    align-items: center;
  }

  .app-trigger {
    display: flex;
    align-items: center;
    gap: 0;
    background: none;
    border: 0;
    color: inherit;
    padding: 0;
    cursor: pointer;
  }
  .app-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    line-height: 0;
  }

  .app-title {
    overflow: hidden;
    white-space: nowrap;
    max-width: 0;
    opacity: 0;
    transform: translateX(-6px);
    padding-left: 0;
    transition:
      max-width 220ms cubic-bezier(0.22, 1, 0.36, 1),
      opacity 180ms ease,
      transform 220ms cubic-bezier(0.22, 1, 0.36, 1),
      padding-left 220ms cubic-bezier(0.22, 1, 0.36, 1);
  }

  /* Expand when hovering/focusing the trigger */
  .app.open .app-title,
  .app-trigger:hover .app-title,
  .app-trigger:focus-visible .app-title {
    max-width: 160px;
    opacity: 1;
    transform: translateX(0);
    padding-left: 8px;
  }

  .dropdown {
    position: absolute;
    top: calc(100% + 10px);
    left: 50%;
    background: #2a2b31;
    border: 1px solid var(--ev-c-gray-1);
    border-radius: 10px;
    padding: 8px 0;
    min-width: 140px;
    box-shadow: 0 10px 24px rgba(0, 0, 0, 0.35);
    z-index: 10;
    opacity: 0;
    transform: translate(-50%, -4px) scale(0.98);
    pointer-events: none;
    max-height: 0;
    overflow: hidden;
    transition:
      opacity 180ms ease,
      transform 200ms cubic-bezier(0.22, 1, 0.36, 1),
      max-height 220ms cubic-bezier(0.22, 1, 0.36, 1);
  }

  .app.open .dropdown {
    opacity: 1;
    transform: translate(-50%, 0) scale(1);
    pointer-events: auto;
    max-height: 400px; /* large enough to fit options */
  }

  .dropdown ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }
  .dropdown li {
    cursor: pointer;
  }
  .dropdown li:hover {
    background: rgba(255, 255, 255, 0.06);
  }
  .dropdown li button {
    background: none;
    border: 0;
    color: var(--ev-c-white);
    padding: 8px 14px;
    cursor: pointer;
    text-align: left;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: flex-start;
  }
</style>
