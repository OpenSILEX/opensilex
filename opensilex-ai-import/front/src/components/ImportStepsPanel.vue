<template>
  <!--
    The order an import is declared in, one line per step: an experiment first, then its factors and
    their levels, then the objects and the observations. A step that has to wait says what for, and
    its button stays off until then.
  -->
  <opensilex-Card v-if="visible.length" label="AiImport.steps.title" :no-footer="true">
    <template #body>
      <p class="text-body-secondary small">{{ t('AiImport.steps.hint') }}</p>
      <n-space class="mb-2" align="center">
        <n-button size="tiny" :secondary="view !== 'list'" :type="view === 'list' ? 'primary' : 'default'"
                  @click="view = 'list'">{{ t('AiImport.steps.viewList') }}</n-button>
        <n-button size="tiny" :secondary="view !== 'diagram'" :type="view === 'diagram' ? 'primary' : 'default'"
                  @click="onDiagramClick">{{ t('AiImport.steps.viewDiagram') }}</n-button>
        <n-button v-if="view === 'diagram'" size="tiny" quaternary @click="onCopyClick">
          {{ t('AiImport.steps.copyMermaid') }}
        </n-button>
      </n-space>

      <!--
        The same steps as a diagram: what is done, what can be done and what waits, with the arrows of
        the order they are declared in. A step that can be done is clicked as it is declared in the list.
      -->
      <div v-if="view === 'diagram'" class="ai-import-diagram">
        <p v-if="diagramError" class="text-body-secondary small">{{ diagramError }}</p>
        <MermaidDiagram
          :text="diagramText"
          id-prefix="ai-import-steps"
          @rendered="onMermaidDiagramRendered"
          @failed="onMermaidDiagramFailed"
        />
      </div>
      <n-list v-else>
        <n-list-item v-for="step in visible" :key="step.step">
          <n-space align="center" justify="space-between">
            <n-space align="center">
              <n-tag size="small" :type="tagType(step.status)">
                {{ t('AiImport.steps.status_' + step.status) }}
              </n-tag>
              <strong>{{ t(step.label_key ?? '') }}</strong>
              <span v-if="step.status !== 'NOT_APPLICABLE'" class="text-body-secondary small">
                {{ t('AiImport.steps.count', { done: step.done, total: step.total }) }}
              </span>
              <span v-if="step.status === 'BLOCKED'" class="small text-body-secondary">
                {{ t('AiImport.steps.waitingFor') }}
                {{ (step.blocked_by ?? []).map((name) => labelOf(name)).join(', ') }}
              </span>
            </n-space>
            <n-button
              v-if="step.status === 'POSSIBLE'"
              size="small"
              type="primary"
              secondary
              @click="emit('declare', step)"
            >
              {{ t('AiImport.steps.declare') }}
            </n-button>
          </n-space>
        </n-list-item>
      </n-list>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import type { ImportStepDTO } from '../lib'
import MermaidDiagram from './MermaidDiagram.vue'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { stepsDiagram } from '../stepsDiagram'
import { useImportSteps } from '../useImportSteps'

//#region Public
const emit = defineEmits<{
  /** The user wants to declare this step: the page opens what declares it. */
  declare: [step: ImportStepDTO]
}>()

const props = defineProps<{
  sessionId: string
  /** Changes whenever the report is recomputed, which is when a step may have been done. */
  reportStamp?: string
}>()
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const { steps, load } = useImportSteps($opensilex, () => props.sessionId)
//#endregion

//#region Data & Computed
/**
 * A step the files say nothing of is left out, so that the path only shows what the import needs.
 */
const visible = computed(() => steps.value.filter((step) => step.status !== 'NOT_APPLICABLE'))
//#endregion

/** The list, or the diagram: the same steps either way. */
const view = ref<'list' | 'diagram'>('list')
const diagramError = ref('')

const diagramText = computed(() =>
  stepsDiagram(
    steps.value,
    (step) => t(step.label_key ?? ''),
    (step) => t('AiImport.steps.count', { done: step.done, total: step.total })
  )
)
//#endregion

//#region Hooks & watchers
onMounted(load)
watch(() => props.reportStamp, load)
//#endregion

//#region Event handlers
function onDiagramClick() {
  view.value = 'diagram'
}

function onMermaidDiagramRendered(host: HTMLElement) {
  diagramError.value = ''
  makeDeclarable(host)
}

function onMermaidDiagramFailed(error: unknown) {
  diagramError.value = t('AiImport.steps.diagramFailed')
  console.warn('Could not draw the diagram of the steps', error)
}

/**
 * Copies the Mermaid text, so the diagram can be put in a document or a merge request; when the
 * browser refuses, the text is shown for the user to copy.
 */
async function onCopyClick() {
  try {
    await navigator.clipboard.writeText(diagramText.value)
  } catch (error) {
    diagramError.value = diagramText.value
  }
}
//#endregion

//#region Methods
/**
 * Lets a step that can be done be clicked in the diagram as it is declared from the list.
 */
function makeDeclarable(host: HTMLElement) {
  for (const step of steps.value.filter((candidate) => candidate.status === 'POSSIBLE')) {
    const node = host.querySelector<SVGElement>(`[id^="flowchart-${step.step}-"]`)
    if (node) {
      node.style.cursor = 'pointer'
      node.addEventListener('click', () => emit('declare', step))
    }
  }
}

function t(key: string, params?: { [name: string]: unknown }): string {
  return String($opensilex.$i18n.t(key, params))
}

function labelOf(name: string): string {
  const step = steps.value.find((candidate) => candidate.step === name)
  return t(step?.label_key ?? '')
}

function tagType(status?: string): 'success' | 'info' | 'warning' | 'default' {
  switch (status) {
    case 'DONE':
      return 'success'
    case 'POSSIBLE':
      return 'info'
    case 'BLOCKED':
      return 'warning'
    default:
      return 'default'
  }
}
//#endregion

//#endregion
</script>
