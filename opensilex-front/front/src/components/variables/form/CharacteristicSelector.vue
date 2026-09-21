<template>
  <InfiniteScrollDropdown
    ref="dropdown"
    v-model:selected="modelSelected"
    :fetchPage="searchCharacteristics"
    :itemLoadingMethod="loadCharacteristics"
    :conversionMethod="characteristicToSelectOption"
    :path="path"
    :label="label"
    :helpMessage="helpMessage"
    :multiple="multiple"
    :required="required"
    :disabled="disabled"
    :actionHandler="actionHandler"
    :placeholder="resolvedPlaceholder"
    :noResultsText="t('component.characteristic.form.selector.filter-search-no-result')"
    @selectionChange="(option) => emit('selectionChange', option)"
    @handlingEnterKey="emit('handlingEnterKey')"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type { CharacteristicGetDTO } from 'opensilex-core'
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
  /** Called by the "+" button, to create a new characteristic */
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
  return t(props.multiple ? 'component.characteristic.form.selector.placeholder-multiple' : 'component.characteristic.form.selector.placeholder')
})

/** Loads the already selected elements (update form), so that their name can be displayed. */
const loadCharacteristics = (uris: string[]): Promise<CharacteristicGetDTO[]> => {
  return $opensilex.getService<VariablesService>('opensilex.VariablesService')
    .getCharacteristicsByURIs(uris, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<CharacteristicGetDTO[]>>) => http.response.result)
    .catch($opensilex.errorHandler)
}

/** Loads one page of results. `page` is zero-based. */
const searchCharacteristics = (name: string, page: number, pageSize: number): Promise<HttpResponse<OpenSilexResponse<CharacteristicGetDTO[]>>> => {
  return $opensilex.getService<VariablesService>('opensilex.VariablesService')
    .searchCharacteristics(name, ['name=asc'], page, pageSize)
    .then((http: HttpResponse<OpenSilexResponse<CharacteristicGetDTO[]>>) => http)
}

const characteristicToSelectOption = (dto: CharacteristicGetDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
})
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