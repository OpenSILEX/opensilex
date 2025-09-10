<template>
  <opensilex-ModalFormSelector
    ref="variableSelector"
    modalComponent="opensilex-VariableModalList"
    :label="label"
    :placeholder="placeholder"

    v-model:selected="variablesURI"
    v-model:selectedInJsonFormat="variablesAsSelectableItems"

    :experiment="experiment"
    :objects="objects"
    :devices="devices"
    :required="required"
    :multiple="true"
    :maximumSelectedItems="maximumSelectedRows"
    :withAssociatedData="withAssociatedData"
    :limit="4"

    @clear="refreshVariableSelector"
    @select="select"
    @deselect="deselect"
    @onValidate="onValidate"
    @hide="$emit('hideSelector')"
    @shown="$emit('shownSelector')"
  />
</template>

<script setup lang="ts">
import { ref, computed} from 'vue'
import type { SelectableItem } from '@/components/common/forms/ModalFormSelector.vue'

const variableSelector = ref()
type SelectableItem = { id: string; label: string; isDisabled?: boolean }

const props = defineProps<{
 variables?: string[]        // v-model:variables
  variablesWithLabels?: SelectableItem[] | null  // v-model:variablesWithLabels
  editMode?: boolean
  placeholder?: string
  label?: string
  required?: boolean
  withAssociatedData?: boolean
  experiment?: any
  objects?: any
  devices?: any
  maximumSelectedRows?: number
}>()

const emit = defineEmits<{
  (e: 'update:variables', value: string[] | undefined): void
  (e: 'update:variablesWithLabels', value: SelectableItem[] | null): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'validate', value: any): void
  (e: 'hideSelector'): void
  (e: 'shownSelector'): void
}>()

const variablesURI = computed<string[] | undefined>({
  get: () => props.variables,
  set: (val) => emit('update:variables', val)
})

const variablesAsSelectableItems = computed<SelectableItem[] | null>({
  get: () => props.variablesWithLabels ?? null,
  set: (val) => emit('update:variablesWithLabels', val)
})

function select(value: any) { emit('select', value) }
function deselect(value: any) { emit('deselect', value) }
function onValidate(value: any) { emit('validate', value) }

function setVariableSelectorToFirstTimeOpen () {
  variableSelector.value?.setSelectorToFirstTimeOpen?.()
}
function refreshVariableSelector () {
  variableSelector.value?.refreshModalSearch?.()
}

defineExpose({
  setVariableSelectorToFirstTimeOpen,
  refreshVariableSelector
})
</script>

<style scoped>
</style>

<i18n>
en:
  VariableSelectorWithFilter:
    placeholder: Select a variable
    placeholder-multiple: Select one or more variables
    filter-search-no-result: No variable found
fr:
  VariableSelectorWithFilter:
    placeholder: Sélectionner une variable
    placeholder-multiple: Sélectionner une ou plusieurs variables
    filter-search-no-result: Aucune variable trouvée
</i18n>
