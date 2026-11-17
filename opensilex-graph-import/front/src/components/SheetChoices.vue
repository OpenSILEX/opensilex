<template>
  <div class="graph-import-sheet-choices">
    <div>
      <div class="d-flex align-items-center gap-2 mb-1">
        <span class="small fw-semibold">{{ $t('GraphImport.understand.class') }}</span>
        <ChoiceStatusTag :status="sheet.sheet_class?.status" />
        <n-button v-if="sheet.sheet_class?.status === 'SUGGESTED'" size="tiny" tertiary @click="onAcceptClass">
          {{ $t('GraphImport.understand.accept') }}
        </n-button>
      </div>
      <opensilex-FormSelector
        :selected="sheet.sheet_class?.chosen_uri"
        :options="classOptions"
        :multiple="false"
        :collapsed="true"
        :placeholder="$t('GraphImport.understand.classPlaceholder')"
        @update:selected="onClassSelectorUpdate"
      />
    </div>
    <div>
      <div class="d-flex align-items-center gap-2 mb-1">
        <span class="small fw-semibold">{{ $t('GraphImport.understand.identifier') }}</span>
        <ChoiceStatusTag :status="sheet.identifier?.status" />
        <n-button v-if="sheet.identifier?.status === 'SUGGESTED'" size="tiny" tertiary @click="onAcceptIdentifier">
          {{ $t('GraphImport.understand.accept') }}
        </n-button>
      </div>
      <opensilex-FormSelector
        :selected="identifierSelected"
        :options="columnOptions"
        :multiple="false"
        :placeholder="$t('GraphImport.understand.identifierPlaceholder')"
        @update:selected="onIdentifierSelectorUpdate"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ClassOptionDTO, SheetInterpretationDTO } from '../lib'
import { decidedSoFar, type SheetDecision } from '../decisions'
import ChoiceStatusTag from './ChoiceStatusTag.vue'

/** An option of the shared selector, with its subclasses for a class. */
interface SelectorOption {
  id: string
  label: string
  children?: SelectorOption[]
  /** Open when the tree opens folded: the branches leading to the class shown. */
  isDefaultExpanded?: boolean
}

//#region Public
const emit = defineEmits<{
  /** The whole decision about the sheet, which replaces the previous one. */
  decide: [decision: SheetDecision]
}>()

const props = defineProps<{
  sheet: SheetInterpretationDTO
  /** The classes a sheet may hold, as trees. */
  classes: ClassOptionDTO[]
}>()
//#endregion

//#region Private
//#region Data & Computed
/** The classes as a folded tree, the branch of the class shown left open so that it can be seen. */
const classOptions = computed<SelectorOption[]>(() => props.classes.map(toOption))

const columnOptions = computed<SelectorOption[]>(() =>
  (props.sheet.columns ?? []).map((column) => ({ id: String(column.index), label: column.header ?? String(column.index) }))
)

const identifierSelected = computed<string | undefined>(() =>
  props.sheet.identifier?.chosen_column === undefined ? undefined : String(props.sheet.identifier.chosen_column)
)
//#endregion

//#region Event handlers
function onClassSelectorUpdate(selected: string | string[] | undefined) {
  const classUri = Array.isArray(selected) ? selected[0] : selected
  if (classUri && classUri !== props.sheet.sheet_class?.chosen_uri) {
    // The column properties and the values given belong to the former class; they go back to the suggestions.
    emit('decide', { ...decidedSoFar(props.sheet), class_uri: classUri, columns: [], left_out: [], given: [] })
  }
}

function onIdentifierSelectorUpdate(selected: string | string[] | undefined) {
  const column = Array.isArray(selected) ? selected[0] : selected
  if (column !== undefined) {
    emit('decide', { ...decidedSoFar(props.sheet), identifier_column: Number(column) })
  }
}

function onAcceptClass() {
  emit('decide', { ...decidedSoFar(props.sheet), class_uri: props.sheet.sheet_class?.chosen_uri })
}

function onAcceptIdentifier() {
  emit('decide', { ...decidedSoFar(props.sheet), identifier_column: props.sheet.identifier?.chosen_column })
}
//#endregion

//#region Methods
function toOption(option: ClassOptionDTO): SelectorOption {
  const children = (option.children ?? []).map(toOption)
  const shown = props.sheet.sheet_class?.chosen_uri
  return {
    id: option.uri!,
    label: option.label ?? option.uri!,
    children: children.length ? children : undefined,
    isDefaultExpanded: children.some((child) => child.id === shown || child.isDefaultExpanded)
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-sheet-choices {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
  gap: 1rem;
}
</style>
