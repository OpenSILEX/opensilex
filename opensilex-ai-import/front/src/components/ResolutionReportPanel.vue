<template>
  <div class="card">
    <div class="card-header d-flex align-items-center justify-content-between flex-wrap gap-2">
      <span class="fw-semibold">{{ $t('AiImport.report.title') }}</span>
      <div class="d-flex align-items-center gap-2">
        <span v-if="report.profile_id" class="badge text-bg-light">
          {{ $t('AiImport.report.profile') }}: {{ report.profile_id }}
        </span>
        <n-button size="small" secondary :loading="revalidating" @click="$emit('revalidate')">
          {{ revalidating ? $t('AiImport.report.revalidating') : $t('AiImport.report.revalidate') }}
        </n-button>
      </div>
    </div>

    <div class="card-body">
      <div v-if="report.is_complete" class="alert alert-success py-2">
        {{ $t('AiImport.report.complete') }}
      </div>
      <div v-else class="d-flex flex-wrap gap-2 mb-3">
        <span v-if="report.missing_count" class="badge text-bg-danger">
          {{ $t('AiImport.report.missingSummary', { count: report.missing_count }) }}
        </span>
        <span v-if="report.ambiguous_count" class="badge text-bg-warning">
          {{ $t('AiImport.report.ambiguousSummary', { count: report.ambiguous_count }) }}
        </span>
      </div>

      <div v-if="anomalies.length" class="alert alert-warning py-2">
        <div class="fw-semibold mb-1">{{ $t('AiImport.report.anomalies') }}</div>
        <ul class="mb-0 ps-3">
          <li v-for="(anomaly, index) in anomalies" :key="index">{{ anomaly }}</li>
        </ul>
      </div>

      <div v-if="warnings.length" class="alert alert-secondary py-2">
        <div class="fw-semibold mb-1">{{ $t('AiImport.report.warnings') }}</div>
        <ul class="mb-0 ps-3">
          <li v-for="(warning, index) in warnings" :key="index">{{ warning }}</li>
        </ul>
      </div>

      <div v-if="notes.length" class="mb-3 small text-body-secondary">
        <span class="fw-semibold">{{ $t('AiImport.report.notes') }}:</span>
        <span v-for="(note, index) in notes" :key="note.key">
          {{ index ? ' · ' : ' ' }}{{ note.key }} = {{ note.value }}
        </span>
      </div>

      <n-collapse v-model:expanded-names="expanded">
        <n-collapse-item
          v-for="category in categoriesWithBadges"
          :key="category.key"
          :name="category.key"
        >
          <template #header>
            <div class="d-flex align-items-center gap-2 flex-wrap">
              <span class="fw-semibold">{{ $t(category.label) }}</span>
              <span class="text-body-secondary small">({{ category.items.length }})</span>
              <!-- The counts sit in the header so a collapsed category still says what is inside. -->
              <n-tag
                v-for="badge in category.badges"
                :key="badge.status"
                size="small"
                :type="tagType(badge.status)"
              >
                {{ badge.count }} {{ $t('AiImport.report.status_' + badge.status) }}
              </n-tag>
            </div>
          </template>

          <p v-if="!category.items.length" class="text-body-secondary small mb-0">
            {{ $t('AiImport.report.empty') }}
          </p>

          <div v-else class="table-responsive ai-import-report__list">
            <table class="table table-sm align-middle small mb-0">
              <thead>
                <tr>
                  <th>{{ $t('AiImport.report.sourceValue') }}</th>
                  <th>{{ $t('AiImport.report.status') }}</th>
                  <th>{{ $t('AiImport.report.matches') }}</th>
                  <th>{{ $t('AiImport.report.hint') }}</th>
                  <th v-if="category.creates"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in category.items" :key="index">
                  <td class="text-nowrap">
                    <code>{{ item.source_value }}</code>
                    <div v-if="item.external_id" class="text-body-secondary">{{ item.external_id }}</div>
                  </td>
                  <td class="text-nowrap">
                    <n-tag size="small" :type="tagType(item.status)">
                      {{ $t('AiImport.report.status_' + item.status) }}
                    </n-tag>
                  </td>
                  <td>
                    <div v-for="(match, matchIndex) in item.matches" :key="matchIndex">
                      <a v-if="category.link" :href="category.link(match.uri)" target="_blank" rel="noopener">
                        {{ match.name }}
                      </a>
                      <span v-else>{{ match.name }}</span>
                      <span v-if="match.shared_resource_instance_label" class="text-body-secondary">
                        — {{ match.shared_resource_instance_label }}
                      </span>
                    </div>

                    <!--
                      Linked by a person, not found by name: said so, and reversible, because the
                      file still spells it differently and a wrong click must cost one more click.
                    -->
                    <div v-if="item.confirmed_by_user" class="ai-import-report__confirmed">
                      <n-tag size="tiny" type="info">{{ $t('AiImport.report.confirmedByYou') }}</n-tag>
                      <n-button
                        size="tiny"
                        quaternary
                        :disabled="revalidating"
                        @click="$emit('forget-match', { category: category.key, value: item.source_value })"
                      >
                        {{ $t('AiImport.report.forgetMatch') }}
                      </n-button>
                      <n-button
                        v-if="item.matches && item.matches.length"
                        size="tiny"
                        secondary
                        :disabled="revalidating"
                        :title="$t('AiImport.report.rememberCorrectionTitle')"
                        @click="$emit('remember-correction', {
                          category: category.key, value: item.source_value, uri: item.matches[0].uri
                        })"
                      >
                        {{ $t('AiImport.report.rememberCorrection') }}
                      </n-button>
                    </div>

                    <!--
                      Recognised from a correction somebody taught the instance: on whose word, and
                      forgettable, since it acts on everyone's imports and this user did not make it.
                    -->
                    <div v-if="item.learned_correction" class="ai-import-report__confirmed">
                      <n-tag size="tiny" type="info">
                        {{ $t('AiImport.report.learnedBy', {
                          author: item.learned_correction.author || '?',
                          date: item.learned_correction.created || '?'
                        }) }}
                      </n-tag>
                      <n-button
                        size="tiny"
                        quaternary
                        :disabled="revalidating"
                        :title="$t('AiImport.report.forgetCorrectionTitle')"
                        @click="$emit('forget-correction', { category: category.key, value: item.source_value })"
                      >
                        {{ $t('AiImport.report.forgetCorrection') }}
                      </n-button>
                    </div>

                    <!--
                      Close names the instance already has. Offered, never applied: the row stays
                      missing until the user picks one, since a click on the wrong neighbour files
                      their data against somebody else's resource.
                    -->
                    <div v-if="item.suggestions && item.suggestions.length" class="ai-import-report__suggestions">
                      <div class="text-body-secondary">{{ $t('AiImport.report.didYouMean') }}</div>
                      <div
                        v-for="suggestion in item.suggestions"
                        :key="suggestion.uri"
                        class="ai-import-report__suggestion"
                      >
                        <span class="ai-import-report__suggestion-name">
                          <span
                            v-for="(segment, segmentIndex) in diffAgainst(item.source_value, suggestion.name)"
                            :key="segmentIndex"
                            :class="{ 'ai-import-report__diff': segment.changed }"
                          >{{ segment.text }}</span>
                        </span>
                        <span
                          v-if="suggestion.similarity != null"
                          class="text-body-secondary"
                        >
                          ({{ $t('AiImport.report.similarity', { percent: Math.round(suggestion.similarity * 100) }) }})
                        </span>
                        <n-button
                          size="tiny"
                          secondary
                          type="primary"
                          :disabled="revalidating"
                          :title="$t('AiImport.report.confirmMatchTitle')"
                          @click="$emit('confirm-match', {
                            category: category.key, value: item.source_value, uri: suggestion.uri
                          })"
                        >
                          {{ $t('AiImport.report.confirmMatch') }}
                        </n-button>
                      </div>
                    </div>
                  </td>
                  <td>
                    {{ say(item.hint_message, item.hint) }}

                    <!--
                      What a missing variable is made of. Shown here because it decides how much
                      work creating it is: three components already present is a two-click job,
                      none is a form to fill.
                    -->
                    <div v-if="item.components && item.components.length" class="mt-1">
                      <n-tag
                        v-for="component in item.components"
                        :key="component.role"
                        size="tiny"
                        :type="component.uri ? 'success' : (component.suggested_name ? 'warning' : 'default')"
                        class="me-1"
                      >
                        {{ $t('AiImport.report.component_' + component.role) }}: {{ component.name }}
                        <!-- A close name the instance has: to pick in the form's selector, not applied. -->
                        <template v-if="!component.uri && component.suggested_name">
                          ≈ {{ component.suggested_name }}
                        </template>
                      </n-tag>
                    </div>
                  </td>
                  <td v-if="category.creates" class="text-nowrap">
                    <n-button
                      v-if="item.status === 'MISSING' && canCreate(category.creates)"
                      size="tiny"
                      secondary
                      :title="$t(createTitleOf(category.creates))"
                      @click="$emit('create', { target: category.creates, value: item.source_value })"
                    >
                      {{ $t('AiImport.report.create') }}
                    </n-button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </n-collapse-item>
      </n-collapse>

    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useReportMessage, type ReportMessage } from '../reportMessage'
