<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="interestEntityURI"
    :multiple="multiple"
    :searchMethod="searchInterestEntities"
    :itemLoadingMethod="loadInterestEntities"
    :placeholder="placeholder"
    :helpMessage="helpMessage"
    :actionHandler="actionHandler"
    :conversionMethod="conversionMethod"
    noResultsText="component.interestEntity.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @enterKey="onEnter"
    @loadMoreItems="loadMoreItems"
  />
</template>

<script setup lang="ts">
import { ref, computed, inject, watch, nextTick } from 'vue'
import FormSelector from '../../common/forms/FormSelector.vue'
import type { InterestEntityGetDTO } from 'opensilex-core/index'
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

// const interestEntityURI = ref(props.selected)
const interestEntityURI = computed({
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
    ? 'component.interestEntity.form.selector.placeholder-multiple'
    : 'component.interestEntity.form.selector.placeholder'
)

const loadInterestEntities = (interestEntities: string[]): Promise<InterestEntityGetDTO[]> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .getInterestEntitiesByURIs(interestEntities, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<InterestEntityGetDTO[]>>) => http.response.result)
    .catch($opensilex!.errorHandler)
}

const searchInterestEntities = (name: string): Promise<HttpResponse<OpenSilexResponse<InterestEntityGetDTO[]>>> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .searchInterestEntity(name, ['name=asc'], 0, pageSize.value, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<InterestEntityGetDTO[]>>) => http)
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

const loadMoreItems = () => {
  pageSize.value = 0
  formSelector.value?.refresh()
  nextTick(() => {
    formSelector.value?.openTreeselect()
  })
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  component: 
    interestEntity: 
      form:
        selector:
          placeholder : Select one entity of interest
          placeholder-multiple : Select one or more entities of interest
          filter-search-no-result : No entities of interest found
fr:
  component: 
    interestEntity: 
      form: 
        selector:
          placeholder : Sélectionner une entité d'intérêt
          placeholder-multiple : Sélectionner une ou plusieurs entités d'intérêt
          filter-search-no-result : Aucune entité d'intérêt trouvée
</i18n>
