<template>
  <div class="graph-import-evidence">
    <p v-if="isLeftOut" class="small text-body-secondary mb-1">{{ $t('GraphImport.understand.leftOutNote') }}</p>
    <p v-for="(note, index) in column.property?.notes ?? []" :key="'note' + index" class="small text-warning-emphasis mb-1">
      {{ reportText($opensilex, note) }}
    </p>

    <n-list v-if="candidates.length" size="small" :show-divider="false">
      <n-list-item v-for="candidate in candidates" :key="candidate.uri">
        <div class="d-flex align-items-center gap-2">
          <strong>{{ candidate.label }}</strong>
          <span class="text-body-secondary small">{{ describe(candidate.uri) }}</span>
          <n-tag size="small" :bordered="false">{{ percent(candidate.score) }}</n-tag>
          <n-button
            size="tiny"
            :type="candidate.uri === chosen ? 'success' : 'default'"
            :secondary="candidate.uri !== chosen"
            @click="emit('choose', candidate.uri)"
          >
            {{ $t(candidate.uri === chosen && isChosen ? 'GraphImport.understand.chosen' : 'GraphImport.understand.choose') }}
          </n-button>
        </div>
        <ul class="graph-import-evidence__reasons small mb-0">
          <li v-for="(evidence, index) in candidate.evidence ?? []" :key="index">
            <span :class="(evidence.weight ?? 0) < 0 ? 'text-danger' : 'text-success'">{{ signed(evidence.weight) }}</span>
            {{ reportText($opensilex, evidence.reason) }}
          </li>
        </ul>
      </n-list-item>
    </n-list>
    <p v-else class="text-body-secondary small mb-2">{{ $t('GraphImport.understand.noCandidate') }}</p>

    <div v-if="properties.length" class="d-flex align-items-end gap-2 mt-2">
      <div class="graph-import-evidence__other">
        <opensilex-FormSelector
          label="GraphImport.understand.otherProperty"
          :selected="otherProperty"
          :options="propertyOptions"
          :multiple="false"
          :placeholder="$t('GraphImport.understand.otherPropertyPlaceholder')"
          @update:selected="onFormSelectorUpdate"
        />
      </div>
      <n-button v-if="isChosen" size="small" tertiary @click="emit('choose', undefined)">
        {{ $t('GraphImport.understand.backToSuggestion') }}
      </n-button>
      <n-button v-if="isLeftOut" size="small" tertiary @click="emit('choose', undefined)">
        {{ $t('GraphImport.understand.takeBack') }}
      </n-button>
      <n-button v-else size="small" tertiary @click="emit('leaveOut')">
        {{ $t('GraphImport.understand.leaveOut') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { CandidateDTO, ColumnInterpretationDTO, PropertyOptionDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { percent, reportText, signed } from '../reasons'

/** An option of the shared selector. */
interface SelectorOption {
  id: string
  label: string
}

//#region Public
const emit = defineEmits<{
  /** The property the column holds; undefined gives the column back to the suggestions. */
  choose: [property: string | undefined]
  /** Leave the column out: no value, and no suggestion again. */
  leaveOut: []
}>()

const props = defineProps<{
  column: ColumnInterpretationDTO
  /** Every property the class of the sheet accepts. */
  properties: PropertyOptionDTO[]
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const candidates = computed<CandidateDTO[]>(() => props.column.property?.candidates ?? [])

const chosen = computed<string | undefined>(() => props.column.property?.chosen_uri)

const isChosen = computed(() => props.column.property?.status === 'CHOSEN')

const isLeftOut = computed(() => props.column.property?.status === 'LEFT_OUT')

/** The property chosen when it is none of the candidates, so that the selector shows it. */
const otherProperty = computed<string | undefined>(() =>
  candidates.value.some((candidate) => candidate.uri === chosen.value) ? undefined : chosen.value
)

const propertyOptions = computed<SelectorOption[]>(() =>
  props.properties.map((property) => ({ id: property.uri, label: property.label + ' · ' + describe(property.uri) }))
)
//#endregion

//#region Event handlers
function onFormSelectorUpdate(selected: string | string[] | undefined) {
  const property = Array.isArray(selected) ? selected[0] : selected
  if (property) {
    emit('choose', property)
  }
}
//#endregion

//#region Methods
/** What a property holds and how many values: "date · 0..1". */
function describe(uri: string | undefined): string {
  const property = props.properties.find((option) => option.uri === uri)
  if (!property) {
    return ''
  }
  const max = property.max_values ?? '*'
  return `${property.range_label ?? ''} · ${property.min_values ?? 0}..${max}`
}

//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-evidence {
  padding: 0.25rem 0.5rem;
}

.graph-import-evidence__reasons {
  padding-left: 1.25rem;
}

.graph-import-evidence__other {
  min-width: 22rem;
}
</style>
