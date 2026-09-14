<template>
  <div class="ai-import-proposal">
    <div class="fw-semibold small mb-1">
      {{ $t('AiImport.proposal.title') }} —
      {{ $t('AiImport.proposal.target_' + proposal.target) }}
    </div>

    <p v-if="proposal.rationale" class="small text-body-secondary mb-2">
      {{ proposal.rationale }}
    </p>

    <!-- Applied or cancelled: the card stays, as the record of what was decided. -->
    <div v-if="proposal.status === 'APPLIED'" class="alert alert-success py-2 small mb-0">
      {{ appliedMessage }}
    </div>

    <div v-else-if="proposal.status === 'CANCELLED'" class="alert alert-secondary py-2 small mb-0">
      {{ $t('AiImport.proposal.cancelled') }}
    </div>

    <template v-else>
      <!--
        A refusal, not a failure: the server resolved every row before writing, found some it could
        not place, and wrote nothing. The draft stays open so the user fixes what is named below and
        validates again.
      -->
      <div v-if="refusal" class="alert alert-warning py-2 small">
        <div class="fw-semibold">{{ $t('AiImport.proposal.refused.title') }}</div>
        <p class="mb-1">
          {{ $t('AiImport.proposal.refused.explanation', { count: refusal.unresolved_count }) }}
        </p>
        <ul class="mb-0 ps-3">
          <li v-for="(row, index) in refusal.unresolved" :key="index">
            {{ $t('AiImport.proposal.refused.row', { sheet: row.sheet, row: row.row }) }} —
            {{ $t(row.reason_key, { value: row.value }) }}
          </li>
        </ul>
        <div v-if="refusal.unresolved_count > refusal.unresolved.length" class="mt-1">
          {{ $t('AiImport.proposal.refused.more', {
            count: refusal.unresolved_count - refusal.unresolved.length
          }) }}
        </div>
      </div>

      <div v-if="proposal.blockers && proposal.blockers.length" class="alert alert-danger py-2 small">
        <div class="fw-semibold">{{ $t('AiImport.proposal.blocked') }}</div>
        <ul class="mb-0 ps-3">
          <li v-for="(blocker, index) in proposal.blockers" :key="index">{{ $t(blocker) }}</li>
        </ul>
      </div>

      <!--
        The fields are handed to the form components OpenSILEX already ships: the same selector the
        variables screen uses for a unit, the same date field, the same checkbox. A URI is chosen
        from what exists rather than pasted, and a field looks here exactly as it does everywhere
        else in the instance.
      -->
      <div class="ai-import-fields">
        <template v-for="field in proposal.fields" :key="field.name">

          <!-- A referential: the selector knows how to search it. -->
          <component
            v-if="selectorFor(field)"
            :is="selectorFor(field).component"
            v-bind="selectorBindings(field)"
            :label="labelOf(field)"
            :required="field.required"
            :multiple="field.kind === 'uri-list'"
            @[selectorFor(field).updateEvent]="(value) => setUri(field, value)"
          />

          <!--
            A decision rather than a value. The sentence the profile wrote sits under the box: it
            explains that the mismatch comes from the template and not from the user's data, and
            ticking without reading it would make the confirmation worthless.
          -->
          <div v-else-if="field.kind === 'boolean'" class="ai-import-decision">
            <opensilex-CheckboxForm
              :value="edited[field.name] === 'true'"
              :label="labelOf(field)"
              :required="field.required"
              @update:value="(checked) => (edited[field.name] = String(checked))"
            />
            <p v-if="field.value" class="ai-import-decision-text">{{ field.value }}</p>
          </div>

          <opensilex-DateForm
            v-else-if="field.kind === 'date'"
            :value="edited[field.name]"
            :label="labelOf(field)"
            :required="field.required"
            :disabled="submitting"
            @update:value="(value) => (edited[field.name] = value ?? '')"
          />

          <opensilex-TextAreaForm
            v-else-if="field.kind === 'textarea'"
            :value="edited[field.name]"
            :label="labelOf(field)"
            :required="field.required"
            :disabled="submitting"
            @update:value="(value) => (edited[field.name] = value ?? '')"
          />

          <opensilex-InputForm
            v-else
            :value="edited[field.name]"
            :label="labelOf(field)"
            :required="field.required"
            :disabled="submitting"
            @update:value="(value) => (edited[field.name] = value ?? '')"
          />

          <p v-if="isEmpty(field)" class="ai-import-field-note missing">
            {{ $t('AiImport.proposal.missing') }}
          </p>
          <p v-else-if="field.kind !== 'boolean' && sourceOf(field)" class="ai-import-field-note">
            {{ sourceOf(field) }}
          </p>
        </template>
      </div>

      <div class="d-flex gap-2">
        <n-button
          type="primary"
          size="small"
          :disabled="!canConfirm"
          :loading="submitting"
          @click="confirm"
        >
          {{ submitting ? $t('AiImport.proposal.submitting') : $t('AiImport.proposal.confirm') }}
        </n-button>
        <n-button size="small" secondary :disabled="submitting" @click="cancel">
          {{ $t('AiImport.proposal.cancel') }}
        </n-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, reactive, watch, ref } from 'vue'

