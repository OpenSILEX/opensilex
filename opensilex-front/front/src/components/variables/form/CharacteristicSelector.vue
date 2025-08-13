<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="modelSelected"
    :multiple="multiple"
    :searchMethod="searchCharacteristics"
    :itemLoadingMethod="loadCharacteristics"
    :placeholder="resolvedPlaceholder"
    :actionHandler="actionHandler"
    :required="required"
    :helpMessage="helpMessage"
    :conversionMethod="conversionMethod"
    noResultsText="component.characteristic.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import FormSelector from '../../common/forms/FormSelector.vue'
import type { CharacteristicGetDTO } from 'opensilex-core'
import type OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type HttpResponse from 'opensilex-security/HttpResponse'

const props = defineProps<{
  selected: string | string[] | undefined
  label?: string
  multiple?: boolean
  helpMessage?: string
  actionHandler?: Function
  required?: boolean
  sharedResourceInstance?: string
  conversionMethod?: Function
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: any): void     // necesaire  pour v-model
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')!
const service = $opensilex.getService<VariablesService>('opensilex.VariablesService')

// v-model proxy (plutôt que ref local)
const modelSelected = computed({
  get: () => props.selected,
  set: (v) => emit('update:selected', v)
})

const formSelector = ref<InstanceType<typeof FormSelector>>()

watch(() => props.sharedResourceInstance, () => {
  formSelector.value?.refresh?.()
})

const resolvedPlaceholder = computed(() => {
  if (props.placeholder) return props.placeholder
  return props.multiple
    ? 'component.characteristic.form.selector.placeholder-multiple'
    : 'component.characteristic.form.selector.placeholder'
})

const loadCharacteristics = (characteristics: string[]): Promise<CharacteristicGetDTO[]> => {
  return $opensilex
    .getService<VariablesService>('opensilex.VariablesService')
    .getCharacteristicsByURIs(characteristics, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<CharacteristicGetDTO[]>>) => http.response.result)
    .catch($opensilex.errorHandler)
}

function searchCharacteristics(name: string, page: number, pageSize: number) {
  return service.searchCharacteristics(name, ['name=asc'], page, pageSize)
}

const select = (value: any) => emit('select', value)
const deselect = (value: any) => emit('deselect', value)
const onEnter = () => emit('handlingEnterKey')
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  component: 
    characteristic: 
      form:
        selector:
          placeholder : Select one characteristic
          placeholder-multiple : Select one or more characteristics
          filter-search-no-result : No characteristics found
fr:
  component: 
    characteristic: 
      form: 
        selector:
          placeholder : Sélectionner une caractéristique
          placeholder-multiple : Sélectionner une ou plusieurs caractéristiques
          filter-search-no-result : Aucune caractéristique trouvée
</i18n>