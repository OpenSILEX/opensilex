<template>
  <div class="graph-import-understand">
    <p class="text-body-secondary small">{{ $t('GraphImport.understand.help') }}</p>

    <n-form class="graph-import-understand__experiment mb-3" label-placement="top" size="small">
      <opensilex-ExperimentSelector
        :experiments="experiment"
        label="GraphImport.understand.experiment"
        @update:experiments="onExperimentSelectorUpdate"
      />
      <opensilex-ProvenanceSelector
        v-if="hasData"
        :provenances="provenance"
        :experiment="experiment"
        label="GraphImport.measure.provenance"
        @update:provenances="onProvenanceSelectorUpdate"
      />
    </n-form>

    <GraphCheckSummary :validation="validation" :loading="checking" />

    <p v-for="(note, index) in interpretation?.notes ?? []" :key="'note' + index" class="small text-warning-emphasis">
      {{ reportText($opensilex, note) }}
    </p>

    <div class="graph-import-understand__views mb-2" role="group" :aria-label="$t('GraphImport.understandGraph.viewLabel')">
      <n-button size="small" :type="view === 'table' ? 'primary' : 'default'" :secondary="view !== 'table'" @click="onViewClick('table')">
        {{ $t('GraphImport.understandGraph.table') }}
      </n-button>
      <n-button size="small" :type="view === 'graph' ? 'primary' : 'default'" :secondary="view !== 'graph'" @click="onViewClick('graph')">
        {{ $t('GraphImport.understandGraph.graph') }}
      </n-button>
    </div>

    <p v-if="loading && !interpretation" class="text-body-secondary">{{ $t('GraphImport.understand.loading') }}</p>
    <UnderstandGraph
      v-if="graphShown"
      v-show="view === 'graph'"
      :session-id="sessionId"
      :interpretation="interpretation"
      :validation="validation"
      :busy="loading || checking"
      @decide="onDecide"
      @forget="onForget"
      @read="onRead"
      @declare="onDeclare"
      @edit-declared="onEditDeclared"
    />
    <template v-if="view === 'table'">
      <SheetUnderstanding
        v-for="sheet in interpretation?.sheets ?? []"
        :key="sheet.name"
        :sheet="sheet"
        :classes="interpretation?.classes ?? []"
        :check="sheetCheck(validation, sheet.name!)"
        :validation="validation"
        :declared="interpretation?.declared ?? []"
        :busy="loading || checking"
        @decide="(decision) => onDecide(sheet.name!, decision)"
        @forget="onForget(sheet.name!)"
        @read="(column, reading) => onRead(sheet.name!, column, reading)"
        @declare="onDeclare"
        @edit-declared="onEditDeclared"
      />
    </template>

    <DeclaredResourceDrawer
      v-model:show="drawerShown"
      :interpretation="interpretation"
      :resource-id="editedResource"
      :busy="loading || checking"
      @save="onDeclaredResourceDrawerSave"
      @forget="onDeclaredResourceDrawerForget"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import type { DeclaredResourceDTO, GraphImportService, InterpretationDTO, ValidationDTO } from '../lib'
import { GRAPH_IMPORT_SERVICE, type OpenSilexPlugin } from '../opensilexPlugin'
import { isDataSheet, type SheetDecision } from '../decisions'
import { declaredName, LABEL } from '../declared'
import { readView, type UnderstandView, writeView } from '../graphViewState'
import { type DateReading, sheetCheck } from '../problems'
import { reportText } from '../reasons'
import DeclaredResourceDrawer from './DeclaredResourceDrawer.vue'
import GraphCheckSummary from './GraphCheckSummary.vue'
import SheetUnderstanding from './SheetUnderstanding.vue'
import UnderstandGraph from './UnderstandGraph.vue'

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
const interpretation = ref<InterpretationDTO>()
const loading = ref(false)
/** The graph the interpretation gives, checked: refreshed after each decision. */
const validation = ref<ValidationDTO>()
const checking = ref(false)
/** Tables or graph, as the user chose last in this browser. */
const view = ref<UnderstandView>(readView())
/** Whether the graph was shown once: it then stays, so that its selection survives a switch. */
const graphShown = ref(view.value === 'graph')
/** The provenance of the import's data, in the platform's prefixed form, as for the experiment. */
const provenance = computed<string | undefined>(() =>
  interpretation.value?.provenance ? $opensilex.getShortUri(interpretation.value.provenance) : undefined
)