interface ProposalField {
  name: string
  label_key?: string
  kind: string
  /** The referential the field designates, when it designates one. */
  resource?: string
  required: boolean
  value?: string
  source?: string
}

/**
 * The selectors OpenSILEX already ships for each referential.
 *
 * Each has its own model prop — a legacy of how they grew — so the name is carried here rather
 * than assumed, which is what lets one template drive all of them.
 */
const SELECTORS: { [resource: string]: { component: string; modelProp: string; updateEvent: string } } = {
  experiment: {
    component: 'opensilex-ExperimentSelector',
    modelProp: 'experiments',
    updateEvent: 'update:experiments'
  },
  project: {
    component: 'opensilex-ProjectSelector',
    modelProp: 'projects',
    updateEvent: 'update:projects'
  },
  entity: {
    component: 'opensilex-EntitySelector',
    modelProp: 'selected',
    updateEvent: 'update:selected'
  },
  characteristic: {
    component: 'opensilex-CharacteristicSelector',
    modelProp: 'selected',
    updateEvent: 'update:selected'
  },
  method: {
    component: 'opensilex-MethodSelector',
    modelProp: 'selected',
    updateEvent: 'update:selected'
  },
  unit: {
    component: 'opensilex-UnitSelector',
    modelProp: 'selected',
    updateEvent: 'update:selected'
  }
}

interface UnresolvedRow {
  sheet: string
  row: number
  column?: string
  reason_key: string
  value?: string
}

interface Refusal {
  unresolved_count: number
  unresolved: UnresolvedRow[]
}

interface Proposal {
  id: string
  target: string
  status: string
  rationale?: string
  fields: ProposalField[]
  missing_required: string[]
  blockers: string[]
  is_ready: boolean
  result_uri?: string
  inserted_count?: number
}

/**
 * A creation the assistant drafted, awaiting the user's decision.
 *
 * The values are editable: a date the assistant read wrong is corrected here rather than through
 * another exchange. What the server writes is what this card shows, and it revalidates before doing
 * so — the card is a convenience, not the authority.
 */
const props = defineProps<{ proposal: Proposal; sessionId: string }>()

const emit = defineEmits<{
  (event: 'applied', payload: { target: string; report: any }): void
  (event: 'cancelled'): void
}>()

const $opensilex: any = inject('$opensilex')

const edited = reactive<{ [field: string]: string }>({})
const submitting = ref(false)
const refusal = ref<Refusal | null>(null)

// A new draft replaces what is on screen, so its values must replace the edited ones too.
watch(
  () => props.proposal,
  (proposal) => {
    refusal.value = null
    Object.keys(edited).forEach((key) => delete edited[key])
    proposal.fields.forEach((field) => {
      // The suggested value of a checkbox is the sentence to read, not a pre-ticked answer.
      edited[field.name] = field.kind === 'boolean' ? 'false' : (field.value ?? '')
    })
  },
  { immediate: true, deep: false }
)

