<script lang="ts">
    let { children, name, overflowHeight = "13rem" }: { children: any, name?: string, overflowHeight?: string } = $props();

    let isOpen = $state(false);

    function toggleOpen() {
        isOpen = !isOpen;
    }
</script>

<div class="flex flex-col w-full">
    <button class="flex justify-between w-full p-2" onclick={toggleOpen}>
        <h3 class="text-orange-300 text-lg font-bold">{name}</h3>
        <p class="text-orange-300 text-lg">{isOpen ? '-' : '+'}</p>
    </button>
    <div class="flex flex-col overflow-y-auto bg-neutral-900 rounded-lg p-2" class:hidden={!isOpen} style="max-height: {overflowHeight}">
        {@render children()}
    </div>
</div>