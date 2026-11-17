<template>
  <div class="graph-import-page p-3">
    <h3 class="mb-1">{{ $t('GraphImport.title') }}</h3>
    <p class="text-body-secondary">{{ $t('GraphImport.description') }}</p>

    <!-- The three moments of an import: the last two need an open import. -->
    <n-space :size="8" class="mb-3" align="center">
      <n-button
        v-for="(step, index) in steps"
        :key="step"
        size="small"
        :type="step === currentStep ? 'primary' : 'default'"
        :secondary="step !== currentStep"
        :disabled="!isAvailable(step)"
        @click="currentStep = step"
      >
        {{ index + 1 }}. {{ $t('GraphImport.steps.' + step) }}
      </n-button>
    </n-space>

    <template v-if="!session">
      <FileDrop :busy="busy" @chosen="onFileDropChosen" />

      <h5 class="mt-4">{{ $t('GraphImport.stored.title') }}</h5>
      <StoredImportList
        :imports="storedImports"
        :resuming="resuming"
        @resume="onStoredImportListResume"
        @remove="onStoredImportListRemove"
      />
    </template>

    <template v-else-if="currentStep === 'understand'">
      <div class="d-flex align-items-center justify-content-between mb-2">
        <h5 class="mb-0">{{ session.title }}</h5>
        <n-button size="small" tertiary @click="onClose">{{ $t('GraphImport.session.close') }}</n-button>
      </div>
      <UnderstandStep :session-id="session.session_id" />
    </template>

    <template v-else-if="currentStep === 'preview'">
      <div class="d-flex align-items-center justify-content-between mb-2">
        <h5 class="mb-0">{{ session.title }}</h5>
        <n-button size="small" tertiary @click="onClose">{{ $t('GraphImport.session.close') }}</n-button>
      </div>
      <PreviewStep :session-id="session.session_id" :title="session.title" />
    </template>

    <template v-else-if="currentStep === 'create'">
      <div class="d-flex align-items-center justify-content-between mb-2">
        <h5 class="mb-0">{{ session.title }}</h5>
        <n-button size="small" tertiary @click="onClose">{{ $t('GraphImport.session.close') }}</n-button>
      </div>
      <CreateStep :session-id="session.session_id" />
    </template>

    <template v-else>
      <div class="d-flex align-items-center justify-content-between mb-2">
        <h5 class="mb-0">{{ session.title }}</h5>
        <n-button size="small" tertiary @click="onClose">{{ $t('GraphImport.session.close') }}</n-button>
      </div>
      <p class="text-body-secondary small">{{ $t('GraphImport.session.saved') }}</p>

      <h6>{{ $t('GraphImport.files.title') }}</h6>
      <ImportFileList
        :files="session.files"
        :busy="busy"
        @add="onImportFileListAdd"
        @remove="onImportFileListRemove"
      />

      <h6 class="mt-4">{{ $t('GraphImport.sheets.title', { count: session.sheets.length }) }}</h6>
      <SheetOverview :sheets="session.sheets" @choose-rows="onSheetOverviewChooseRows" />
      <SheetLayoutDrawer
        v-model:show="layoutShown"
        :session-id="session.session_id"
        :sheet="layoutSheet"
        @applied="onSheetLayoutDrawerApplied"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
import type { ErrorDTO, GraphImportFileDTO, GraphImportService, GraphImportSessionDTO, StoredImportDTO } from '../lib'
import { GRAPH_IMPORT_SERVICE, SESSIONS_PATH, type OpenSilexPlugin } from '../opensilexPlugin'
import FileDrop from './FileDrop.vue'
import ImportFileList from './ImportFileList.vue'
import CreateStep from './CreateStep.vue'
import PreviewStep from './PreviewStep.vue'
import SheetLayoutDrawer from './SheetLayoutDrawer.vue'
import SheetOverview from './SheetOverview.vue'
import StoredImportList from './StoredImportList.vue'
import UnderstandStep from './UnderstandStep.vue'

