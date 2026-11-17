<template>
  <div class="graph-import-create">
    <p class="text-body-secondary small">{{ $t('GraphImport.creation.help') }}</p>
    <p v-if="plan && !plan.enabled" class="small text-warning-emphasis">{{ $t('GraphImport.creation.disabled') }}</p>

    <div class="d-flex justify-content-end mb-2">
      <n-button size="small" tertiary :disabled="busy" @click="load(true)">{{ $t('GraphImport.creation.refresh') }}</n-button>
    </div>

    <p v-if="!plan && busy" class="text-body-secondary">{{ $t('GraphImport.creation.loading') }}</p>
    <CreationStepRow
      v-for="step in plan?.steps ?? []"
      :key="step.step"
      :step="step"
      :outcome="outcomes[step.step!]"
      :enabled="plan?.enabled ?? false"
      :busy="busy"
      @check="onCreationStepRowCheck"
      @run="onCreationStepRowRun"
    />

    <div v-if="plan?.unsupported?.length" class="small mb-2">
      <div class="fw-semibold">{{ $t('GraphImport.creation.unsupported') }}</div>
      <ul class="mb-0">
        <li v-for="count in plan.unsupported" :key="count.class_uri">{{ count.label }} : {{ count.count }}</li>
      </ul>
    </div>

    <div v-if="plan?.history?.length" class="small">
      <div class="fw-semibold">{{ $t('GraphImport.creation.history') }}</div>
      <ul class="mb-0">
        <li v-for="(record, index) in plan.history" :key="index">
          {{ $t('GraphImport.creation.record', { step: $t('GraphImport.creation.steps.' + record.step), count: record.created, at: when(record.at) }) }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, watch } from 'vue'
import type { CreationOutcomeDTO, CreationPlanDTO, CreationStepDTO, GraphImportService } from '../lib'
import { GRAPH_IMPORT_SERVICE, type OpenSilexPlugin } from '../opensilexPlugin'
import CreationStepRow from './CreationStepRow.vue'

//#region Public
const props = defineProps<{
  sessionId: string
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const service = $opensilex.getService<GraphImportService>(GRAPH_IMPORT_SERVICE)
//#endregion

//#region Data & Computed
const plan = ref<CreationPlanDTO>()
/** What the last check or run of each step gave, by step. */
const outcomes = ref<Record<string, CreationOutcomeDTO>>({})
const busy = ref(false)
//#endregion

//#region Hooks & watchers
// The instance may have changed since the preview: the plan is read afresh on arrival.
onMounted(() => load(true))
watch(() => props.sessionId, () => load(true))
//#endregion

//#region Event handlers
async function onCreationStepRowCheck(step: CreationStepDTO) {
  await send(step, () => service.checkCreation({ session_id: props.sessionId, step: step.step!, expected: step.count ?? 0 }))
}

async function onCreationStepRowRun(step: CreationStepDTO) {
  await send(step, () => service.createStep({ session_id: props.sessionId, step: step.step!, expected: step.count ?? 0 }))
}
//#endregion

//#region Methods
async function load(afresh: boolean) {
  busy.value = true
  try {
    const http = await service.getCreationPlan(props.sessionId, afresh)
    plan.value = http.response.result
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    busy.value = false
  }
}

/** Sends a check or a run of a step, and shows what it gave and the plan as it now stands. */
async function send(step: CreationStepDTO, request: () => Promise<{ response: { result?: CreationOutcomeDTO } }>) {
  if (busy.value) {
    return
  }
  busy.value = true
  try {
    const http = await request()
    const outcome = http.response.result
    if (outcome) {
      outcomes.value = { ...outcomes.value, [step.step!]: outcome }
      plan.value = outcome.plan
    }
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    busy.value = false
  }
}

function when(at: string | undefined): string {
  return at ? new Date(at).toLocaleString() : ''
}
//#endregion
//#endregion
</script>
