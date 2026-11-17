<template>
  <!--
    What the import would do to the instance, without writing anything: figures that filter the grid,
    the map of the sheets, each sheet a page at a time, each resource in a drawer, and the whole as
    Turtle. Asked afresh on arrival, so that it shows what the instance holds now.
  -->
  <div class="graph-import-preview">
    <p class="text-body-secondary small">{{ $t('GraphImport.preview.help') }}</p>

    <div class="d-flex align-items-center flex-wrap gap-2 mb-2">
      <PreviewCounters :counts="preview?.counts" :chosen="filter" @choose="onPreviewCountersChoose" />
      <n-button size="small" tertiary :loading="loading" @click="onAfreshClick">
        {{ $t('GraphImport.preview.afresh') }}
      </n-button>
      <n-button size="small" secondary :disabled="!preview" @click="onTurtleClick">
        {{ $t('GraphImport.preview.turtle') }}
      </n-button>
    </div>
    <p v-if="loading && !preview" class="text-body-secondary">{{ $t('GraphImport.preview.loading') }}</p>
    <p v-if="preview?.problem_total" class="small text-warning-emphasis">
      {{ $t('GraphImport.preview.problems', { count: preview.problem_total }) }}
    </p>

    <template v-if="preview">
      <DataSheetCounters v-if="preview.data_sheets?.length" :sheets="preview.data_sheets" />
      <SheetMap :sheets="preview.sheets ?? []" :links="preview.links ?? []" />

      <n-space :size="6" align="center" class="mb-2">
        <span class="fw-semibold">{{ $t('GraphImport.preview.sheets') }}</span>
        <n-button
          v-for="candidate in preview.sheets ?? []"
          :key="candidate.name"
          size="small"
          :type="candidate.name === sheet ? 'primary' : 'default'"
          :secondary="candidate.name !== sheet"
          @click="sheet = candidate.name"
        >
          {{ candidate.name }}
        </n-button>
      </n-space>
      <SheetPreview
        v-if="sheet"
        :session-id="sessionId"
        :sheet="sheet"
        :state="filter.state"
        :to-fix="filter.toFix"
        :stamp="stamp"
        @open="onSheetPreviewOpen"
      />
    </template>

    <NodeSheet v-model:show="nodeShown" :session-id="sessionId" :node="node" />
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, watch } from 'vue'
import type { GraphImportService, PreviewDTO } from '../lib'
import { GRAPH_IMPORT_SERVICE, SESSIONS_PATH, type OpenSilexPlugin } from '../opensilexPlugin'
import type { PreviewFilter } from '../preview'
import NodeSheet from './NodeSheet.vue'
import DataSheetCounters from './DataSheetCounters.vue'
import PreviewCounters from './PreviewCounters.vue'
import SheetMap from './SheetMap.vue'
import SheetPreview from './SheetPreview.vue'

//#region Public
const props = defineProps<{
  sessionId: string
  /** What the import is called, which names the Turtle file. */
  title?: string
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const service = $opensilex.getService<GraphImportService>(GRAPH_IMPORT_SERVICE)
//#endregion

//#region Data & Computed
const preview = ref<PreviewDTO>()
const loading = ref(false)
/** Changes each time the preview is worked out again, so that the grid reads its page again. */
const stamp = ref(0)
const filter = ref<PreviewFilter>({ toFix: false })
/** The sheet the grid shows. */
const sheet = ref<string>()
/** The resource the drawer shows. */
const node = ref<number>()
const nodeShown = ref(false)
//#endregion

//#region Hooks & watchers
onMounted(() => load(true))
watch(() => props.sessionId, () => load(true))
//#endregion

//#region Event handlers
function onPreviewCountersChoose(chosen: PreviewFilter) {
  filter.value = chosen
}

async function onAfreshClick() {
  await load(true)
}

async function onTurtleClick() {
  const path = SESSIONS_PATH + '/' + encodeURIComponent(props.sessionId) + '/preview/turtle'
  const name = (props.title ?? 'import').replace(/\.[A-Za-z0-9]{1,5}$/, '') + '-preview'
  try {
    await $opensilex.downloadFilefromService(path, name, 'ttl')
  } catch {
    // The host's helper has shown the error already.
  }
}

function onSheetPreviewOpen(index: number) {
  node.value = index
  nodeShown.value = true
}
//#endregion

//#region Methods
/** Works the preview out, afresh to read the instance again; keeps the sheet shown when it is still there. */
async function load(afresh: boolean) {
  loading.value = true
  try {
    const http = await service.preview(props.sessionId, afresh)
    preview.value = http.response.result
    const names = (preview.value?.sheets ?? []).map((candidate) => candidate.name)
    if (!sheet.value || !names.includes(sheet.value)) {
      sheet.value = names[0]
    }
    stamp.value++
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    loading.value = false
  }
}
//#endregion
//#endregion
</script>
