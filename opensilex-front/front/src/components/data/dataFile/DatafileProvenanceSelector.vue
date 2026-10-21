<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="provenancesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadProvenances"
    :placeholder="
      multiple
        ? t('component.datafile.provenance-selector.placeholder-multiple')
        : t('component.datafile.provenance-selector.placeholder')
    "
    :noResultsText="t('component.datafile.provenance-selector.filter-search-no-result')"
    :disableBranchNodes="true"
    :showCount="true"
    :actionHandler="actionHandler"
    :viewHandler="viewHandler"
    :required="required"
    :viewHandlerDetailsVisible="viewHandlerDetailsVisible"
    @clear="emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter="onEnter"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { useI18n } from 'vue-i18n'

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:provenances', value: any): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const props = withDefaults(defineProps<{
  actionHandler?: Function
  provenances?: any
  required?: boolean
  label?: string
  experiments?: any
  targets?: any
  devices?: any
  multiple?: boolean
  viewHandler?: Function
  viewHandlerDetailsVisible?: boolean
}>(), {
  required: false,
  label: 'component.data.provenance.search',
  multiple: false,
  viewHandlerDetailsVisible: false
})

const formSelector = ref<any>(null)

const provenancesURI = computed({
  get: () => props.provenances,
  set: (value) => emit('update:provenances', value)
})

function refresh() {
  formSelector.value?.refresh?.()
}

function select(value: any) {
  emit('select', value)
}

function deselect(value: any) {
  emit('deselect', value)
}

function onEnter() {
  emit('handlingEnterKey')
}

function loadProvenances() {
  return $opensilex
    .getService('opensilex.DataService')
    .getDatafilesProvenancesByTargets(
      props.experiments,
      props.devices,
      props.targets
    )
    .then((http: any) => {
      return http.response.result
    })
}

defineExpose({
  refresh
})
</script>

<style scoped>
</style>