const canConfirm = computed(() => {
  if (submitting.value || (props.proposal.blockers?.length ?? 0) > 0) {
    return false
  }
  return props.proposal.fields.every(
    (field) => !field.required || (edited[field.name] ?? '').trim()
  )
})

// What was created reads differently per target: a URI for a project, a count of values for an
// insertion, a count of events for the trial records.
const appliedMessage = computed(() => {
  const t = (key: string, params?: any) => $opensilex.$i18n.t(key, params)
  if (props.proposal.inserted_count == null) {
    return t('AiImport.proposal.appliedUri', { uri: props.proposal.result_uri })
  }
  const key = {
    EVENT: 'AiImport.proposal.appliedEvents',
    VARIABLE: 'AiImport.proposal.appliedVariables'
  }[props.proposal.target] ?? 'AiImport.proposal.appliedCount'
  return t(key, { count: props.proposal.inserted_count })
})

function labelOf(field: ProposalField): string {
  return field.label_key ? String($opensilex.$i18n.t(field.label_key)) : field.name
}

function selectorFor(field: ProposalField) {
  return field.resource ? SELECTORS[field.resource] : undefined
}

function selectorBindings(field: ProposalField) {
  const selector = selectorFor(field)
  if (!selector) {
    return {}
  }
  const raw = (edited[field.name] ?? '').trim()
  const value = field.kind === 'uri-list' ? (raw ? raw.split(',').map((u) => u.trim()) : []) : raw
  return { [selector.modelProp]: value }
}

// A multi-valued selector hands back an array; the server takes the same comma-separated list it
// would have taken from a text field, so nothing downstream has to know a selector was used.
function setUri(field: ProposalField, value: any) {
  const flat = Array.isArray(value) ? value.filter(Boolean).join(',') : (value ?? '')
  edited[field.name] = String(flat)
}

function isEmpty(field: ProposalField): boolean {
  return field.required && !(edited[field.name] ?? '').trim()
}

function sourceOf(field: ProposalField): string | null {
  // An edited value is the user's, whatever the server recorded when it drafted it.
  if ((edited[field.name] ?? '') !== (field.value ?? '')) {
    return $opensilex.$i18n.t('AiImport.proposal.source_USER')
  }
  return field.source ? $opensilex.$i18n.t('AiImport.proposal.source_' + field.source) : null
}

function service() {
  return $opensilex.getService('opensilex-ai-import.AiImportService')
}

async function confirm() {
  if (!canConfirm.value) {
    return
  }
  submitting.value = true
  try {
    const http = await service().create({
      session_id: props.sessionId,
      proposal_id: props.proposal.id,
      values: { ...edited }
    })
    const result = http.response.result
    if (result.refused) {
      refusal.value = {
        unresolved_count: result.unresolved_count,
        unresolved: result.unresolved ?? []
      }
      return
    }
    refusal.value = null
    emit('applied', { target: result.target, report: result.report })
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    submitting.value = false
  }
}

async function cancel() {
  try {
    await service().cancelProposal({
      session_id: props.sessionId,
      proposal_id: props.proposal.id
    })
    emit('cancelled')
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}
</script>

<style scoped>
.ai-import-fields {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.ai-import-field-note {
  margin: -0.15rem 0 0.35rem;
  font-size: 0.8rem;
  color: var(--bs-secondary-color, #6c757d);
}

.ai-import-field-note.missing {
  color: var(--bs-danger, #dc3545);
}

.ai-import-decision-text {
  margin: 0.15rem 0 0;
  font-size: 0.85rem;
  color: var(--bs-secondary-color, #6c757d);
}

.ai-import-proposal {
  margin-top: 0.5rem;
  padding: 0.75rem;
  border: 1px solid rgba(13, 110, 253, 0.35);
  border-radius: 0.5rem;
  background-color: rgba(13, 110, 253, 0.04);
}

/*
 * The app forces every .btn-sm to 32px wide for the icon-only action buttons in its tables
 * (opensilex-front/front/src/styles/common.scss). A labelled button needs its width back.
 */
.btn-sm {
  width: auto;
}
</style>