import { diffAgainst } from '../textDiff'

interface Match {
  uri: string
  name: string
  similarity?: number
  shared_resource_instance_label?: string
}



interface ResolvedItem {
  source_value: string
  external_id?: string
  status: string
  hint_message?: ReportMessage
  suggestions?: Match[]
  confirmed_by_user?: boolean
  learned_correction?: { author?: string; created?: string }
  components?: Array<{
    role: string
    name: string
    accession?: string
    uri?: string
    suggested_name?: string
    suggested_uri?: string
  }>
  matches: Match[]
  hint?: string
}

/**
 * The deliverable of a session: what exists, what does not, and what to do about it.
 *
 * Every URI shown here came back from a database, never from the assistant, which is why the
 * matches are safe to turn into links.
 */
const props = defineProps<{
  report: {
    profile_id?: string
    experiments: ResolvedItem[]
    projects: ResolvedItem[]
    variables: ResolvedItem[]
    germplasm: ResolvedItem[]
    scientific_objects: ResolvedItem[]
    facilities?: ResolvedItem[]
    persons?: ResolvedItem[]
    organizations?: ResolvedItem[]
    anomalies?: string[]
    warnings?: string[]
    anomaly_messages?: ReportMessage[]
    warning_messages?: ReportMessage[]
    notes?: { [key: string]: string }
    missing_count: number
    ambiguous_count: number
    is_complete: boolean
  }
  revalidating: boolean
}>()

