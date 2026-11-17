<template>
  <div class="graph-import-measure">
    <div class="d-flex align-items-center gap-2 flex-wrap mb-1">
      <span class="small fw-semibold">{{ $t('GraphImport.measure.title') }}</span>
      <template v-if="column.variable">
        <span class="small">{{ column.variable_label }}</span>
        <ChoiceStatusTag :status="column.variable_status" />
        <n-button
          v-if="column.variable_status === 'SUGGESTED'"
          size="tiny"
          tertiary
          :disabled="busy"
          @click="emit('decide', withMeasure(sheet, column.index!, column.variable))"
        >
          {{ $t('GraphImport.understand.accept') }}
        </n-button>
        <n-button
          v-if="column.variable_status === 'CHOSEN'"
          size="tiny"
          tertiary
          :disabled="busy"
          @click="emit('decide', withMeasure(sheet, column.index!, undefined))"
        >
          {{ $t('GraphImport.understand.backToSuggestion') }}
        </n-button>
      </template>
      <span v-else class="small text-body-secondary">{{ $t('GraphImport.measure.none') }}</span>
    </div>
    <div class="graph-import-measure__selector">
      <opensilex-VariableSelector
        :variables="selected"
        label="GraphImport.measure.choose"
        @update:variables="onVariableSelectorUpdate"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { ColumnInterpretationDTO, SheetInterpretationDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { type SheetDecision, withMeasure } from '../decisions'
import ChoiceStatusTag from './ChoiceStatusTag.vue'

//#region Public
const emit = defineEmits<{
  /** The whole decision about the sheet, which replaces the previous one. */
  decide: [decision: SheetDecision]
}>()

const props = defineProps<{
  sheet: SheetInterpretationDTO
  /** A column of a sheet of data. */
  column: ColumnInterpretationDTO
  busy: boolean
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
/**
 * The variable in the platform's prefixed form: the shared selector matches it against the options it
 * lists, which are prefixed.
 */
const selected = computed<string | undefined>(() =>
  props.column.variable ? $opensilex.getShortUri(props.column.variable) : undefined
)
//#endregion

//#region Event handlers
function onVariableSelectorUpdate(chosen: string | string[] | undefined) {
  const variable = Array.isArray(chosen) ? chosen[0] : chosen
  if (!props.busy && variable && variable !== selected.value) {
    emit('decide', withMeasure(props.sheet, props.column.index!, variable))
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-measure {
  padding: 0.25rem 0.5rem;
}

.graph-import-measure__selector {
  max-width: 28rem;
}
</style>
