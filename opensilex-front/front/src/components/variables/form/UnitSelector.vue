<template>
  <InfiteScrollDropdown
    ref="dropdown"
    v-model:selected="modelSelected"
    :fetchPage="searchUnits"
    :itemLoadingMethod="loadUnits"
    :conversionMethod="unitToSelectOption"
    :path="path"
    :label="label"
    :helpMessage="helpMessage"
    :multiple="multiple"
    :required="required"
    :disabled="disabled"
    :actionHandler="actionHandler"
    :placeholder="resolvedPlaceholder"
    @selectionChange="(option) => emit('selectionChange', option)"
    @handlingEnterKey="emit('handlingEnterKey')"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type { UnitGetDTO } from 'opensilex-core/index'
import type OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type HttpResponse from 'opensilex-security/HttpResponse'
import InfiteScrollDropdown from '@/components/common/forms/InfiteScrollDropdown.vue'

const props = defineProps<{
  /** Path of the field in the parent NForm model, used for validation */
  path?: string
  selected: string | string[] | undefined
  label?: string
  multiple?: boolean
  helpMessage?: string
  /** Called by the "+" button, to create a new unit */
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

// v-model proxy
const modelSelected = computed({
  get: () => props.selected,
  set: (v) => emit('update:selected', v)
})

// InfiteScrollDropdown is a generic component, so it has no constructor type for InstanceType to
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
  return t(props.multiple ? 'component.unit.form.selector.placeholder-multiple' : 'component.unit.form.selector.placeholder')
})

/** Loads the already selected elements (update form), so that their name can be displayed. */
const loadUnits = (uris: string[]): Promise<UnitGetDTO[]> => {
  return $opensilex.getService<VariablesService>('opensilex.VariablesService')
    .getUnitsByURIs(uris, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<UnitGetDTO[]>>) => http.response.result)
    .catch($opensilex.errorHandler)
}

/** Loads one page of results. `page` is zero-based. */
const searchUnits = (name: string, page: number, pageSize: number): Promise<HttpResponse<OpenSilexResponse<UnitGetDTO[]>>> => {
  return $opensilex.getService<VariablesService>('opensilex.VariablesService')
    .searchUnits(name, ['name=asc'], page, pageSize, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<UnitGetDTO[]>>) => http)
}

const unitToSelectOption = (dto: UnitGetDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
})
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
