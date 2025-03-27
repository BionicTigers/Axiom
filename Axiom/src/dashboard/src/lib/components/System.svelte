<script lang="ts" module>
  import type { CommandData } from "./Command.svelte";

    export class SystemData {
        name: string;
        beforeRun: CommandData;
        afterRun: CommandData;
        dependencies: CommandData[];

        constructor(name: string, beforeRun: CommandData, afterRun: CommandData, dependencies: CommandData[]) {
            this.name = name;
            this.beforeRun = beforeRun;
            this.afterRun = afterRun;
            this.dependencies = dependencies;
        }
    }
</script>

<script lang="ts">
    let { children, name }: { children: any, name?: string } = $props();
</script>

<div class="flex flex-col w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2 overflow-y-auto">
    {#if name != undefined}
        <h1 class="text-orange-300 text-lg font-bold select-none text-center">
            {name}
        </h1>
    {/if}

    <div class="flex flex-col items-center justify-center w-full">
        {@render children()}
    </div>
</div>