/** The three moments of an import, in order. */
type ImportStep = 'upload' | 'understand' | 'preview' | 'create'

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const service = $opensilex.getService<GraphImportService>(GRAPH_IMPORT_SERVICE)
//#endregion

//#region Data & Computed
const steps: ImportStep[] = ['upload', 'understand', 'preview', 'create']
const currentStep = ref<ImportStep>('upload')

/** The open import; the page owns it, the panels only show it. */
const session = ref<GraphImportSessionDTO>()
const storedImports = ref<StoredImportDTO[]>([])
const busy = ref(false)
/** The stored import being read back. */
const resuming = ref<string>()
/** The sheet whose header row and first row of values are being chosen. */
const layoutSheet = ref<string>()
const layoutShown = ref(false)
//#endregion

//#region Hooks & watchers
onMounted(loadStoredImports)
//#endregion

//#region Event handlers
async function onFileDropChosen(file: File) {
  await whileBusy(async () => {
    const opened = await upload<GraphImportSessionDTO>(SESSIONS_PATH, file)
    if (opened) {
      session.value = opened
    }
  })
}

async function onStoredImportListResume(sessionId: string) {
  resuming.value = sessionId
  try {
    const http = await service.getSession(sessionId)
    session.value = http.response.result
  } catch (error) {
    $opensilex.errorHandler(error)
    await loadStoredImports()
  } finally {
    resuming.value = undefined
  }
}

async function onStoredImportListRemove(sessionId: string) {
  try {
    await service.deleteSession(sessionId)
  } catch (error) {
    $opensilex.errorHandler(error)
  }
  await loadStoredImports()
}

async function onImportFileListAdd(file: File) {
  const sessionId = session.value.session_id
  await whileBusy(async () => {
    const files = await upload<GraphImportFileDTO[]>(SESSIONS_PATH + '/' + encodeURIComponent(sessionId) + '/files', file)
    if (files) {
      await reloadSession(sessionId)
    }
  })
}

async function onImportFileListRemove(fileId: string) {
  const sessionId = session.value.session_id
  await whileBusy(async () => {
    await service.removeFile(sessionId, fileId)
    await reloadSession(sessionId)
  })
}

function onSheetOverviewChooseRows(sheet: string) {
  layoutSheet.value = sheet
  layoutShown.value = true
}

function onSheetLayoutDrawerApplied(laidOut: GraphImportSessionDTO) {
  session.value = laidOut
}

async function onClose() {
  session.value = undefined
  currentStep.value = 'upload'
  await loadStoredImports()
}
//#endregion

//#region Methods
/** Upload is always open; understanding and the preview need an open import. */
function isAvailable(step: ImportStep): boolean {
  return step === 'upload' || session.value !== undefined
}

async function loadStoredImports() {
  try {
    const http = await service.listSessions()
    storedImports.value = http.response.result ?? []
  } catch (error) {
    // A convenience of the first screen: without it, a new file can still be opened.
    storedImports.value = []
    $opensilex.errorHandler(error)
  }
}

/**
 * Sends a file. The host's helper answers a refused file without rejecting, so the status is read
 * here: a refusal shows the server's reason and gives nothing back.
 */
async function upload<T>(path: string, file: File): Promise<T | undefined> {
  const answer = await $opensilex.uploadFileToService<T>(path, { file })
  if (answer.metadata.status >= 400) {
    const error = answer.result as ErrorDTO | undefined
    $opensilex.showErrorToast([error?.title, error?.message].filter(Boolean).join(' ') || String(answer.metadata.status))
    return undefined
  }
  return answer.result as T
}

/** Reads the import again, so that its files and sheets show what the server now holds. */
async function reloadSession(sessionId: string) {
  const http = await service.getSession(sessionId)
  session.value = http.response.result
}

/** Runs a step that changes the import, with the buttons held while it runs. */
async function whileBusy(step: () => Promise<void>) {
  busy.value = true
  try {
    await step()
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    busy.value = false
  }
}
//#endregion
//#endregion
</script>
