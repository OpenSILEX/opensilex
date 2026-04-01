<template>
  <div v-if="renderComponent">
    <opensilex-FormSelector
      ref="deviceSelector"
      :label="label"
      :placeholder="t('DeviceSelector.placeholder')"
      :noResultsText="t('DeviceSelector.no-results-text')"
      v-model:selected="deviceURIs"
      :multiple="multiple"
      :required="required"
      :searchMethod="search"
      :itemLoadingMethod="load"
      :conversionMethod="dtoToSelectNode"
      :key="lang"
      :showCount="true"
      @clear="emit('clear')"
      @select="emit('select', $event)"
      @deselect="emit('deselect', $event)"
      @keyup.enter="onEnter"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { DeviceGetDTO } from 'opensilex-core/index'

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
  (e: 'clear'): void
  (e: 'select', value?: any): void
  (e: 'deselect', value?: any): void
  (e: 'handlingEnterKey'): void
}>()

const store = useStore()
const { t } = useI18n()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<any>('opensilex.DevicesService')

const pageSize = 10
const page = 0

const renderComponent = ref(true)
const dtoByUriCache = ref<Map<string, DeviceGetDTO>>(new Map())
const deviceSelector = ref<any>(null)

const lang = computed(() => store.getters.language)

const deviceURIs = computed({
  get: () => props.value ?? (props.multiple ? [] : null),
  set: (value) => emit('update:value', value)
})

watch(
  () => props.type,
  async () => {
    renderComponent.value = false
    await nextTick()
    renderComponent.value = true
  }
)

async function search(query: string, pageArg: number, pageSizeArg: number) {
  try {
    const http = await service.searchDevices(
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
      ['name=asc'],
      pageArg,
      pageSizeArg
    )

    if (http?.response?.result) {
      dtoByUriCache.value.clear()
      for (const dto of http.response.result) {
        dtoByUriCache.value.set(dto.uri, dto)
      }
    }

    return http
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

async function load(devices: string[]) {
  try {
    const http = await service.getDeviceByUris(devices)
    return http?.response?.result
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

function dtoToSelectNode(dto: DeviceGetDTO) {
  if (!dto) {
    return undefined
  }

  return {
    label: dto.name,
    id: dto.uri
  }
}

function onEnter() {
  emit('handlingEnterKey')
}

defineExpose({
  search,
  load,
  dtoToSelectNode,
  deviceSelector,
  pageSize,
  page
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