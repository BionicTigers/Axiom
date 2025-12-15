<script lang="ts">
  import { update } from '../../../lib/stores/windows'

  let { id }: { id: string } = $props()
  update(id, { maxW: 450 })
</script>

<div>
  <h1>Telemetry</h1>
  <ul></ul>
</div>