defineEmits<{
  (event: 'revalidate'): void
  (event: 'create', payload: { target: string; value: string }): void
  (event: 'confirm-match', payload: { category: string; value: string; uri: string }): void
  (event: 'forget-match', payload: { category: string; value: string }): void
  (event: 'remember-correction', payload: { category: string; value: string; uri: string }): void
  (event: 'forget-correction', payload: { category: string; value: string }): void
}>()

/**
 * A category holds anything from nothing to several hundred entries, so each one collapses. What is
 * open is state of its own rather than a computed value: once the user has folded a category away,
 * a revalidation must not unfold it again.
 */
const expanded = ref<string[]>([])

const $opensilex: any = inject('$opensilex')

/**
 * A report sentence, in the user's language.
 *
 * The server sends both a translation key with its parameters and the English text it gave the
 * assistant. The key wins when there is one; the English is the fallback for a sentence a profile
 * wrote itself — showing it beats showing nothing.
 */
/**
 * Two of these buttons open a form of the platform's own; the others hand the question to the
 * assistant. The tooltip has to say which, or the user cannot tell what a click will do.
 */
/**
 * The credential each creation needs on the platform. Without it the form would open, be filled in,
 * and fail with a 403 on submit; the platform's own screens hide the button instead, and so does
 * this one.
 */
const CREDENTIAL_OF_TARGET: { [target: string]: string } = {
  EXPERIMENT: 'CREDENTIAL_EXPERIMENT_MODIFICATION_ID',
  PROJECT: 'CREDENTIAL_PROJECT_MODIFICATION_ID',
  VARIABLE: 'CREDENTIAL_VARIABLE_MODIFICATION_ID',
  FACILITY: 'CREDENTIAL_FACILITY_MODIFICATION_ID',
  PERSON: 'CREDENTIAL_PERSON_MODIFICATION_ID',
  ORGANIZATION: 'CREDENTIAL_ORGANIZATION_MODIFICATION_ID'
}

function canCreate(target: string | null): boolean {
  const credential = target ? $opensilex.$store?.state?.credentials?.[CREDENTIAL_OF_TARGET[target]] : null
  const user = $opensilex.getUser?.()
  return !!credential && !!user && user.hasCredential(credential)
}

function createTitleOf(target: string | null): string {
  if (target === 'VARIABLE') {
    return 'AiImport.report.createVariableTitle'
  }
  if (target === 'PERSON') {
    return 'AiImport.report.createPersonTitle'
  }
  if (target === 'PROJECT') {
    return 'AiImport.report.createProjectTitle'
  }
  if (target === 'FACILITY') {
    return 'AiImport.report.createFacilityTitle'
  }
  if (target === 'ORGANIZATION') {
    return 'AiImport.report.createOrganizationTitle'
  }
  return 'AiImport.report.createTitle'
}

const say = useReportMessage($opensilex)

const anomalies = computed(() => {
  const messages = props.report.anomaly_messages
  return messages?.length
    ? messages.map((message) => say(message))
    : (props.report.anomalies ?? [])
})

const warnings = computed(() => {
  const messages = props.report.warning_messages
  return messages?.length
    ? messages.map((message) => say(message))
    : (props.report.warnings ?? [])
})

