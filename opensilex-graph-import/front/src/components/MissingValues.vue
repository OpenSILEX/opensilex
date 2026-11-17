<template>
  <div v-if="entries.length" class="graph-import-missing">
    <div class="small fw-semibold mb-1">{{ $t('GraphImport.missing.title') }}</div>
    <p class="small text-body-secondary mb-2">{{ $t('GraphImport.missing.help') }}</p>
    <div v-for="entry in entries" :key="entry.property.uri" class="graph-import-missing__entry mb-2">
      <div class="d-flex align-items-center gap-2 flex-wrap">
        <strong class="small">{{ entry.property.label }}</strong>
        <n-tag v-if="entry.required" size="tiny" type="warning" :bordered="false">{{ $t('GraphImport.missing.required') }}</n-tag>
        <template v-if="entry.given !== undefined">
          <n-tag size="small" type="info" :bordered="false">{{ $t('GraphImport.missing.given', { value: entry.given }) }}</n-tag>
          <n-button size="tiny" tertiary :disabled="busy" @click="onRemoveClick(entry)">{{ $t('GraphImport.missing.remove') }}</n-button>
        </template>
      </div>
      <div v-if="entry.given === undefined" class="d-flex align-items-center gap-2 mt-1">
        <n-input
          v-model:value="drafts[entry.property.uri!]"
          size="small"
          :placeholder="$t(entry.link ? 'GraphImport.missing.linkPlaceholder' : 'GraphImport.missing.valuePlaceholder')"
          :disabled="busy"
          @keydown.enter="onGiveClick(entry)"
        />
        <n-button size="small" :disabled="busy || !drafts[entry.property.uri!]?.trim()" @click="onGiveClick(entry)">
          {{ $t('GraphImport.missing.give') }}
        </n-button>
      </div>
      <div v-if="entry.unresolved" class="d-flex align-items-center gap-2 mt-1">
        <span class="small text-danger">{{ $t('GraphImport.missing.unresolved', { value: entry.given }) }}</span>
        <n-button v-if="entry.declarable" size="tiny" type="primary" secondary :disabled="busy" @click="onDeclareClick(entry)">
          {{ $t('GraphImport.missing.declare', { name: entry.given, class: entry.rangeLabel }) }}
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ClassOptionDTO, PropertyOptionDTO, SheetInterpretationDTO, ValidationDTO } from '../lib'
import { type SheetDecision, withGiven } from '../decisions'
import { linksOfColumn } from '../understandGraph'

/** A property the sheet needs or was given a value for, and what the user may do about it. */
interface MissingEntry {
  property: PropertyOptionDTO
  required: boolean
  link: boolean
  /** The value given for every row; undefined while none is. */
  given?: string
  /** Whether the value given points to nothing the check could find. */
  unresolved: boolean
  /** Whether the user may declare, in the assistant, the resource the value names. */
  declarable: boolean
  rangeLabel: string
}

//#region Public
const emit = defineEmits<{
  /** The whole decision about the sheet, which replaces the previous one. */
  decide: [decision: SheetDecision]
  /** Declare a resource of this class under this name, since nothing of that name exists. */
  declare: [classUri: string, name: string]
}>()

const props = defineProps<{
  sheet: SheetInterpretationDTO
  /** What the check of the graph says, to tell a given link that points to nothing. */
  validation?: ValidationDTO
  /** The classes the assistant may declare a resource of, as trees. */
  classes: ClassOptionDTO[]
  busy: boolean
}>()
//#endregion

//#region Private
//#region Data & Computed
/** What the user is typing for each property, by URI. */
const drafts = ref<Record<string, string>>({})

const declarableClasses = computed<Set<string>>(() => {
  const uris = new Set<string>()
  const walk = (options: ClassOptionDTO[]) =>
    options.forEach((option) => {
      if (option.uri) {
        uris.add(option.uri)
      }
      walk(option.children ?? [])
    })
  walk(props.classes)
  return uris
})

const entries = computed<MissingEntry[]>(() => {
  const given = new Map((props.sheet.given ?? []).map((value) => [value.property, value.value]))
  const wanted = new Set([...(props.sheet.unfilled_required ?? []), ...given.keys()])
  return (props.sheet.properties ?? [])
    .filter((property) => property.uri && wanted.has(property.uri))
    .map((property) => {
      const value = given.get(property.uri!)
      const link = property.kind === 'OBJECT'
      const column = (props.sheet.columns ?? []).find(
        (candidate) => candidate.given && candidate.property?.chosen_uri === property.uri
      )
      const unresolved =
        link && value !== undefined &&
        linksOfColumn(props.validation, props.sheet.name ?? '', column?.index, property.uri).some(
          (found) => found.kind === 'UNRESOLVED'
        )
      return {
        property,
        required: (property.min_values ?? 0) > 0,
        link,
        given: value,
        unresolved,
        declarable: unresolved && !!property.range && declarableClasses.value.has(property.range),
        rangeLabel: property.range_label ?? ''
      }
    })
})
//#endregion

//#region Event handlers
function onGiveClick(entry: MissingEntry) {
  const value = drafts.value[entry.property.uri!]?.trim()
  if (props.busy || !value) {
    return
  }
  drafts.value = { ...drafts.value, [entry.property.uri!]: '' }
  emit('decide', withGiven(props.sheet, entry.property.uri!, value))
}

function onRemoveClick(entry: MissingEntry) {
  if (!props.busy) {
    emit('decide', withGiven(props.sheet, entry.property.uri!, undefined))
  }
}

function onDeclareClick(entry: MissingEntry) {
  if (!props.busy && entry.property.range && entry.given) {
    emit('declare', entry.property.range, entry.given)
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-missing {
  border-left: 3px solid var(--bs-warning, #f0a020);
  padding: 0.25rem 0 0.25rem 0.75rem;
}

.graph-import-missing__entry :deep(.n-input) {
  max-width: 22rem;
}
</style>
