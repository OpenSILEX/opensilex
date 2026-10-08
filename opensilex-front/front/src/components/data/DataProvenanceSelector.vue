<template>
  <FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="provenancesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadProvenances"
    :placeholder="
      placeholder ??
      (multiple
        ? t('component.data.form.selector.placeholder-multiple')
        : t('component.data.form.selector.placeholder'))
    "
    :noResultsText="t('component.data.form.selector.filter-search-no-result')"
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
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse, ProvenanceGetDTO } from 'opensilex-core/index'

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const emit = defineEmits<{
  'update:provenances': [value: any]
  select: [value: any]
  deselect: [value: any]
  clear: []
  handlingEnterKey: []
}>()

const props = withDefaults(defineProps<{
  actionHandler?: Function
  provenances?: any
  required?: boolean
  label?: string
  placeholder?: string
  experiments?: any
  targets?: any
  devices?: any
  variables?: any
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

/**
 * Load only the provenances used by data of the given experiments/variables/devices/targets
 */
function loadProvenances() {
  return $opensilex
    .getService('opensilex.DataService')
    .getUsedProvenancesByTargets(
      props.experiments,
      props.variables,
      props.devices,
      props.targets
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => {
      return http.response.result
    })
}

defineExpose({
  refresh
})
</script>
