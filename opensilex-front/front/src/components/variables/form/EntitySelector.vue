<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="entityURI"
    :multiple="multiple"
    :searchMethod="searchEntities"
    :itemLoadingMethod="loadEntities"
    :placeholder="placeholder"
    :conversionMethod="conversionMethod"
    :actionHandler="actionHandler"
    :required="required"
    :helpMessage="helpMessage"
    noResultsText="component.entity.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { ref, computed, inject, watch } from 'vue'
import FormSelector from '../../common/forms/FormSelector.vue'
import type { EntityGetDTO } from 'opensilex-core/index'
import type OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type HttpResponse from 'opensilex-security/HttpResponse'

// Props
const props = defineProps<{
  selected: string | string[]
  label?: string
  multiple?: boolean
  required?: boolean
  actionHandler?: Function
  helpMessage?: string
  conversionMethod?: Function
  sharedResourceInstance?: string
}>()

// Emits
const emit = defineEmits<{
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')
const entityURI = ref(props.selected)

const formSelector = ref<InstanceType<typeof FormSelector>>()

const pageSize = 10

watch(() => props.sharedResourceInstance, () => {
  formSelector.value?.refresh()
})

const placeholder = computed(() =>
  props.multiple
    ? 'component.entity.form.selector.placeholder-multiple'
    : 'component.entity.form.selector.placeholder'
)

const loadEntities = (entities: string[]): Promise<EntityGetDTO[]> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .getEntitiesByURIs(entities, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<EntityGetDTO[]>>) => http.response.result)
    .catch($opensilex!.errorHandler)
}

const searchEntities = (name: string, page: number, pageSize: number): Promise<HttpResponse<OpenSilexResponse<EntityGetDTO[]>>> => {
  return $opensilex!.getService<VariablesService>('opensilex.VariablesService')
    .searchEntities(name, ['name=asc'], page, pageSize, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<EntityGetDTO[]>>) => http)
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
    entity: 
      form:
        selector:
          placeholder : Select one entity
          placeholder-multiple : Select one or more entities
          filter-search-no-result : No entities found
fr:
  component: 
    entity: 
      form: 
        selector:
          placeholder : Sélectionner une entité
          placeholder-multiple : Sélectionner une ou plusieurs entités
          filter-search-no-result : Aucune entité trouvée
</i18n>
