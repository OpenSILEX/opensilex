<template>
  <div class="container-fluid py-3">
    <h3 class="mb-1">{{ $t('AiImport.title') }}</h3>
    <p class="text-body-secondary">{{ $t('AiImport.description') }}</p>

    <!--
      First screen: a new file, or a stored conversation. The platform's own tab markup (styled by
      its theme), as on a project's or a device's page. The sessions tab is always there, empty or
      not, so that where stored conversations live is never a surprise.
    -->
    <nav v-if="!session" class="tabs mb-3">
      <button type="button" :class="['tab', { active: homeTab === 'upload' }]" @click="homeTab = 'upload'">
        {{ $t('AiImport.home.newFile') }}
      </button>
      <button type="button" :class="['tab', { active: homeTab === 'sessions' }]" @click="homeTab = 'sessions'">
        {{ $t('AiImport.home.sessions') }}
        <span class="tabBadge">{{ savedSessions.length }}</span>
      </button>
      <button type="button" :class="['tab', { active: homeTab === 'export' }]" @click="homeTab = 'export'">
        {{ $t('AiImport.home.export') }}
      </button>
    </nav>

    <!-- Step one: pick a file. Kept on screen until a session opens, so a failed upload can be retried. -->
    <div v-if="!session && homeTab === 'upload'" class="card">
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

    <!--
      Conversations stored as they went, most recent first. Resuming one reads its file again and
      recomputes the report against the instance as it is now; the messages, the drafts and the
      confirmed names come back as they were left.
    -->
    <div v-if="!session && homeTab === 'sessions'" class="card">
      <div class="card-body">
        <h5 class="card-title">{{ $t('AiImport.saved.title') }}</h5>
        <p class="card-text text-body-secondary small">{{ $t('AiImport.saved.hint') }}</p>
        <div v-if="!savedSessions.length" class="text-body-secondary py-3">
          <p class="mb-2">{{ $t('AiImport.saved.empty') }}</p>
          <n-button size="small" secondary @click="homeTab = 'upload'">
            {{ $t('AiImport.home.newFile') }}
          </n-button>
        </div>
        <table v-else class="table table-sm align-middle mb-0">
          <thead>
            <tr>
              <th>{{ $t('AiImport.saved.file') }}</th>
              <th>{{ $t('AiImport.saved.updated') }}</th>
              <th class="text-end">{{ $t('AiImport.saved.messages') }}</th>
              <th>{{ $t('AiImport.saved.expires') }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="saved in savedSessions" :key="saved.session_id">
              <td>
                <code>{{ saved.file_name }}</code>
                <span v-if="saved.profile_id" class="text-body-secondary small ms-2">
                  {{ saved.profile_id }}
                </span>
              </td>
              <td class="small">{{ formatDate(saved.updated_at) }}</td>
              <td class="text-end small">{{ saved.message_count }}</td>
              <td class="small text-body-secondary">{{ formatDate(saved.expires_at) }}</td>
              <td class="text-end text-nowrap">
                <n-button size="tiny" type="primary" secondary :loading="resuming === saved.session_id"
                          :disabled="!!resuming" @click="resumeSession(saved.session_id)">
                  {{ $t('AiImport.saved.resume') }}
                </n-button>
                <opensilex-DeleteButton
                  class="ms-1"
                  :label="$t('AiImport.saved.delete')"
                  :small="true"
                  @click="deleteSavedSession(saved.session_id)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!--
      The other direction: an experiment of this instance written as a STAR workbook — the file the
      STAR profile reads. No language model is involved, and nothing is written.
    -->
    <div v-if="!session && homeTab === 'export'" class="card">
      <div class="card-body">
        <h5 class="card-title">{{ $t('AiImport.export.title') }}</h5>
        <p class="card-text text-body-secondary small">{{ $t('AiImport.export.hint') }}</p>
        <div class="row g-2 align-items-end">
          <div class="col-md-6">
            <opensilex-ExperimentSelector
              v-model:experiments="exportExperiment"
              :label="$t('AiImport.export.experiment')"
              :required="true"
            />
          </div>
          <div class="col-md-auto">
            <n-button type="primary" size="small" :disabled="!exportExperiment" :loading="exporting"
                      @click="exportStar">
              {{ $t('AiImport.export.download') }}
            </n-button>
          </div>
        </div>
        <div v-if="exportError" class="alert alert-danger py-2 mt-3 mb-0">{{ exportError }}</div>
      </div>
    </div>

    <!-- Step two: the conversation beside what the instance holds. -->
    <div v-if="session" class="d-flex align-items-center gap-2 mb-2">
      <code>{{ session.file_name ?? '' }}</code>
      <n-button size="tiny" secondary :title="$t('AiImport.saved.leaveTitle')" @click="leaveSession">
        {{ $t('AiImport.saved.leave') }}
      </n-button>
      <n-button
        size="tiny"
        :secondary="chatOpen"
        :type="chatOpen ? 'default' : 'primary'"
        :aria-expanded="chatOpen"
        @click="toggleChat"
      >
        {{ chatOpen ? $t('AiImport.chat.hide') : $t('AiImport.chat.show') }}
      </n-button>
      <span
        v-if="!assistantConnected"
        class="badge text-bg-warning"
        :title="$t('AiImport.chat.disconnectedTitle')"
      >
        {{ $t('AiImport.chat.disconnected') }}
      </span>
    </div>
    <div v-if="session" class="row g-3">
      <!--
        Folded rather than removed, so a half-written question survives closing the panel. The
        report takes the whole width meanwhile.
      -->
      <div v-show="chatOpen" class="col-xl-6">
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
      <div :class="[chatOpen ? 'col-xl-6' : 'col-12', 'd-flex flex-column gap-3']">
        <opensilex-ai-import-ResolutionReportPanel
          v-if="session.report"
          :report="session.report"
          :revalidating="revalidating"
          @revalidate="revalidate"
          @create="requestCreation"
          @confirm-match="confirmMatch"
          @forget-match="forgetMatch"
          @remember-correction="rememberCorrection"
          @forget-correction="forgetCorrection"
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
          @onCreate="onCreatedInForm"
        />

        <!-- Likewise for a person: the platform's form, prefilled from the file. -->
        <opensilex-PersonForm
          ref="personForm"
          createTitle="component.person.add"
          editTitle="component.person.update"
          @onCreate="onCreatedInForm"
        />

        <!--
          And for a project and a facility: the platform's own wizards, prefilled through their
          initForm hook with what the file says. An experiment has no creation form in this front
          yet, so it is still drafted on the proposal card.
        -->
        <opensilex-ProjectForm
          ref="projectForm"
          :initForm="prefillProject"
          @onCreate="onCreatedInForm"
        />
        <opensilex-FacilityModalForm
          ref="facilityForm"
          :initForm="prefillFacility"
          @onCreate="onCreatedInForm"
        />
        <!-- The organisations screen's own form, a unit opened with its institution as parent. -->
        <opensilex-OrganizationForm
          ref="organizationForm"
          createTitle="OrganizationView.create"
          editTitle="OrganizationView.update"
          @onCreate="onCreatedInForm"
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

    <!--
      The object sheets across the whole width: their columns are the file's, and a narrow column
      would hide the very drop-downs the user came to set.
    -->
    <opensilex-ai-import-ObjectSheetsPanel
      v-if="session"
      class="mt-3"
      :session-id="session.session_id"
      :report-stamp="reportStamp"
      :experiment="resolvedExperiment"
      @create="requestCreation"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'

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
 * The notice the module writes in the conversation when the model did not answer.
 */
const UNREACHABLE_NOTICE = 'AiImport.chat.assistantUnreachable'

/**
 * Whether the language model can hold a conversation, asked each time a session opens. Null until
 * the answer comes, and counted as connected meanwhile, so a slow probe never hides the panel.
 */
const assistant = ref<{ configured: boolean; reachable: boolean } | null>(null)
const assistantConnected = computed(() => assistant.value === null || assistant.value.reachable)

/**
 * Whether the conversation is shown. It starts folded when the model cannot answer — the report,
 * the mapping and the creation forms work without it — and the user opens or closes it at will.
 * Once they have, their choice stands for the rest of the visit.
 */
const chatOpen = ref(true)
const chatChosen = ref(false)

function toggleChat() {
  chatChosen.value = true
  chatOpen.value = !chatOpen.value
}

async function checkAssistant() {
  try {
    const http = await service().getAssistantStatus()
    assistant.value = http.response.result
  } catch (error) {
    // Unknown is not disconnected: the panel stays as it is, and the first question will tell.
    console.warn('Could not ask whether the assistant is connected', error)
    return
  }
  if (!chatChosen.value) {
    chatOpen.value = assistant.value?.reachable ?? true
  }
}

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
const projectForm = ref<any>(null)
const facilityForm = ref<any>(null)
const organizationForm = ref<any>(null)

/**
 * What the next platform form opens with. The forms call their initForm hook when they build their
 * empty form, so the values are set here just before they are shown.
 */
const projectPrefill = ref<{ [field: string]: string }>({})

/**
 * The row of the report a creation form was opened for, and the report category it belongs to.
 */
const pendingRow = ref<{ category: string; value: string } | null>(null)
const CATEGORY_OF_TARGET: { [target: string]: string } = {
  VARIABLE: 'variables',
  PERSON: 'persons',
  PROJECT: 'projects',
  FACILITY: 'facilities',
  ORGANIZATION: 'organizations'
}
const facilityPrefill = ref<string | null>(null)
const uploadError = ref<string | null>(null)

/**
 * The user's stored conversations, offered for resumption on the first screen.
 */
const savedSessions = ref<any[]>([])
const resuming = ref<string | null>(null)

/**
 * The tab of the first screen: a new file, the stored conversations, or an export.
 */
const homeTab = ref<'upload' | 'sessions' | 'export'>('upload')

/**
 * The experiment to write as a STAR workbook.
 */
const exportExperiment = ref<string | null>(null)
const exporting = ref(false)
const exportError = ref<string | null>(null)

const STAR_EXPORT_PATH = '/ai-import/star'

/**
 * The experiment the report found for this file, offered first where objects are created in it.
 */
const resolvedExperiment = computed<string | null>(() => {
  const found = session.value?.report?.experiments?.find((item: any) => item.status === 'FOUND')
  return found?.matches?.[0]?.uri ?? null
})

function service() {
  return $opensilex.getService('opensilex-ai-import.AiImportService')
}

/**
 * Downloads the workbook through the platform's own download helper, which carries the token and
 * the language. The file is named after the experiment's URI, the only name at hand here; the
 * server names it after the experiment itself in its header.
 */
async function exportStar() {
  if (!exportExperiment.value) {
    return
  }
  exporting.value = true
  exportError.value = null
  try {
    const uri = exportExperiment.value
    const name = 'STAR_' + uri.substring(Math.max(uri.lastIndexOf('/'), uri.lastIndexOf('#'), uri.lastIndexOf(':')) + 1)
    await $opensilex.downloadFilefromService(STAR_EXPORT_PATH, name, 'xlsx', { experiment: uri })
  } catch (error) {
    // The helper has already shown the platform's error; this says which action it was.
    exportError.value = $opensilex.$i18n.t('AiImport.export.failed')
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  loadSavedSessions()
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
    openSession(response.result ?? response)
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    busy.value = false
  }
}

async function loadSavedSessions() {
  try {
    const http = await service().listSavedSessions()
    savedSessions.value = http.response.result ?? []
  } catch (error) {
    // The list is a convenience on the first screen; without it, a new file can still be analysed.
    console.warn('Could not list the stored import conversations', error)
    savedSessions.value = []
  }
}

/**
 * Opens a stored conversation. The server rebuilds it from its file if it is no longer in memory,
 * so reading it is all it takes.
 */
async function resumeSession(sessionId: string) {
  resuming.value = sessionId
  try {
    const http = await service().getSession(sessionId)
    openSession(http.response.result)
  } catch (error) {
    $opensilex.errorHandler(error)
    await loadSavedSessions()
  } finally {
    resuming.value = null
  }
}

async function deleteSavedSession(sessionId: string) {
  try {
    await service().deleteSession(sessionId)
  } catch (error) {
    $opensilex.errorHandler(error)
  }
  await loadSavedSessions()
}

/**
 * Back to the first screen, on the sessions tab. The conversation is stored as it goes, so leaving it
 * loses nothing: it is in the list, ready to be resumed.
 */
async function leaveSession() {
  session.value = null
  proposalsById.value = {}
  file.value = null
  homeTab.value = 'sessions'
  await loadSavedSessions()
}

/**
 * Shows a session, from an upload or a resumption, with every draft it made so that each card
 * appears under its message again.
 */
function openSession(opened: any) {
  session.value = opened
  proposalsById.value = {}
  for (const proposal of opened?.proposals ?? []) {
    rememberProposal(proposal)
  }
  rememberProposal(opened?.pending_proposal)
  stampReport()
  checkAssistant()
}

function formatDate(value: string | null): string {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  return isNaN(date.getTime()) ? value : date.toLocaleString($opensilex.getLang())
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
    const reply = http.response.result
    session.value.messages.push(reply)
    // The reply says more recently than the probe whether the model answers. The panel is left as
    // it is: folding it now would hide the notice the user is reading.
    assistant.value = {
      configured: assistant.value?.configured ?? true,
      reachable: reply?.content_key !== UNREACHABLE_NOTICE
    }
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
  await updateReport(() => service().revalidate(session.value.session_id))
}

/**
 * The user says which existing resource a misspelt name of the file means.
 *
 * The server accepts only one of the suggestions it made for that name, and answers with the
 * report recomputed — the name now resolves as if it had been spelt right, for the rest of the
 * conversation. Nothing is renamed, in the file or in the instance.
 */
async function confirmMatch(payload: { category: string; value: string; uri: string }) {
  await updateReport(() => service().confirmMatch({
    session_id: session.value.session_id,
    category: payload.category,
    value: payload.value,
    uri: payload.uri
  }))
}

/**
 * Takes a confirmation back: the name returns to missing, with its suggestions.
 */
async function forgetMatch(payload: { category: string; value: string }) {
  await updateReport(() => service().forgetMatch({
    session_id: session.value.session_id,
    category: payload.category,
    value: payload.value
  }))
}

/**
 * Teaches the instance a confirmed correction, for every later import. The server checks the right
 * to modify this kind of resource and refuses otherwise, with a message the error handler shows.
 */
async function rememberCorrection(payload: { category: string; value: string; uri: string }) {
  await updateReport(() => service().rememberCorrection({
    session_id: session.value.session_id,
    category: payload.category,
    value: payload.value,
    uri: payload.uri
  }))
}

/**
 * Makes the instance forget a correction it was taught, for everyone.
 */
async function forgetCorrection(payload: { category: string; value: string }) {
  await updateReport(() => service().forgetCorrection({
    session_id: session.value.session_id,
    category: payload.category,
    value: payload.value
  }))
}

/**
 * Runs a call that answers with a recomputed report, and shows it — the same steps as a
 * revalidation, since that is what the server did.
 */
async function updateReport(call: () => Promise<any>) {
  if (!session.value) {
    return
  }
  revalidating.value = true
  try {
    const http = await call()
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
 * A "create" click in the report opens the platform's own creation form, prefilled from the file:
 * variable, person, project, facility. The row it was opened for is remembered, so that what the
 * form creates is bound to that row (see onCreatedInForm).
 *
 * An experiment has no creation form in this front, so its click becomes a question to the
 * assistant, whose draft is confirmed on the proposal card.
 */
function requestCreation(payload: { target: string; value: string }) {
  pendingRow.value = CATEGORY_OF_TARGET[payload.target]
    ? { category: CATEGORY_OF_TARGET[payload.target], value: payload.value }
    : null
  if (payload.target === 'VARIABLE') {
    openVariableForm(payload.value)
    return
  }
  if (payload.target === 'PERSON') {
    openPersonForm(payload.value)
    return
  }
  if (payload.target === 'PROJECT') {
    openProjectForm(payload.value)
    return
  }
  if (payload.target === 'FACILITY') {
    openFacilityForm(payload.value)
    return
  }
  if (payload.target === 'ORGANIZATION') {
    openOrganizationForm(payload.value)
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
 * Opens the platform's project wizard, with what the file suggests for each field.
 *
 * The suggestions are the ones the server computes for a PROJECT draft — name, short name, dates,
 * objective — so the form and the proposal card start from the same values. Their field names are
 * those of the platform's creation DTO. The name clicked in the report wins over the suggestion.
 */
async function openProjectForm(name: string) {
  const values: { [field: string]: string } = {}
  try {
    const http = await service().getCreationRequirements(session.value.session_id, 'PROJECT')
    for (const field of http.response.result.fields ?? []) {
      if (field.name && field.suggested_value) {
        values[field.name] = field.suggested_value
      }
    }
  } catch (error) {
    // Without suggestions the form still opens, with the name: the user fills in the rest.
    console.warn('Could not read what the file suggests for a project', error)
  }
  values.name = name
  projectPrefill.value = values
  projectForm.value?.showCreateForm()
}

function prefillProject(form: any) {
  Object.assign(form, projectPrefill.value)
}

/**
 * Opens the platform's facility wizard with the name the file gives. The type, the organisation
 * and the site are the user's to choose: the file does not say them.
 */
function openFacilityForm(name: string) {
  facilityPrefill.value = name
  facilityForm.value?.showCreateForm()
}

/**
 * What the file says of the field, in the facility form: its name, its commune as the address's
 * locality, its centroid as a dated location — a point, which the location step shows in WKT — the
 * organisations the report found, and in the description what no core property holds, such as the
 * row and plant spacing.
 */
function prefillFacility(form: any) {
  const name = facilityPrefill.value
  form.name = name ?? undefined
  const item = (session.value?.report?.facilities ?? []).find((candidate: any) => candidate.source_value === name)
  const details: { [key: string]: string } = item?.details ?? {}
  if (details.town) {
    form.address = { ...(form.address ?? {}), locality: details.town }
  }
  const latitude = Number.parseFloat(details.latitude)
  const longitude = Number.parseFloat(details.longitude)
  if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
    form.locations = [{
      geojson: { type: 'Point', coordinates: [longitude, latitude] },
      endDate: new Date().toISOString()
    }]
  }
  const described = [
    details.row_spacing ? $opensilex.$i18n.t('AiImport.report.fieldRowSpacing', { value: details.row_spacing }) : null,
    details.plant_spacing ? $opensilex.$i18n.t('AiImport.report.fieldPlantSpacing', { value: details.plant_spacing }) : null,
    details.insee ? $opensilex.$i18n.t('AiImport.report.fieldInsee', { value: details.insee }) : null
  ].filter((part) => !!part)
  if (described.length) {
    form.description = described.join(' ; ')
  }
  const organizations = (session.value?.report?.organizations ?? [])
    .filter((candidate: any) => candidate.status === 'FOUND' && candidate.matches?.length)
    .map((candidate: any) => candidate.matches[0].uri)
  if (organizations.length) {
    form.organizations = organizations
  }
}

/**
 * Opens the platform's organisation form with the name the file gives. A unit the file places in
 * an institution the instance already has starts with that institution as its parent; one whose
 * institution is still missing starts without, and the institution is best created first.
 */
function openOrganizationForm(name: string) {
  const organizations: any[] = session.value?.report?.organizations ?? []
  const item = organizations.find((candidate) => candidate.source_value === name)
  const parent = item?.parent_value
    ? organizations.find((candidate) => candidate.source_value === item.parent_value && candidate.status === 'FOUND')
    : null
  const parentUri = parent?.matches?.[0]?.uri
  organizationForm.value?.showCreateForm({
    uri: null,
    rdf_type: null,
    name,
    parents: parentUri ? [parentUri] : [],
    groups: [],
    facilities: []
  })
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
/**
 * Something was created in a platform form, opened from a row of the report.
 *
 * The row is bound to the created resource by its URI rather than found again by its name: the form
 * may have renamed it — the variable form names a variable after its components, and the user may
 * edit any name — and a column whose variable was just created must not come back "missing". The
 * server reads the URI back under the user's rights before accepting it.
 *
 * The forms do not hand back the same thing: the project and facility wizards emit the form with
 * its new URI, the variable and person forms the HTTP response whose result is the URI.
 */
async function onCreatedInForm(payload: any) {
  const row = pendingRow.value
  pendingRow.value = null
  const uri = payload?.uri ?? payload?.response?.result
  if (!row || typeof uri !== 'string') {
    await revalidate()
    return
  }
  await updateReport(() => service().bindCreatedResource({
    session_id: session.value.session_id,
    category: row.category,
    value: row.value,
    uri
  }))
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
