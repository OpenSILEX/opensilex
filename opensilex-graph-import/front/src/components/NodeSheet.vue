<template>
  <!--
    One resource of the import: its values merged across sheets, each with the sheet and row it comes
    from and what the import would do with it, what the existing resource holds instead, and the
    problems of the file about it.
  -->
  <n-drawer v-model:show="show" width="560" placement="right">
    <n-drawer-content :title="resource?.name ?? ''" closable>
      <p v-if="loading" class="text-body-secondary">{{ $t('GraphImport.preview.loading') }}</p>
      <template v-else-if="resource">
        <n-space :size="6" align="center" class="mb-2">
          <n-tag size="small" :bordered="false" :type="nodeStateType(resource.state)">
            {{ $t('GraphImport.preview.state.' + resource.state) }}
          </n-tag>
          <n-tag v-if="resource.to_fix" size="small" :bordered="false" type="warning">
            {{ $t('GraphImport.preview.grid.toFix') }}
          </n-tag>
          <span class="small text-body-secondary">{{ resource.class_labels }}</span>
        </n-space>

        <p v-if="resource.existing" class="small mb-1">
          {{ $t('GraphImport.preview.node.existing') }} <code>{{ resource.existing }}</code>
        </p>
        <p v-if="resource.reason" class="small text-danger mb-1">{{ reportText($opensilex, resource.reason) }}</p>
        <ul v-if="resource.candidates?.length" class="small mb-2">
          <li v-for="candidate in resource.candidates" :key="candidate"><code>{{ candidate }}</code></li>
        </ul>
        <p v-if="resource.missing?.length" class="small text-warning-emphasis mb-2">
          {{ $t('GraphImport.preview.node.missing', { properties: resource.missing.join(', ') }) }}
        </p>
        <p class="small text-body-secondary">
          {{ $t('GraphImport.preview.node.rows', { rows: resource.rows?.map(placeOf).join(' ; ') ?? '' }) }}
        </p>

        <h6 class="mt-3">{{ $t('GraphImport.preview.node.values') }}</h6>
        <p v-if="!resource.values?.length" class="small text-body-secondary">{{ $t('GraphImport.preview.node.none') }}</p>
        <n-list size="small">
          <n-list-item v-for="(value, index) in resource.values ?? []" :key="index">
            <div class="d-flex justify-content-between gap-2">
              <span class="fw-semibold">{{ value.property_label }}</span>
              <n-tag size="small" :bordered="false" :type="valueStateType(value.state)">
                {{ $t('GraphImport.preview.value.' + value.state) }}
              </n-tag>
            </div>
            <div>
              {{ value.raw }}
              <span v-if="value.value && value.value !== value.raw" class="text-body-secondary small">→ {{ value.value }}</span>
              <span v-if="value.language" class="text-body-secondary small">@{{ value.language }}</span>
            </div>
            <div v-if="value.target" class="small text-body-secondary">
              {{ $t('GraphImport.preview.node.target') }} <code>{{ value.target }}</code>
            </div>
            <div v-if="value.held?.length" class="small text-danger">
              {{ $t('GraphImport.preview.node.held', { values: value.held.join(', ') }) }}
            </div>
            <div class="small text-body-secondary">{{ placeOf(value.cell) }}</div>
          </n-list-item>
        </n-list>

        <template v-if="resource.problems?.length">
          <h6 class="mt-3">{{ $t('GraphImport.preview.node.problems') }}</h6>
          <ul class="small">
            <li
              v-for="(problem, index) in resource.problems"
              :key="index"
              :class="problem.severity === 'ERROR' ? 'text-danger' : 'text-warning-emphasis'"
            >
              {{ reportText($opensilex, problem.message) }}
            </li>
          </ul>
        </template>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import type { CellRefDTO, GraphImportService, PreviewNodeDTO } from '../lib'
import { GRAPH_IMPORT_SERVICE, type OpenSilexPlugin } from '../opensilexPlugin'
import { nodeStateType, valueStateType } from '../preview'
import { reportText } from '../reasons'

//#region Public
const props = defineProps<{
  sessionId: string
  /** The index of the resource to show, as the rows of the grid give it. */
  node?: number
}>()

const show = defineModel<boolean>('show', { default: false })
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const service = $opensilex.getService<GraphImportService>(GRAPH_IMPORT_SERVICE)
//#endregion

//#region Data & Computed
const resource = ref<PreviewNodeDTO>()
const loading = ref(false)
//#endregion

//#region Hooks & watchers
watch(
  () => [show.value, props.node],
  () => {
    if (show.value && props.node !== undefined) {
      load(props.node)
    }
  }
)
//#endregion

//#region Methods
async function load(node: number) {
  loading.value = true
  try {
    const http = await service.previewNode(props.sessionId, node)
    resource.value = http.response.result
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    loading.value = false
  }
}

/** Where a cell is, as the user finds it: its sheet and its row among the rows of values, from 1. */
function placeOf(cell: CellRefDTO | undefined): string {
  if (!cell) {
    return ''
  }
  return $opensilex.$i18n.t('GraphImport.preview.node.place', { sheet: cell.sheet ?? '', row: (cell.row ?? 0) + 1 })
}
//#endregion
//#endregion
</script>
