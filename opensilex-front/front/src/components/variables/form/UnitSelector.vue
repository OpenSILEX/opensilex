<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="unitURI"
    :multiple="multiple"
    :searchMethod="searchUnits"
    :itemLoadingMethod="loadUnits"
    :placeholder="placeholder"
    :helpMessage="helpMessage"
    :actionHandler="actionHandler"
    :required="required"
    noResultsText="component.unit.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { ref, computed, inject, watch } from 'vue'
import FormSelector from '../../common/forms/FormSelector.vue'
import type { UnitGetDTO } from 'opensilex-core/index'
import type OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type HttpResponse from 'opensilex-security/HttpResponse'

// Props
const props = defineProps<{
  selected: string | string[]
  label?: string
  multiple?: boolean
  helpMessage?: string
  actionHandler?: Function
  required?: boolean
  sharedResourceInstance?: string
}>()

const emit = defineEmits<{
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')

const unitURI = ref(props.selected)
const formSelector = ref<InstanceType<typeof FormSelector>>()

const pageSize = ref(10)

watch(() => props.sharedResourceInstance, () => {
  formSelector.value?.refresh()
})

const placeholder = computed(() =>
  props.multiple
    ? 'component.unit.form.selector.placeholder-multiple'
    : 'component.unit.form.selector.placeholder'
)

const loadUnits = (units: string[]): Promise<UnitGetDTO[]> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .getUnitsByURIs(units, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<UnitGetDTO[]>>) => http.response.result)
    .catch($opensilex!.errorHandler)
}

const searchUnits = (name: string, page: number, pageSize: number): Promise<HttpResponse<OpenSilexResponse<UnitGetDTO[]>>> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .searchUnits(name, ['name=asc'], page, pageSize, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<UnitGetDTO[]>>) => http)
}

const select = (value: any) => {
  emit('select', value)
}

const deselect = (value: any) => {
  emit('deselect', value)
}

const onEnter = () => {
  emit('handlingEnterKey')
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  component: 
    unit: 
      form:
        selector:
          placeholder : Select one unit
          placeholder-multiple : Select one or more units
          filter-search-no-result : No units found
fr:
  component: 
    unit: 
      form: 
        selector:
          placeholder : Sélectionner une unité
          placeholder-multiple : Sélectionner une ou plusieurs unités
          filter-search-no-result : Aucune unité trouvée
</i18n>
