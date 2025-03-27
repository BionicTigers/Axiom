<script lang="ts">
    import type { InputType } from "./Command.svelte";
    import { Websocket } from "./Networking";
    import State from "../components/State.svelte";
    let { key, value, path }: { key: string, value: InputType, path: string } = $props();

    function getInputType(value: any) {
        if (typeof value === "number") return "number";
        if (typeof value === "string") return "text";
        if (typeof value === "boolean") return "checkbox";
        return "text";
    }

    function roundIfNumber(value: any) {
        if (typeof value === "number") return Math.round(value * 1000) / 1000;
        return value;
    }

    function handleChange(event: Event) {
        let input = event.target as HTMLInputElement;
        Websocket.Functions.edit(path, input.value);
    }
</script>

<div class="flex gap-2">
    <label for={key} class="text-orange-100 text-lg font-medium select-none h-8 w-fit max-w-1/2">
        {key}
    </label>
    {#if value.value == undefined}
        {#each Object.entries(value) as [key, v]}
            <State key={key} value={v} path={path + "." + key} />
        {/each}
    {:else if !value.readOnly}
        <input id={key} type={getInputType(value.value)} class="bg-neutral-800/40 text-orange-200 w-1/2 ring-2 h-8 ring-neutral-900 rounded-lg p-2 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors" value={roundIfNumber(value.value)} onchange={handleChange} />
    {:else}
        <p class="text-orange-200 text-lg font-medium select-none h-8 w-fit max-w-1/2 bg-neutral-800/40 ring-2 ring-neutral-900 rounded-lg p-2 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors flex items-center">
            {roundIfNumber(value.value)}
        </p>
    {/if}
</div>