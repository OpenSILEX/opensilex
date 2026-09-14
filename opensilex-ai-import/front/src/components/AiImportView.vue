<template>
  <div class="container-fluid py-3">
    <h3 class="mb-1">{{ $t('AiImport.title') }}</h3>
    <p class="text-body-secondary">{{ $t('AiImport.description') }}</p>

    <!-- Step one: pick a file. Kept on screen until a session opens, so a failed upload can be retried. -->
    <div v-if="!session" class="card">
      <div class="card-body">
        <h5 class="card-title">{{ $t('AiImport.upload.title') }}</h5>
        <p class="card-text text-body-secondary small">{{ $t('AiImport.upload.hint') }}</p>

        <div
          class="ai-import-drop mb-3"
          :class="{ 'ai-import-drop--over': dragging }"
          @dragover.prevent="dragging = true"
          @dragleave.prevent="dragging = false"
          @drop.prevent="onDrop"
        >
          <input
            ref="fileInput"
            type="file"
            class="d-none"
            accept=".xlsx,.xlsm,.xls"
            @change="onFileChosen"
          />
          <div class="ai-import-drop__row">
            <n-button secondary size="small" @click="fileInput?.click()">
              {{ $t('AiImport.upload.choose') }}
            </n-button>
            <span class="text-body-secondary small">{{ $t('AiImport.upload.drop') }}</span>
          </div>
          <div v-if="file" class="mt-2"><code>{{ file.name }}</code></div>
        </div>

        <div class="row g-2 align-items-end">
          <div class="col-md-4">
            <label class="form-label small mb-1" for="ai-import-profile">
              {{ $t('AiImport.upload.profile') }}
            </label>
            <select id="ai-import-profile" v-model="profileId" class="form-select form-select-sm">
              <option value="">{{ $t('AiImport.upload.profileAuto') }}</option>
              <option v-for="profile in profiles" :key="profile.id" :value="profile.id">
                {{ profile.label }}
              </option>
            </select>
          </div>
          <div class="col-md-auto">
            <n-button type="primary" size="small" :disabled="!file" :loading="busy"
                      @click="startSession">
              {{ $t('AiImport.upload.submit') }}
            </n-button>
          </div>
        </div>

        <div v-if="uploadError" class="alert alert-danger py-2 mt-3 mb-0">{{ uploadError }}</div>

        <div v-if="busy" class="d-flex align-items-center gap-2 mt-3 text-body-secondary">
          <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
          <span>{{ $t('AiImport.upload.analysing') }}</span>
        </div>

        <p class="text-body-secondary small mt-3 mb-0">
          <em>{{ $t('AiImport.upload.noWrite') }}</em>
        </p>
      </div>
    </div>

    <!-- Step two: the conversation beside what the instance holds. -->
    <div v-else class="row g-3">
      <div class="col-xl-6">
        <opensilex-ai-import-AiChatPanel
          :messages="session.messages"
          :busy="asking"
          :session-id="session.session_id"
          :proposals-by-id="proposalsById"
          @ask="ask"
          @applied="onCreated"
          @proposal-cancelled="onProposalCancelled"
        />
      </div>
      <div class="col-xl-6 d-flex flex-column gap-3">
        <opensilex-ai-import-ResolutionReportPanel
          v-if="session.report"
          :report="session.report"
          :revalidating="revalidating"
          @revalidate="revalidate"
          @create="requestCreation"
        />

        <!--
          The variable form the variables screen uses, opened from here rather than reimplemented.
          It brings its component selectors and their inline creation modals with it, so an entity
          or a unit that does not exist yet is created the way it is created everywhere else — and
          a component created by mistake is a permanent entry in this instance's referential, which
          is reason enough not to invent a second form for it.
        -->
        <opensilex-VariableForm
          ref="variableForm"
          createTitle="component.variable.add"
          editTitle="component.variable.edit"
          @onCreate="onVariableCreated"
        />

        <!-- Likewise for a person: the platform's form, prefilled from the file. -->
        <opensilex-PersonForm
          ref="personForm"
          createTitle="component.person.add"
          editTitle="component.person.update"
          @onCreate="onVariableCreated"
        />
        <opensilex-ai-import-MappingPanel
          v-if="session.mapping"
          :mapping="session.mapping"
          @ask="ask"
        />
        <opensilex-ai-import-WorkbookStructurePanel
          v-if="session.structure"
          :structure="session.structure"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'

/**
 * The import assistant page.
 *
 * Owns the session: the upload opens it, a question can change the report, and revalidation
 * refreshes it. The child panels stay presentational.
 */
const $opensilex: any = inject('$opensilex')

const ACCEPTED_EXTENSIONS = ['xlsx', 'xlsm', 'xls']

