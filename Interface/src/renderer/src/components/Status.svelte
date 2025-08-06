<script lang="ts">
  let { isConnected, latency }: { isConnected: boolean; latency: number } = $props()
  let status = $derived(latency < 10 ? 'stable' : 'unstable')
</script>

<ul>
  {#if isConnected}
    <li>
      Axiom - <span class="axiom-status {status}">{status}</span>
      <span class="latency">({latency}ms)</span>
    </li>
  {:else}
    <li>Axiom - <span class="not-connected axiom-status">not connected</span></li>
  {/if}
  <li>Seek - v1.0.0</li>
</ul>

<style>
  ul {
    position: absolute;
    bottom: 30px;
    margin: 0 auto;
    padding: 15px 0;
    font-family: 'Menlo', 'Lucida Console', monospace;
    display: inline-flex;
    overflow: hidden;
    align-items: center;
    border-radius: 22px;
    background-color: #202127;
    backdrop-filter: blur(24px);
  }

  ul li {
    display: block;
    float: left;
    border-right: 1px solid var(--ev-c-gray-1);
    padding: 0 20px;
    font-size: 14px;
    line-height: 14px;
    opacity: 0.8;
    &:last-child {
      border: none;
    }
  }

  .axiom-status {
    text-transform: uppercase;
  }

  .stable {
    color: var(--ev-c-green);
  }

  .unstable {
    color: var(--ev-c-yellow);
  }

  .latency {
    color: var(--ev-c-gray-1);
    font-size: 12px;
  }

  .not-connected {
    color: var(--ev-c-red);
  }
</style>
