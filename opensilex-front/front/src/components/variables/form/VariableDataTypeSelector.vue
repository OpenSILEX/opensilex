<template>
  <opensilex-FormSelector
    :label="label"
    v-model:selected="dataTypeURI"
    :options="datatypesNodes"
    :itemLoadingMethod="loadDataType"
    :required="required"
    :disabled="disabled"
    :helpMessage="helpMessage"
    :placeholder="placeholder"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { ref, inject, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { VariableDatatypeDTO } from 'opensilex-core/index'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

// Props
const props = defineProps<{
  selected: string | string[]
  label?: string
  placeholder?: string
  required?: boolean
  helpMessage?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')
const { t, locale } = useI18n()

const dataTypeURI = ref(props.selected)
const datatypes = ref<VariableDatatypeDTO[]>([])
const datatypesNodes = ref<{ id: string; label: string }[]>([])

const loadDatatypes = async () => {
  if (datatypes.value.length === 0) {
    const http = await $opensilex!.getService<VariablesService>('opensilex.VariablesService')
      .getDatatypes()
    datatypes.value = http.response.result
  }
  updateDatatypeNodes()
}

// Update formatted nodes for FormSelector with translation
const updateDatatypeNodes = () => {
  datatypesNodes.value = datatypes.value.map(dto => {
    const translated = t(dto.name)
    return {
      id: dto.uri,
      label: translated.charAt(0).toUpperCase() + translated.slice(1)
    }
  })
}

// itemLoadingMethod
const loadDataType = (dataTypeUri: string) => {
  if (!dataTypeUri) return undefined
  return [datatypesNodes.value.find(node => node.id === dataTypeUri)]
}

// Watch language change to update labels
watch(() => locale.value, () => {
  updateDatatypeNodes()
})

// Load datatypes on mount
onMounted(() => {
  loadDatatypes()
})

const onEnter = () => {
  emit('handlingEnterKey')
}
</script>
