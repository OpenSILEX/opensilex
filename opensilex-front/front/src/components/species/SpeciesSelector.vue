<template>
  <opensilex-FormSelector
    :label="label"
    v-model:selected="speciesURI"
    :multiple="multiple"
    :checkable="checkable"
    :required="required"
    :optionsLoadingMethod="loadSpecies"
    :conversionMethod="speciesToSelectNode"
    :placeholder="placeholder"
    :key="refreshKey"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { ref, computed, inject, watch, onMounted, onBeforeUnmount } from 'vue'
import { useStore } from 'vuex'
import type OpenSilexVuePlugin from '../../models/OpenSilexVuePlugin'
import type { SpeciesService } from 'opensilex-core/api/species.service'
import type { ExperimentsService } from 'opensilex-core/api/experiments.service'
import type { SpeciesDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

// Props
const props = defineProps<{
  selected: string | string[]
  label?: string
  placeholder?: string
  required?: boolean
  multiple?: boolean
  checkable?: boolean
  experimentURI?: string
  sharedResourceInstance?: string
}>()

const emit = defineEmits<{
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')
const store = useStore()

const speciesURI = ref(props.selected)
const refreshKey = ref(0)

watch(() => props.sharedResourceInstance, () => {
  refreshKey.value += 1
})

// Watch language change to refresh species list
let unwatchLang: () => void

onMounted(() => {
  unwatchLang = store.watch(
    (state, getters) => getters.language,
    () => {
      loadSpecies() // reload species when language changes
    }
  )
})

onBeforeUnmount(() => {
  if (unwatchLang) {
    unwatchLang()
  }
})

const loadSpecies = (): Promise<SpeciesDTO[]> => {
  if (!props.experimentURI) {
    return $opensilex!.getService<SpeciesService>('opensilex.SpeciesService')
      .getAllSpecies(props.sharedResourceInstance)
      .then((http: HttpResponse<OpenSilexResponse<SpeciesDTO[]>>) => http.response.result)
  } else {
    return $opensilex!.getService<ExperimentsService>('opensilex.ExperimentsService')
      .getAvailableSpecies(props.experimentURI)
      .then((http: HttpResponse<OpenSilexResponse<SpeciesDTO[]>>) => http.response.result)
  }
}

const speciesToSelectNode = (dto: SpeciesDTO) => {
  return {
    id: dto.uri,
    label: dto.name
  }
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

<i18n>
en:
  SpeciesSelector:
    select-multiple-placeholder: "Select species"
    select-multiple: "Species"
fr:
  SpeciesSelector:
    select-multiple-placeholder: "Sélectionner des espèces"
    select-multiple: "Espèces"
</i18n>

<style scoped lang="scss">
</style>
