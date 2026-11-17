<template>
  <!--
    The sheets of the import and how their rows hang together. Always readable as a list; drawn as a
    diagram by the assistant module's Mermaid component when that module can be loaded, the list
    taking over when it cannot.
  -->
  <div class="graph-import-sheet-map mb-3">
    <n-space align="center" :size="6" class="mb-2">
      <span class="fw-semibold">{{ $t('GraphImport.preview.map.title') }}</span>
      <n-button size="tiny" :type="view === 'list' ? 'primary' : 'default'" :secondary="view !== 'list'"
                @click="view = 'list'">
        {{ $t('GraphImport.preview.map.list') }}
      </n-button>
      <n-button size="tiny" :type="view === 'diagram' ? 'primary' : 'default'" :secondary="view !== 'diagram'"
                :loading="loadingDiagram" @click="onDiagramClick">
        {{ $t('GraphImport.preview.map.diagram') }}
      </n-button>
    </n-space>
    <p v-if="diagramFailed" class="small text-body-secondary">{{ $t('GraphImport.preview.map.diagramFailed') }}</p>

    <component
      :is="diagram"
      v-if="view === 'diagram' && diagram"
      :text="diagramText"
      id-prefix="graph-import-map"
      @failed="onDiagramFailed"
    />
    <n-list v-else size="small">
      <n-list-item v-for="sheet in sheets" :key="sheet.name">
        <strong>{{ sheet.name }}</strong>
        <span class="small text-body-secondary">
          —
          {{
            $t('GraphImport.preview.map.sheet', {
              class: sheet.class_label ?? $t('GraphImport.preview.map.noClass'),
              rows: sheet.row_count ?? 0,
              resources: sheet.resources ?? 0
            })
          }}
        </span>
      </n-list-item>
      <n-list-item v-for="(link, index) in links" :key="'link' + index">
        <span class="small">{{ linkText(link) }}</span>
      </n-list-item>
    </n-list>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, inject, ref, shallowRef, type Component } from 'vue'
import type { PreviewSheetDTO, SheetLinkDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { edgeLabel, sheetMapDiagram } from '../preview'

/** The module whose bundle carries Mermaid, and the component it registers to draw with it. */
const DIAGRAM_MODULE = 'opensilex-ai-import'
const DIAGRAM_COMPONENT = 'opensilex-ai-import-MermaidDiagram'

//#region Public
const props = defineProps<{
  sheets: PreviewSheetDTO[]
  links: SheetLinkDTO[]
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
/** Where globally registered components are found once another module is loaded. */
const appContext = getCurrentInstance()?.appContext
//#endregion

//#region Data & Computed
const view = ref<'list' | 'diagram'>('list')
const diagram = shallowRef<Component>()
const loadingDiagram = ref(false)
const diagramFailed = ref(false)

const diagramText = computed(() => sheetMapDiagram(props.sheets, props.links, t))
//#endregion

//#region Event handlers
/**
 * Draws the map. The assistant's bundle is loaded the first time; a bundle that does not load, or that
 * does not register the component, leaves the list in place with a word of why.
 */
async function onDiagramClick() {
  if (!diagram.value) {
    loadingDiagram.value = true
    try {
      await $opensilex.loadModule(DIAGRAM_MODULE)
      diagram.value = appContext?.components[DIAGRAM_COMPONENT]
    } catch {
      diagram.value = undefined
    } finally {
      loadingDiagram.value = false
    }
  }
  diagramFailed.value = !diagram.value
  view.value = diagram.value ? 'diagram' : 'list'
}

function onDiagramFailed() {
  diagramFailed.value = true
  view.value = 'list'
}
//#endregion

//#region Methods
function linkText(link: SheetLinkDTO): string {
  const key = link.kind === 'INSTANCE' ? 'GraphImport.preview.map.toExisting' : 'GraphImport.preview.map.between'
  return t(key, { from: link.from ?? '', to: link.to ?? '', what: edgeLabel(link, t) })
}

function t(key: string, params?: { [name: string]: unknown }): string {
  return String($opensilex.$i18n.t(key, params))
}
//#endregion
//#endregion
</script>