const categories = computed(() => [
  {
    key: 'experiments',
    label: 'AiImport.report.experiments',
    items: props.report.experiments ?? [],
    link: (uri: string) => `#/experiment/details/${encodeURIComponent(uri)}`,
    // Only the categories with a way to create them from here: a platform form (project, facility,
    // variable, person) or a draft on the proposal card (experiment). Germplasm and scientific
    // objects are created elsewhere, and a button that opens an empty form would be a false promise.
    creates: 'EXPERIMENT'
  },
  {
    key: 'projects',
    label: 'AiImport.report.projects',
    items: props.report.projects ?? [],
    link: (uri: string) => `#/project/details/${encodeURIComponent(uri)}`,
    creates: 'PROJECT'
  },
  {
    key: 'variables',
    label: 'AiImport.report.variables',
    items: props.report.variables ?? [],
    link: (uri: string) => `#/variable/details/${encodeURIComponent(uri)}`,
    creates: 'VARIABLE'
  },
  {
    key: 'germplasm',
    label: 'AiImport.report.germplasm',
    items: props.report.germplasm ?? [],
    link: null,
    creates: null
  },
  {
    key: 'scientificObjects',
    label: 'AiImport.report.scientificObjects',
    items: props.report.scientific_objects ?? [],
    link: null,
    creates: null
  },
  {
    key: 'facilities',
    label: 'AiImport.report.facilities',
    items: props.report.facilities ?? [],
    link: (uri: string) => `#/facility/details/${encodeURIComponent(uri)}`,
    creates: 'FACILITY'
  },
  {
    key: 'persons',
    label: 'AiImport.report.persons',
    items: props.report.persons ?? [],
    link: null,
    creates: 'PERSON'
  },
  {
    key: 'organizations',
    label: 'AiImport.report.organizations',
    items: props.report.organizations ?? [],
    link: null,
    creates: 'ORGANIZATION'
  }
])

/**
 * Counts per status, shown in the header of a collapsed category.
 */
function badgesOf(items: ResolvedItem[]): Array<{ status: string; count: number }> {
  const counts = new Map<string, number>()
  items.forEach((item) => counts.set(item.status, (counts.get(item.status) ?? 0) + 1))
  return Array.from(counts, ([status, count]) => ({ status, count }))
}

const categoriesWithBadges = computed(() =>
  categories.value.map((category) => ({ ...category, badges: badgesOf(category.items) }))
)

/**
 * Opens, on arrival, only the categories that need the user to do something. A report where nothing
 * resolves would otherwise open five long tables at once, which is the problem the collapse is
 * there to solve.
 */
function openWhatNeedsAttention() {
  expanded.value = categoriesWithBadges.value
    .filter((category) =>
      category.items.some(
        (item) => item.status === 'MISSING' || item.status === 'AMBIGUOUS'
      )
    )
    .map((category) => category.key)
}

// Only for the first report of a session: reopening on every revalidation would undo the user's
// own folding, and revalidation is exactly what they do after acting on a category.
let opened = false
watch(
  () => props.report,
  () => {
    if (!opened && props.report) {
      openWhatNeedsAttention()
      opened = true
    }
  },
  { immediate: true }
)

const notes = computed(() =>
  Object.entries(props.report.notes ?? {}).map(([key, value]) => ({ key, value }))
)

function tagType(status: string): string {
  switch (status) {
    case 'FOUND':
      return 'success'
    case 'AMBIGUOUS':
      return 'warning'
    case 'FOUND_IN_SHARED_RESOURCE':
      return 'info'
    case 'MISSING':
      return 'error'
    default:
      return 'default'
  }
}
</script>

<style scoped>
.ai-import-report__suggestions,
.ai-import-report__confirmed {
  margin-top: 0.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.ai-import-report__confirmed {
  flex-direction: row;
  align-items: center;
}

/* The letters that differ from the file's spelling: what the user actually has to check. */
.ai-import-report__diff {
  font-weight: 600;
  text-decoration: underline;
  text-decoration-color: var(--bs-warning, #ffc107);
  text-decoration-thickness: 2px;
}

.ai-import-report__suggestion {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

/*
 * Even folded away, an open category can hold hundreds of rows. Capping it keeps the panel usable
 * beside the conversation instead of pushing everything else off the screen.
 */
.ai-import-report__list {
  max-height: 24rem;
  overflow-y: auto;
}

/*
 * The app forces every .btn-sm to 32px wide for the icon-only action buttons in its tables
 * (opensilex-front/front/src/styles/common.scss). A labelled button in this panel needs its width
 * back; a scoped selector is specific enough to win without !important.
 */
.btn-sm {
  width: auto;
}
</style>
