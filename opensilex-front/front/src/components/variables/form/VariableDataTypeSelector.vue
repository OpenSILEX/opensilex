<template>
  <opensilex-FormSelector
    :path="path"
    :label="label"
    v-model:selected="dataTypeURI"
    :options="datatypesNodes"
    :itemLoadingMethod="loadDataTypesByUris"
    :conversionMethod="convertDatatype"
    :required="required"
    :disabled="disabled"
    :helpMessage="helpMessage"
    :placeholder="placeholder"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { ref, inject, onMounted, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { VariableDatatypeDTO } from 'opensilex-core'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

/** Props **/
const props = defineProps<{
  /** chemin du champ dans `form` (pour NForm rules) */
  path: string
  /** v-model:selected */
  selected: string | undefined
  label?: string
  placeholder?: string
  required?: boolean
  helpMessage?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: string | undefined): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')!
const { t, locale } = useI18n()

/** v-model proxy */
const dataTypeURI = computed({
  get: () => props.selected,
  set: (v) => emit('update:selected', v)
})


const datatypes = ref<VariableDatatypeDTO[]>([])
const datatypesNodes = ref<{ id: string; label: string }[]>([])

const loadDatatypes = async () => {
  if (!datatypes.value.length) {
    const http: HttpResponse<OpenSilexResponse<VariableDatatypeDTO[]>> =
      await $opensilex.getService<VariablesService>('opensilex.VariablesService').getDatatypes()
    datatypes.value = http.response.result ?? []
  }
  updateDatatypeNodes()
}

/** Conversion DTO -> option { id, label }*/
const convertDatatype = (dto: VariableDatatypeDTO) => {
  if (!dto) return null
  const translated = t(dto.name)
  return {
    id: dto.uri,
    label: translated ? translated.charAt(0).toUpperCase() + translated.slice(1) : dto.name
  }
}

/** Alimente la liste visible depuis les DTO chargés */
const updateDatatypeNodes = () => {
  datatypesNodes.value = datatypes.value.map(convertDatatype).filter(Boolean) as { id: string; label: string }[]
}

/** itemLoadingMethod attendu par CustomTreeselect
 *    - signature: (uris: string[]) => Promise<VariableDatatypeDTO[]>
 *    - retourne les DTO bruts (avec { uri, name })
 *    - l’affichage sera fait via conversionMethod = convertDatatype
 */
const loadDataTypesByUris = async (uris: string[]) => {
  if (!Array.isArray(uris) || !uris.length) return []
  // On s’assure que la source est chargée
  if (!datatypes.value.length) await loadDatatypes()
  return datatypes.value.filter(d => uris.includes(d.uri))
}

/** MàJ des libellés si la langue change */
watch(() => locale.value, () => {
  updateDatatypeNodes()
})

onMounted(() => {
  loadDatatypes()
})

const onEnter = () => emit('handlingEnterKey')
</script>

<style scoped>
</style>
