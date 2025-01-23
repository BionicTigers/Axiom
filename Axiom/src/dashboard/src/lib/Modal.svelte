<script lang="ts">
    let { children, enabled, closeModal }: { children: any, enabled: boolean, closeModal: () => void } = $props();
</script>

{#if enabled}
    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div 
        class="fixed inset-0 bg-black/50 flex items-center justify-center z-10"
        onclick={closeModal}
        role="dialog"
    >
        <!-- svelte-ignore a11y_no_static_element_interactions -->
        <div 
        class="bg-neutral-800/50 ring-2 z-10 ring-neutral-900 rounded-lg p-4 w-2/3 mx-auto mt-5 backdrop-blur-lg transition-colors"
            onclick={(e) => e.stopPropagation()}
        >
            {@render children()}
        </div>
    </div>
{/if}