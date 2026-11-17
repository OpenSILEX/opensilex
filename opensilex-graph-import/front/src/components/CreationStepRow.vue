<template>
  <div class="graph-import-creation-step">
    <div class="d-flex align-items-center gap-2 flex-wrap">
      <strong>{{ $t('GraphImport.creation.steps.' + step.step) }}</strong>
      <n-tag size="small" :type="statusType" :bordered="false">{{ $t('GraphImport.creation.status.' + step.status) }}</n-tag>
      <span class="small text-body-secondary">
        {{ $t('GraphImport.creation.counts', { ready: step.count ?? 0, blocked: step.blocked_count ?? 0, done: step.done ?? 0 }) }}
      </span>
      <span v-if="step.after?.length" class="small text-body-secondary">
        {{ $t('GraphImport.creation.after', { steps: afterText }) }}
      </span>
      <n-space :size="6" class="ms-auto">
        <n-button size="small" :disabled="busy || !step.count" @click="emit('check', step)">
          {{ $t('GraphImport.creation.check') }}
        </n-button>
        <n-button
          size="small"
          type="primary"
          :disabled="busy || !enabled || !step.allowed || !step.count"
          @click="emit('run', step)"
        >
          {{ $t('GraphImport.creation.run', { count: step.count ?? 0 }) }}
        </n-button>
      </n-space>
    </div>
    <p v-if="!step.allowed" class="small text-warning-emphasis mb-1">{{ $t('GraphImport.creation.notAllowed') }}</p>
    <div v-if="step.data" class="d-flex align-items-center flex-wrap gap-2 small mt-1">
      <n-tag v-if="step.data.waiting" size="small" type="info" :bordered="false">
        {{ $t('GraphImport.measure.counts.waiting', { count: step.data.waiting }) }}
      </n-tag>
      <n-tag v-if="step.data.not_available" size="small" :bordered="false">
        {{ $t('GraphImport.measure.counts.notAvailable', { count: step.data.not_available }) }}
      </n-tag>
      <n-tag v-if="step.data.refused" size="small" type="warning" :bordered="false">
        {{ $t('GraphImport.measure.counts.refused', { count: step.data.refused }) }}
      </n-tag>
      <span v-if="step.data.refused" class="text-body-secondary">{{ $t('GraphImport.creation.dataRefusedHelp') }}</span>
    </div>

    <n-collapse v-if="step.ready?.length || step.blocked?.length" class="mt-1">
      <n-collapse-item v-if="step.ready?.length" :title="$t('GraphImport.creation.readyTitle', { count: step.ready.length })" name="ready">
        <ul class="small mb-0">
          <li v-for="node in step.ready" :key="node.index">{{ node.name }}</li>
        </ul>
      </n-collapse-item>
      <n-collapse-item v-if="step.blocked?.length" :title="$t('GraphImport.creation.blockedTitle', { count: step.blocked.length })" name="blocked">
        <ul class="small mb-0">
          <li v-for="(node, index) in step.blocked" :key="index">
            <strong v-if="node.name">{{ node.name }} · </strong>{{ reportText($opensilex, node.reason) }}
          </li>
        </ul>
      </n-collapse-item>
    </n-collapse>

    <div v-if="outcome" class="mt-2 small">
      <p v-if="outcome.created_count" class="text-success mb-1">
        {{ $t('GraphImport.creation.created', { count: outcome.created_count }) }}
      </p>
      <p v-else-if="!outcome.problems?.length" class="text-success mb-1">{{ $t('GraphImport.creation.checked') }}</p>
      <ul v-if="outcome.problems?.length" class="text-danger mb-1">
        <li v-for="(problem, index) in outcome.problems" :key="index">
          <strong v-if="problem.name">{{ problem.name }} · </strong>{{ reportText($opensilex, problem.reason) }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { CreationOutcomeDTO, CreationStepDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { reportText } from '../reasons'

/** The colours of the tags, as naive-ui names them. */
type TagType = 'default' | 'success' | 'info' | 'warning'

const STATUS_TYPES: Record<string, TagType> = { READY: 'info', BLOCKED: 'warning', DONE: 'success', EMPTY: 'default' }

//#region Public
const emit = defineEmits<{
  /** Check the step without writing. */
  check: [step: CreationStepDTO]
  /** Create the resources of the step that are ready. */
  run: [step: CreationStepDTO]
}>()

const props = defineProps<{
  step: CreationStepDTO
  /** What the last check or run of this step gave; undefined before. */
  outcome?: CreationOutcomeDTO
  /** Whether the platform lets the assistant write. */
  enabled: boolean
  busy: boolean
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const statusType = computed<TagType>(() => STATUS_TYPES[props.step.status ?? ''] ?? 'default')

const afterText = computed(() =>
  (props.step.after ?? []).map((name) => $opensilex.$i18n.t('GraphImport.creation.steps.' + name)).join(', ')
)
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-creation-step {
  border: 1px solid var(--bs-border-color, #dee2e6);
  border-radius: 0.5rem;
  padding: 0.75rem;
  margin-bottom: 0.75rem;
}
</style>