/** Whether a sheet holds data, which then need a provenance. */
const hasData = computed(() => (interpretation.value?.sheets ?? []).some(isDataSheet))

/** Whether the drawer of a declared resource is open, and which resource it shows. */
const drawerShown = ref(false)
const editedResource = ref<string>()

/**
 * The chosen experiment in the platform's prefixed form. The shared selector matches its value against
 * the options it lists, which are prefixed: given the full URI, it shows the URI instead of the name.
 */
const experiment = computed<string | undefined>(() =>
  interpretation.value?.experiment ? $opensilex.getShortUri(interpretation.value.experiment) : undefined
)
//#endregion

//#region Hooks & watchers
onMounted(load)
watch(() => props.sessionId, load)
//#endregion

//#region Event handlers
function onViewClick(chosen: UnderstandView) {
  view.value = chosen
  graphShown.value = graphShown.value || chosen === 'graph'
  writeView(chosen)
}

async function onExperimentSelectorUpdate(selected: string | string[] | undefined) {
  const chosen = Array.isArray(selected) ? selected[0] : selected
  if ((chosen ? $opensilex.getShortUri(chosen) : undefined) === experiment.value) {
    return
  }
  await answer(() => service.chooseExperiment({ session_id: props.sessionId, experiment: chosen }))
}

async function onProvenanceSelectorUpdate(selected: string | string[] | undefined) {
  const chosen = Array.isArray(selected) ? selected[0] : selected
  if ((chosen ? $opensilex.getShortUri(chosen) : undefined) === provenance.value) {
    return
  }
  await answer(() => service.chooseProvenance({ session_id: props.sessionId, provenance: chosen }))
}

async function onDecide(sheet: string, decision: SheetDecision) {
  await answer(() => service.decideSheet({ session_id: props.sessionId, sheet, ...decision }))
}

async function onForget(sheet: string) {
  await answer(() => service.forgetSheetDecisions(props.sessionId, sheet))
}

/**
 * Declares a resource under the name the user gave, then opens it, so that the user fills in what its
 * class needs: the class's properties are known once the assistant has added its sheet.
 */
async function onDeclare(classUri: string, name: string) {
  const declared = await answer(() =>
    service.declareResource({
      session_id: props.sessionId,
      resource: { class_uri: classUri, values: [{ property: LABEL, value: name }] }
    })
  )
  const resource = (interpretation.value?.declared ?? [])
    .filter((candidate) => candidate.class_uri === classUri && declaredName(candidate) === name)
    .pop()
  if (declared && resource?.id) {
    onEditDeclared(resource.id)
  }
}

function onEditDeclared(id: string) {
  editedResource.value = id
  drawerShown.value = true
}

async function onDeclaredResourceDrawerSave(resource: DeclaredResourceDTO) {
  if (await answer(() => service.declareResource({ session_id: props.sessionId, resource }))) {
    drawerShown.value = false
  }
}

async function onDeclaredResourceDrawerForget(id: string) {
  if (await answer(() => service.forgetDeclaredResource(props.sessionId, id))) {
    drawerShown.value = false
  }
}

async function onRead(sheet: string, column: number, reading: DateReading) {
  await check(() => service.readColumn({ session_id: props.sessionId, sheet, column, ...reading }))
}
//#endregion

//#region Methods
async function load() {
  await answer(() => service.getInterpretation(props.sessionId))
}

/**
 * Runs a request whose answer is the interpretation, and shows it. Every decision is answered with
 * the interpretation it leads to, so that the screen shows at once what changed.
 */
async function answer(request: () => Promise<{ response: { result?: InterpretationDTO } }>): Promise<boolean> {
  loading.value = true
  try {
    const http = await request()
    interpretation.value = http.response.result
  } catch (error) {
    $opensilex.errorHandler(error)
    return false
  } finally {
    loading.value = false
  }
  await check(() => service.validate(props.sessionId))
  return true
}

/** Runs a request whose answer is the checked graph, and shows it. */
async function check(request: () => Promise<{ response: { result?: ValidationDTO } }>) {
  checking.value = true
  try {
    const http = await request()
    validation.value = http.response.result
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    checking.value = false
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-understand__experiment {
  max-width: 32rem;
}

.graph-import-understand__views {
  display: flex;
  gap: 0.25rem;
}
</style>