const fileInput = ref<HTMLInputElement | null>(null)
const file = ref<File | null>(null)
const dragging = ref(false)
const profileId = ref('')
const profiles = ref<Array<{ id: string; label: string }>>([])

const session = ref<any>(null)

/**
 * Changes whenever the report is recomputed. The creation panel watches it, because creating an
 * experiment changes what inserting the data requires.
 */
const reportStamp = ref('')

/**
 * Every draft this conversation produced, keyed by identifier, so each turn keeps showing the one
 * it made — applied or cancelled included.
 */
const proposalsById = ref<{ [id: string]: any }>({})
const busy = ref(false)
const asking = ref(false)
const revalidating = ref(false)
const variableForm = ref<any>(null)
const personForm = ref<any>(null)
const uploadError = ref<string | null>(null)

function service() {
  return $opensilex.getService('opensilex-ai-import.AiImportService')
}

onMounted(async () => {
  try {
    const http = await service().getProfiles()
    profiles.value = http.response.result
  } catch (error) {
    // A missing profile list only costs the user the manual override, so it is not worth an error
    // dialog on arrival.
    console.warn('Could not load the import profiles', error)
  }
})

function onFileChosen(event: Event) {
  const input = event.target as HTMLInputElement
  select(input.files && input.files.length ? input.files[0] : null)
}

function onDrop(event: DragEvent) {
  dragging.value = false
  const dropped = event.dataTransfer?.files
  select(dropped && dropped.length ? dropped[0] : null)
}

function select(chosen: File | null) {
  uploadError.value = null
  if (!chosen) {
    file.value = null
    return
  }
  const extension = chosen.name.split('.').pop()?.toLowerCase() ?? ''
  if (!ACCEPTED_EXTENSIONS.includes(extension)) {
    file.value = null
    uploadError.value = $opensilex.$i18n.t('AiImport.upload.wrongType')
    return
  }
  file.value = chosen
}

async function startSession() {
  if (!file.value) {
    return
  }
  busy.value = true
  uploadError.value = null
  try {
    // Multipart, so it goes through the upload helper rather than the generated client.
    const response = await $opensilex.uploadFileToService(
      '/ai-import/sessions',
      { file: file.value },
      profileId.value ? { profile: profileId.value } : null
    )
    session.value = response.result ?? response
    stampReport()
    rememberProposal(session.value.pending_proposal)
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    busy.value = false
  }
}

async function ask(question: string) {
  if (!session.value) {
    return
  }
  // Shown immediately: waiting for the round trip before echoing the question makes the page feel
  // broken, and the reply can take a while when the assistant queries the instance.
  session.value.messages.push({ role: 'user', content: question, lookups: [] })
  asking.value = true
  try {
    const http = await service().askQuestion({
      session_id: session.value.session_id,
      content: question
    })
    session.value.messages.push(http.response.result)
    // A reply can carry a draft, which lives on the session rather than on the message.
    await refreshSession()
    await refreshReport()
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    asking.value = false
  }
}

async function revalidate() {
  if (!session.value) {
    return
  }
  revalidating.value = true
  try {
    const http = await service().revalidate(session.value.session_id)
    session.value.report = http.response.result
    stampReport()
    await refreshMapping()
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    revalidating.value = false
  }
}

/**
 * A "create" click in the report becomes a question to the assistant.
 *
 * It could have opened a form, but then the draft would arrive with no explanation. Asking keeps
 * the reasoning and the confirmation in the same place.
 */
function requestCreation(payload: { target: string; value: string }) {
  if (payload.target === 'VARIABLE') {
    openVariableForm(payload.value)
    return
  }
  if (payload.target === 'PERSON') {
    openPersonForm(payload.value)
    return
  }
  const what = $opensilex.$i18n.t('AiImport.report.askCreate_' + payload.target,
    { value: payload.value })
  ask(what)
}

/**
 * Opens the platform's variable form, carrying what the file already says about the column.
 *
 * The name and the ontology identifier come from the workbook; the entity, characteristic, method
 * and unit do not — no template names all four — so those are chosen in the form, from what the
 * instance already has, and created there only if they are genuinely new.
 */
function openVariableForm(columnKey: string) {
  const candidate = (session.value.report?.variables ?? [])
    .find((item: any) => item.source_value === columnKey)

  // A component the instance already has is filled in; one it does not is left for the user to
  // choose or create in the form. Reusing beats creating: a duplicate entity entered under a
  // second spelling stays in the referential for good.
  const components: any[] = candidate?.components ?? []
  const uriOf = (role: string) => components.find((c) => c.role === role)?.uri ?? null

  variableForm.value?.showCreateForm({
    uri: null,
    name: columnKey,
    alternative_name: null,
    entity: uriOf('entity'),
    entity_of_interest: null,
    characteristic: uriOf('characteristic'),
    description: describeComponents(components),
    time_interval: null,
    sampling_interval: null,
    datatype: null,
    trait: null,
    trait_name: null,
    method: uriOf('method'),
    unit: uriOf('unit'),
    // The file's ontology identifier is an exact match by definition — it names the same variable
    // in a published referential — so it is carried as one rather than dropped.
    exact_match: candidate?.external_id ? [candidate.external_id] : [],
    close_match: [],
    broad_match: [],
    narrow_match: [],
    species: null,
    linked_data_nb: 0
  })
}

