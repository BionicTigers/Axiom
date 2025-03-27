<script lang="ts">
    let { options, selected = $bindable() }: { options: string[], selected: string } = $props();
</script>

<div class="flex items-center justify-center w-full gap-2">
    {#each options as option}
        <button 
            class="text-orange-400 bg-neutral-800/40 ring-2 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
            onclick={() => selected = option}
        >
            {option}
        </button>
    {/each}
</div>