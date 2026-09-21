<template>
  <InfiniteScrollDropdown
    ref="dropdown"
    v-model:selected="modelSelected"
    :fetchPage="searchEntities"
    :itemLoadingMethod="loadEntities"
    :conversionMethod="entityToSelectOption"
    :resultLimit="PAGE_SIZE"
    :path="path"
    :label="label"
    :helpMessage="helpMessage"
    :multiple="multiple"
    :required="required"
    :disabled="disabled"
    :actionHandler="actionHandler"
    :placeholder="resolvedPlaceholder"
    :noResultsText="t('component.entity.form.selector.filter-search-no-result')"
    @selectionChange="(option) => emit('selectionChange', option)"
    @handlingEnterKey="emit('handlingEnterKey')"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type { EntityGetDTO, EntityDetailsDTO } from 'opensilex-core/index'
import type OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type HttpResponse from 'opensilex-security/HttpResponse'
import InfiniteScrollDropdown from '@/components/common/forms/InfiniteScrollDropdown.vue'

const props = defineProps<{
  /** Path of the field in the parent NForm model, used for validation */
  path?: string
  selected: string | string[] | undefined
  label?: string
  multiple?: boolean
  helpMessage?: string
  /** Called by the "+" button, to create a new entity */
  actionHandler?: Function
  required?: boolean
  disabled?: boolean
  sharedResourceInstance?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: any): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'handlingEnterKey'): void
}>()

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('opensilex')!

const PAGE_SIZE = 10

// v-model proxy
const modelSelected = computed({
  get: () => props.selected,
  set: (v) => emit('update:selected', v)
})

// InfiniteScrollDropdown is a generic component, so it has no constructor type for InstanceType to
// read. Only `refresh` is needed here, so the exposed shape is declared directly.
const dropdown = ref<{ refresh: () => Promise<void> } | null>(null)

// Results depend on the shared resource instance, so the search is re-run whenever it changes.
watch(() => props.sharedResourceInstance, () => {
  dropdown.value?.refresh()
})

const resolvedPlaceholder = computed(() => {
  if (props.placeholder) {
    return props.placeholder
  }
  return t(props.multiple ? 'component.entity.form.selector.placeholder-multiple' : 'component.entity.form.selector.placeholder')
})

/** Loads the already selected elements (update form), so that their name can be displayed. */
const loadEntities = (uris: string[]): Promise<EntityDetailsDTO[]> => {
  return $opensilex.getService<VariablesService>('opensilex.VariablesService')
    .getEntitiesByURIs(uris, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<EntityDetailsDTO[]>>) => http.response.result)
    .catch($opensilex.errorHandler)
}

/** Loads one page of results. `page` is zero-based. */
const searchEntities = (name: string, page: number, pageSize: number): Promise<HttpResponse<OpenSilexResponse<EntityGetDTO[]>>> => {
  return $opensilex.getService<VariablesService>('opensilex.VariablesService')
    .searchEntities(name, ['name=asc'], page, pageSize, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<EntityGetDTO[]>>) => http)
}

const entityToSelectOption = (dto: EntityGetDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
})
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
