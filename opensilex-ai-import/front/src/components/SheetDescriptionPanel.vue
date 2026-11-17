<template>
  <!--
    The sheets of the files, one tab each: what the rows are, what each column is, and what is wrong
    with that. Every choice goes to the server at once, which answers with what it contradicts; the
    panel keeps no copy of its own to drift from it.
  -->
  <opensilex-Card v-if="sheets.length" label="AiImport.advanced.sheets.title" :no-footer="true">
    <template #body>
      <p class="text-body-secondary small">{{ t('AiImport.advanced.sheets.hint') }}</p>

      <nav class="tabs ai-import-tabs mb-2">
        <button
          v-for="sheet in sheets"
          :key="sheet.sheet"
          type="button"
          :class="['tab', { active: sheet.sheet === currentName }]"
          @click="currentName = sheet.sheet"
        >
          {{ sheet.sheet }}
          <span class="tabBadge">{{ sheet.row_count }}</span>
        </button>
      </nav>

      <div v-if="current" class="d-flex flex-column gap-2">
        <div class="d-flex flex-wrap align-items-end gap-3">
          <label class="d-flex flex-column small">
            <span class="mb-1">{{ t('AiImport.advanced.sheets.kind') }}</span>
            <select
              class="ai-import-kind"
              :disabled="busy"
              @change="onKindChange(($event.target as HTMLSelectElement).value)"
            >
              <option
                v-for="choice in kinds"
                :key="choice.value"
                :value="choice.value"
                :selected="choice.value === current.kind"
              >
                {{ t(choice.label_key ?? '') }}
              </option>
            </select>
          </label>
          <span v-if="!current.kind_chosen" class="text-body-secondary small">
            {{ t('AiImport.advanced.sheets.kindGuessed') }}
          </span>

          <div v-if="current.base_type" class="flex-grow-1 ai-import-type">
            <opensilex-TypeForm
              :type="current.rdf_type ?? undefined"
              :baseType="current.base_type"
              :label="t('AiImport.advanced.sheets.type')"
              :required="false"
              @update:type="onTypeChange"
            />
          </div>
        </div>

        <!--
          A sheet of levels with no column that names their factor: every row is a level of one factor,
          which the user names here. The factor itself is created with the platform's factor form.
        -->
        <div v-if="needsFactorName" class="d-flex flex-wrap align-items-end gap-2">
          <label class="d-flex flex-column small ai-import-factor">
            <span class="mb-1">{{ t('AiImport.advanced.sheets.factor') }}</span>
            <n-input
              size="small"
              :value="current.factor ?? ''"
              :disabled="busy"
              :placeholder="t('AiImport.advanced.sheets.factor')"
              @change="onFactorChange"
            />
          </label>
          <span class="text-body-secondary small">{{ t('AiImport.advanced.sheets.factorHint') }}</span>
        </div>

        <!--
          A template looks like this sheet and the user has described nothing of it: what it says is
          offered, never applied before they choose.
        -->
        <div v-if="suggestion" class="ai-import-suggestion small">
          <span>
            {{ t('AiImport.templates.proposedFrom', {
              name: suggestion.template_name, author: suggestion.author_name,
              date: shortDate(suggestion.updated_at), recognised: suggestion.recognised, total: suggestion.total
            }) }}
          </span>
          <n-space>
            <n-button size="tiny" type="primary" :disabled="busy" @click="onSuggestionApply">
              {{ t('AiImport.templates.apply') }}
            </n-button>
            <n-button size="tiny" quaternary @click="onSuggestionIgnore">
              {{ t('AiImport.templates.ignore') }}
            </n-button>
          </n-space>
        </div>

        <ul v-if="problems.length" class="ai-import-problems small mb-0 ps-4">
          <li v-for="(problem, index) in problems" :key="index">{{ problem }}</li>
        </ul>

        <opensilex-ai-import-SheetPreviewTable
          v-if="current.kind !== 'IGNORED'"
          :sheet="current"
          :roles="rolesOfKind"
          :properties="properties"
          @role="onRoleChange"
          @property="onPropertyChange"
        />
        <p v-else class="text-body-secondary small mb-0">{{ t('AiImport.advanced.sheets.ignored') }}</p>
      </div>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import type { ChoiceDTO, SheetDescriptionDTO, SheetDescriptionUpdateDTO, TypePropertyDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { useReportMessage } from '../reportMessage'

const ROLE_UNREAD = 'UNREAD'

//#region Public
const emit = defineEmits<{
  describe: [change: SheetDescriptionUpdateDTO]
  /** A type was chosen whose properties the panel needs to offer for the columns that are some. */
  needProperties: [kind: string, type: string]
}>()

const props = defineProps<{
  sheets: SheetDescriptionDTO[]
  kinds: ChoiceDTO[]
  roles: ChoiceDTO[]
  /** The properties of a type, for a kind of sheet. */
  propertiesOf: (kind: string, type?: string) => TypePropertyDTO[]
  busy: boolean
}>()
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const say = useReportMessage($opensilex)
//#endregion

//#region Data & Computed
const currentName = ref<string | null>(props.sheets[0]?.sheet ?? null)

const current = computed(() => props.sheets.find((sheet) => sheet.sheet === currentName.value))

/**
 * Only what the kind of the sheet can use is offered for a column: the table of roles is the
 * server's, and says which kinds accept each.
 */
const rolesOfKind = computed(() =>
  props.roles.filter((role) => role.value === ROLE_UNREAD || (role.accepted_by ?? []).includes(current.value?.kind ?? ''))
)

const properties = computed(() => props.propertiesOf(current.value?.kind ?? '', current.value?.rdf_type))

/**
 * The sheets whose proposal the user turned down, for as long as the page is open: nothing is
 * stored, so the next visit proposes again.
 */
const ignored = ref<string[]>([])

const suggestion = computed(() =>
  current.value?.suggestion && !ignored.value.includes(current.value.sheet ?? '') ? current.value.suggestion : undefined
)

/**
 * A sheet of levels is read with one factor for all its rows when no column names it.
 */
const needsFactorName = computed(
  () => current.value?.kind === 'FACTORS' && !Object.values(current.value.roles ?? {}).includes('FACTOR')
)

const problems = computed(() => (current.value?.problems ?? []).map((problem) => say(problem)))
//#endregion

//#region Hooks & watchers
watch(
  () => props.sheets,
  (sheets) => {
    if (!sheets.some((sheet) => sheet.sheet === currentName.value)) {
      currentName.value = sheets[0]?.sheet ?? null
    }
  }
)

watch(
  () => [current.value?.kind, current.value?.rdf_type],
  () => {
    if (current.value?.rdf_type) {
      emit('needProperties', current.value.kind ?? '', current.value.rdf_type)
    }
  },
  { immediate: true }
)
//#endregion

//#region Event handlers
/**
 * Applies what the template proposes as the user's own choice: it goes to the server like any other,
 * for the columns the sheet has, and is theirs from then on.
 */
function onSuggestionApply() {
  const proposal = suggestion.value
  if (!proposal) {
    return
  }
  const headers = current.value?.headers ?? []
  const keep = (map?: { [header: string]: string }) =>
    Object.fromEntries(Object.entries(map ?? {}).filter(([header]) => headers.includes(header)))
  describeCurrent({
    kind: proposal.kind,
    roles: keep(proposal.roles),
    rdf_type: proposal.rdf_type,
    properties: keep(proposal.properties)
  })
}

function onSuggestionIgnore() {
  ignored.value = [...ignored.value, current.value?.sheet ?? '']
}

function onFactorChange(factor: string) {
  describeCurrent({ factor })
}

function onKindChange(kind: string) {
  describeCurrent({ kind })
}

function onTypeChange(type: string | string[] | undefined) {
  const chosen = Array.isArray(type) ? type[0] : type
  if (chosen && chosen !== current.value?.rdf_type) {
    describeCurrent({ rdf_type: chosen })
  }
}

function onRoleChange(header: string, role: string) {
  describeCurrent({ roles: { [header]: role } })
}

function onPropertyChange(header: string, property: string) {
  describeCurrent({ properties: { [header]: property } })
}
//#endregion

//#region Methods
function t(key: string, params?: { [name: string]: unknown }): string {
  return String($opensilex.$i18n.t(key, params))
}

function shortDate(value?: string): string {
  return value ? value.substring(0, 10) : ''
}

function describeCurrent(change: Omit<SheetDescriptionUpdateDTO, 'sheet'>) {
  if (current.value) {
    emit('describe', { sheet: current.value.sheet, ...change })
  }
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
/*
 * Many sheets must stay visible: the tabs wrap onto a second line rather than run past the frame,
 * whatever the theme does to `.tabs` (which only sets how a single row looks).
 */
.ai-import-tabs {
  display: flex;
  flex-wrap: wrap;
  overflow: visible;
  white-space: normal;
  row-gap: 0.25rem;
}

.ai-import-tabs .tab {
  flex: 0 0 auto;
}

.ai-import-problems {
  padding-top: 0.4rem;
  padding-bottom: 0.4rem;
  border: 1px solid var(--bs-warning-border-subtle, #ffe69c);
  border-radius: 0.25rem;
  background: var(--bs-warning-bg-subtle, #fff3cd);
}

.ai-import-suggestion {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.4rem 0.6rem;
  border: 1px solid var(--bs-info-border-subtle, #9eeaf9);
  border-radius: 0.25rem;
  background: var(--bs-info-bg-subtle, #cff4fc);
}

.ai-import-kind {
  min-width: 14rem;
  padding: 0.25rem 0.4rem;
  border: 1px solid var(--bs-border-color, #ced4da);
  border-radius: 0.25rem;
  background: var(--bs-body-bg, #fff);
  color: inherit;
}

.ai-import-factor {
  min-width: 16rem;
}

.ai-import-type {
  min-width: 18rem;
}
</style>
