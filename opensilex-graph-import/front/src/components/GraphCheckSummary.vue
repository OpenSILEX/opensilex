<template>
  <div class="graph-import-check mb-3">
    <div class="d-flex align-items-center flex-wrap gap-2">
      <span class="fw-semibold">{{ $t('GraphImport.check.title') }}</span>
      <template v-if="validation">
        <span class="small text-body-secondary">
          {{
            $t('GraphImport.check.figures', {
              nodes: validation.node_count ?? 0,
              values: validation.value_count ?? 0,
              links: validation.link_count ?? 0
            })
          }}
        </span>
        <n-tag v-if="counted.errors" size="small" type="error" :bordered="false">
          {{ $t('GraphImport.check.errors', { count: counted.errors }) }}
        </n-tag>
        <n-tag v-if="counted.warnings" size="small" type="warning" :bordered="false">
          {{ $t('GraphImport.check.warnings', { count: counted.warnings }) }}
        </n-tag>
        <n-tag v-if="!counted.errors && !counted.warnings" size="small" type="success" :bordered="false">
          {{ $t('GraphImport.check.none') }}
        </n-tag>
      </template>
      <span v-else-if="loading" class="small text-body-secondary">{{ $t('GraphImport.check.loading') }}</span>
    </div>
    <p v-if="isCut" class="small text-body-secondary mb-0">
      {{ $t('GraphImport.check.cut', { shown: validation?.problems?.length ?? 0, total: validation?.total ?? 0 }) }}
    </p>
    <p v-for="(note, index) in shownNotes" :key="index" class="small text-warning-emphasis mb-0">
      {{ reportText($opensilex, note) }}
    </p>
    <n-collapse v-if="foldedNotes.length" class="mt-1">
      <n-collapse-item :title="$t('GraphImport.check.moreNotes', { count: foldedNotes.length })" name="notes">
        <p v-for="(note, index) in foldedNotes" :key="index" class="small text-warning-emphasis mb-0">
          {{ reportText($opensilex, note) }}
        </p>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { ReportMessageDTO, ValidationDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { tally, type Tally } from '../problems'
import { reportText } from '../reasons'

/** How many notes the banner shows open: the others fold, so that the sheets stay near the top. */
const OPEN_NOTES = 3

//#region Public
const props = defineProps<{
  validation?: ValidationDTO
  loading: boolean
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const counted = computed<Tally>(() => tally(props.validation?.counts ?? []))

const shownNotes = computed<ReportMessageDTO[]>(() => (props.validation?.notes ?? []).slice(0, OPEN_NOTES))

const foldedNotes = computed<ReportMessageDTO[]>(() => (props.validation?.notes ?? []).slice(OPEN_NOTES))

const isCut = computed(() => (props.validation?.problems?.length ?? 0) < (props.validation?.total ?? 0))
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-check {
  border-left: 3px solid var(--bs-border-color, #dee2e6);
  padding-left: 0.75rem;
}
</style>
