<script lang="ts">
  import type { Win } from '../../../lib/stores/windows'

  let { win } = $props<{ win: Win }>()
</script>

<div>
  <h1>{win.title}</h1>
</div>
