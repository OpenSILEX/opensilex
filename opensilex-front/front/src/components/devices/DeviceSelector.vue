<template>
  <div v-if="renderComponent">
    <InfiteScrollDropdown
      v-model:selected="deviceURIs"
      :fetchPage="search"
      :itemLoadingMethod="load"
      :conversionMethod="deviceToSelectOption"
      :label="label"
      :placeholder="t('DeviceSelector.placeholder')"
    :noResultsText="t('DeviceSelector.no-results-text')"
      :multiple="multiple"
      :required="required"
      :key="lang"
      @selectionChange="(option) => emit('selectionChange', option)"
      @clear="emit('clear')"
      @handlingEnterKey="emit('handlingEnterKey')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { DeviceGetDTO } from 'opensilex-core/index'
import InfiteScrollDropdown from '@/components/common/forms/InfiteScrollDropdown.vue'

const props = withDefaults(defineProps<{
  value?: string | string[] | null
  type?: string
  multiple?: boolean
  required?: boolean
  label?: string
}>(), {
  multiple: false,
  required: false,
  label: 'component.menu.devices'
})

const emit = defineEmits<{
  (e: 'update:value', value: string | string[] | null): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const store = useStore()
const { t } = useI18n()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<any>('opensilex.DevicesService')

const renderComponent = ref(true)

const lang = computed(() => store.getters.language)

// v-model proxy
const deviceURIs = computed({
  get: () => props.value ?? (props.multiple ? [] : null),
  set: (value) => emit('update:value', value)
})

// The device type filters the search, so the selector is remounted to drop the loaded results.
watch(
  () => props.type,
  async () => {
    renderComponent.value = false
    await nextTick()
    renderComponent.value = true
  }
)

/** Loads one page of results. `page` is zero-based. */
async function search(query: string, page: number, pageSize: number) {
  try {
    return await service.searchDevices(
      props.type,      // rdf_type
      true,            // include_subtypes
      query,           // name
      undefined,       // variable
      undefined,       // year
      undefined,       // existence_date
      undefined,       // facility
      undefined,       // brand
      undefined,       // model
      undefined,       // serial_number
      undefined,       // metadata
      undefined,       // Relations
      ['name=asc'],
      page,
      pageSize
    )
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

/** Loads the already selected elements (update form), so that their name can be displayed. */
async function load(uris: string[]) {
  try {
    const http = await service.getDeviceByUris(uris)
    return http?.response?.result
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

const deviceToSelectOption = (dto: DeviceGetDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
})
</script>

<i18n>
en:
  DeviceSelector:
    placeholder: Search and select devices
    no-results-text: No device found
fr:
  DeviceSelector:
    placeholder: Rechercher et sélectionner un ou plusieurs appareils
    no-results-text: Aucun appareil trouvé
</i18n>