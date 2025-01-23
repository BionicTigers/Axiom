<script lang="ts">
    import type { CommandData, InputType } from "./Command.svelte";
    import StateSelectorOption from "./StateSelectorOption.svelte";

    let { state = $bindable<Array<string>>([]), key, value, path }: { state: Array<string>, key: string, value: InputType, path: string } = $props();

    let fullPath = path + "." + key;
</script>

{#if value.value == undefined || typeof value.value == "number"}
    <div class="flex w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2">
        {#if value.value == undefined}
            {#each Object.entries(value) as [key, v]}
                <StateSelectorOption bind:state={state} key={key} value={v} path={fullPath} />
            {/each}
        {:else}
            <p class="text-orange-200 text-lg font-medium select-none h-8 w-fit max-w-1/2 bg-neutral-800/40 ring-2 ring-neutral-900 rounded-lg p-2 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors flex items-center">
                {key}
            </p>
            <input type="checkbox" class="w-full h-8 bg-neutral-800/40 ring-2 ring-neutral-900 rounded-xl outline-none p-2 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors" onchange={() => {
                if (state.includes(fullPath)) {
                    state.splice(state.indexOf(fullPath), 1);
                } else {
                    state.push(fullPath);
                }
            }} />
        {/if}
    </div>
{/if}