/**
 * What the file said about the parts of this variable, in the description.
 *
 * A component that exists here goes into its selector; one that does not would otherwise be lost
 * between reading the file and filling the form, and it is exactly what the user needs in front of
 * them to pick or create the right thing.
 */
function describeComponents(components: any[]): string | null {
  const unresolved = components.filter((component) => !component.uri && component.name)
  if (!unresolved.length) {
    return null
  }
  const said = unresolved
    .map((component) => `${$opensilex.$i18n.t('AiImport.report.component_' + component.role)}: `
      + component.name + (component.accession ? ` (${component.accession})` : ''))
    .join(' — ')
  return String($opensilex.$i18n.t('AiImport.report.fromTheFile')) + ' ' + said
}

/**
 * Opens the platform's person form, with the name split into a given and a family name.
 *
 * That split is a guess — "Ines Chaves" divides cleanly, "Jean-Pierre de la Rue" does not — so it
 * is offered in a form where it is visible and correctable, never written silently.
 */
function openPersonForm(fullName: string) {
  const trimmed = (fullName ?? '').trim()
  const lastSpace = trimmed.lastIndexOf(' ')

  personForm.value?.showCreateForm({
    uri: null,
    email: null,
    first_name: lastSpace < 0 ? trimmed : trimmed.slice(0, lastSpace).trim(),
    last_name: lastSpace < 0 ? null : trimmed.slice(lastSpace + 1).trim(),
    affiliation: null,
    phone_number: null,
    orcid: null
  })
}

/**
 * The instance changed, so the report the user is looking at is stale.
 */
function onVariableCreated() {
  revalidate()
}

/**
 * A draft was cancelled: keep it on screen as the record of what was decided.
 */
function onProposalCancelled() {
  refreshSession()
}

/**
 * Re-reads the conversation, which is what carries the drafts and their state.
 */
async function refreshSession() {
  try {
    const http = await service().getSession(session.value.session_id)
    const fresh = http.response.result
    session.value.messages = fresh.messages
    rememberProposal(fresh.pending_proposal)
  } catch (error) {
    console.warn('Could not refresh the conversation', error)
  }
}

function rememberProposal(proposal: any) {
  if (proposal) {
    proposalsById.value = { ...proposalsById.value, [proposal.id]: proposal }
  }
}

/**
 * A creation returns the recomputed report, so the panel does not have to ask for it again.
 */
async function onCreated(payload: { target: string; report: any }) {
  if (!session.value) {
    return
  }
  session.value.report = payload.report
  stampReport()
  await refreshSession()
  await refreshMapping()
}

/**
 * The mapping quotes the matched variable and its expected type, so it goes stale the moment a
 * variable is created.
 */
async function refreshMapping() {
  try {
    const http = await service().getMapping(session.value.session_id)
    session.value.mapping = http.response.result
  } catch (error) {
    console.warn('Could not refresh the column mapping', error)
  }
}

function stampReport() {
  reportStamp.value = session.value?.report?.computed_at ?? String(Date.now())
}

/**
 * The assistant cannot change the instance, but the user can in another tab while the conversation
 * is open, so the report is re-read after each exchange.
 */
async function refreshReport() {
  try {
    const http = await service().getReport(session.value.session_id)
    session.value.report = http.response.result
    stampReport()
  } catch (error) {
    console.warn('Could not refresh the report', error)
  }
}
</script>

<style scoped>
/*
 * The app forces every .btn-sm to 32px wide for the icon-only action buttons in its tables
 * (opensilex-front/front/src/styles/common.scss). A labelled button in this panel needs its width
 * back; a scoped selector is specific enough to win without !important.
 */
.btn-sm {
  width: auto;
}

/* Flex rather than inline text, so the label can never end up on top of the button. */
.ai-import-drop__row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ai-import-drop {
  border: 2px dashed rgba(127, 127, 127, 0.4);
  border-radius: 0.5rem;
  padding: 1.25rem;
  text-align: center;
  transition: border-color 0.15s ease-in-out, background-color 0.15s ease-in-out;
}

.ai-import-drop--over {
  border-color: var(--bs-primary, #0d6efd);
  background-color: rgba(13, 110, 253, 0.05);
}
</style>
