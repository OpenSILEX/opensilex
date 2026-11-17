<template>
  <!--
    A Mermaid diagram drawn from its text. The library is loaded the first time a diagram is shown; it
    renders with the page's own theme and sanitises what it draws. Registered globally so that other
    modules draw their diagrams without carrying Mermaid a second time.
  -->
  <div ref="host" class="ai-import-mermaid" v-html="svg"></div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, useTemplateRef, watch } from 'vue'

//#region Public
const emit = defineEmits<{
  /** The diagram is drawn: the host element holds its SVG, for the caller to make parts of it clickable. */
  rendered: [host: HTMLElement]
  /** The diagram could not be drawn: the caller shows another view of the same content. */
  failed: [error: unknown]
}>()

const props = defineProps<{
  /** The diagram, in Mermaid's language. */
  text: string
  /** Starts the id of the drawn SVG, so that two diagrams of one page do not share ids. */
  idPrefix?: string
}>()
//#endregion

//#region Private

//#region Template refs
const host = useTemplateRef<HTMLElement>('host')
//#endregion

//#region Data & Computed
const svg = ref('')
//#endregion

//#region Hooks & watchers
onMounted(render)
watch(() => props.text, render)
//#endregion

//#region Methods
async function render() {
  try {
    const { default: mermaid } = await import('mermaid')
    mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: isDark() ? 'dark' : 'default' })
    const drawn = await mermaid.render((props.idPrefix ?? 'ai-import-diagram') + '-' + Date.now(), props.text)
    svg.value = drawn.svg
    await nextTick()
    if (host.value) {
      emit('rendered', host.value)
    }
  } catch (error) {
    svg.value = ''
    emit('failed', error)
  }
}

function isDark(): boolean {
  const chosen = document.documentElement.getAttribute('data-theme')
  return chosen ? chosen === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
.ai-import-mermaid {
  overflow-x: auto;
}
</style>
