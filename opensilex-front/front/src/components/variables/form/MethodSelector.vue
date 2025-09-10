<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="methodURI"
    :multiple="multiple"
    :searchMethod="searchMethods"
    :itemLoadingMethod="loadMethods"
    :conversionMethod="conversionMethod"
    :placeholder="placeholder"
    :actionHandler="actionHandler"
    :required="required"
    :helpMessage="helpMessage"
    noResultsText="component.method.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { ref, computed, inject, watch } from 'vue'
import FormSelector from '../../common/forms/FormSelector.vue'
import type { MethodGetDTO } from 'opensilex-core/index'
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
  required?: boolean
  actionHandler?: Function
  conversionMethod?: Function
  sharedResourceInstance?: string
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: any): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')

// const methodURI = ref(props.selected)
const methodURI = computed({
  get: () => props.selected,
  set: (v) => emit('update:selected', v)
})
const formSelector = ref<InstanceType<typeof FormSelector>>()

const pageSize = ref(10)

watch(() => props.sharedResourceInstance, () => {
  formSelector.value?.refresh()
})

const placeholder = computed(() =>
  props.multiple
    ? 'component.method.form.selector.placeholder-multiple'
    : 'component.method.form.selector.placeholder'
)

const loadMethods = (methods: string[]): Promise<MethodGetDTO[]> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .getMethodsByURIs(methods, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<MethodGetDTO[]>>) => http.response.result)
    .catch($opensilex!.errorHandler)
}

const searchMethods = (name: string, page: number, pageSize: number): Promise<HttpResponse<OpenSilexResponse<MethodGetDTO[]>>> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .searchMethods(name, ['name=asc'], page, pageSize, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<MethodGetDTO[]>>) => http)
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
    method: 
      form:
        selector:
          placeholder : Select one method
          placeholder-multiple : Select one or more methods
          filter-search-no-result : No methods found
fr:
  component: 
    method: 
      form: 
        selector:
          placeholder : Sélectionner une méthode
          placeholder-multiple : Sélectionner une ou plusieurs méthodes
          filter-search-no-result : Aucune méthode trouvée
</i18n>