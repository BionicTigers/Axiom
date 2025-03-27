<script lang="ts">
    import { filterInternalValues, type CommandData, type InputType } from "./Command.svelte";
    import StateSelectorOption from "./StateSelectorOption.svelte";

    let { state: pathState = $bindable<Array<string>>([]), key, value, path }: { state: Array<string>, key: string, value: InputType, path: string } = $props();

    let fullPath = path + "." + key;
    let isChecked = $state(pathState.includes(fullPath));

    // svelte-ignore state_referenced_locally
    let lastIsChecked = isChecked;
    $effect(() => {
        if (isChecked != lastIsChecked) {
            lastIsChecked = isChecked;
            if (isChecked) {
                pathState.push(fullPath);
            } else {
                pathState.splice(pathState.indexOf(fullPath), 1);
            }
        }
    });
</script>

{#if value.value == undefined || typeof value.value == "number"}
    <div class="flex w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2">
        {#if value.value == undefined}
            <div class="flex flex-col">
                <p class="text-orange-200">{key}</p>
                <div class="flex overflow-x-auto">
                    {#each Object.entries(value) as [key, v]}
                        <StateSelectorOption bind:state={pathState} key={key} value={v} path={fullPath} />
                    {/each}
                </div>
            </div>
        {:else}
            <p class="text-orange-200 text-lg font-medium select-none h-8 w-fit max-w-1/2 bg-neutral-800/40 ring-2 ring-neutral-900 rounded-lg p-2 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors flex items-center">
                {key}
            </p>
            <input type="checkbox" class="w-8 h-8 bg-neutral-800/40 ring-2 ring-neutral-900 rounded-xl outline-none p-2 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors" bind:checked={isChecked} />
        {/if}
    </div>
{/if}
