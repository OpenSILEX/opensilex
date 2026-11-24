<template>
  <FormSelector
    v-model:selected="attribute"
    label="component.common.attributes-filter.attribute"
    helpMessage="component.common.attributes-filter.attribute-help"
    :placeholder="t('component.common.attributes-filter.attribute-placeholder')"
    :optionsLoadingMethod="loadAttributeOptions"
    @enterKey="emit('handlingEnterKey')"
  />

  <!-- Disabled until an attribute is selected. Keyed on the attribute, so that the proposed values are reloaded when it changes -->
  <FormSelector
    :key="attribute"
    :disabled="!attribute"
    v-model:selected="value"
    label="component.common.attributes-filter.value"
    :placeholder="t('component.common.attributes-filter.value-placeholder')"
    :searchMethod="searchValueOptions"
    @enterKey="emit('handlingEnterKey')"
  />
</template>

<script setup lang="ts">
import { inject, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import HttpResponse, { type OpenSilexResponse } from '@/lib/HttpResponse'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import FormSelector from '@/components/common/forms/FormSelector.vue'
import type { SelectorNamedResource } from '@/components/common/forms/CustomTreeselect.vue'

//#region Public
const emit = defineEmits<{
  handlingEnterKey: []
}>()

const props = defineProps<{
  /** Returns every attribute name, e.g. `GermplasmService.getGermplasmAttributes` */
  loadAttributes: () => Promise<HttpResponse<OpenSilexResponse<string[]>>>
  /** Returns the values of `attribute` matching the `query` regex, e.g. `GermplasmService.getGermplasmAttributeValues` */
  searchValues: (attribute: string, query: string, page: number, pageSize: number) => Promise<HttpResponse<OpenSilexResponse<string[]>>>
}>()

/** Selected attribute name */
const attribute = defineModel<string>('attribute')
/** Selected attribute value, only selectable once an attribute is selected */
const value = defineModel<string>('value')
//#endregion

//#region Private

//#region Plugins & services
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
//#endregion

//#region Hooks & watchers
/** A value only makes sense for the attribute it was picked for */
watch(attribute, () => {
  value.value = undefined
})
//#endregion

//#region Methods
/** FormSelector works with named resources: an attribute name or value is both the id and the label */
function toNamedResource(name: string): SelectorNamedResource {
  return { uri: name, name }
}

async function loadAttributeOptions(): Promise<SelectorNamedResource[]> {
  try {
    const http = await props.loadAttributes()
    return http.response.result.map(toNamedResource)
  } catch (error) {
    $opensilex.errorHandler(error)
    return []
  }
}

async function searchValueOptions(query: string, page: number, pageSize: number): Promise<HttpResponse<OpenSilexResponse<SelectorNamedResource[]>>> {
  const http = await props.searchValues(attribute.value, query, page, pageSize)
  return new HttpResponse(
    { ...http.response, result: http.response.result.map(toNamedResource) },
    http.status,
    http.headers
  )
}
//#endregion

//#endregion
</script>
