<template>
  <div>
    <template v-if="isAgroportalReachable">
      <opensilex-AgroportalSearch
        class="v-step-agroportal-search"
        ref="searchComponent"
        label="component.common.name"
        :placeholder="placeholder"
        v-model:selected="selectedOntologiesProxy"
        v-model:isAllOntologies="useAllOntologies"
        @change="onSearchTextChange"
        @inputValueHasChanged="onInputValueChanged"
      />

      <opensilex-AgroportalResults
        class="v-step-agroportal-results"
        ref="searchResults"
        :text="searchText"
        :ontologies="selectedOntologiesProxy"
        :isMappingMode="isMappingMode"
        @import="onImport"
        @importMapping="onImportMapping"
      />
    </template>

    <div v-else class="alert alert-warning" role="alert">
      <span v-html="t('server.errors.agroportal-instance-connection')"></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { AgroportalAPIService } from 'opensilex-core/api/agroportalAPI.service'

// Types simples localement
type AgroportalTermDTO = { id: string; name: string; definitions?: string[] }
type UriSkosRelation = { uri: string; relationDtoKey: string }

const props = withDefaults(defineProps<{
  placeholder?: string
  isMappingMode?: boolean
  ontologies?: string[]
}>(), {
  placeholder: undefined,
  isMappingMode: false,
  ontologies: () => []
})

const emit = defineEmits<{
  (e: 'import', term: AgroportalTermDTO): void
  (e: 'importMapping', rel: UriSkosRelation): void
  (e: 'inputValueHasChanged', text: string): void
  (e: 'update:ontologies', value: string[]): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

// Services
let service: AgroportalAPIService

// State
const searchText = ref<string>('')
const selectedOntologies = ref<string[]>([...props.ontologies]) // init à partir de la prop
const useAllOntologies = ref<boolean>(false)
const isAgroportalReachable = ref<boolean>(false)

// Refs enfants
const searchComponent = ref<any | null>(null)
const searchResults = ref<any | null>(null)

// Pour le composant Search (et re-emit vers parent si besoin)
const selectedOntologiesProxy = computed<string[]>({
  get: () => selectedOntologies.value,
  set: (v) => {
    selectedOntologies.value = Array.isArray(v) ? v : []
    emit('update:ontologies', selectedOntologies.value)
  }
})

// Keep in sync si le parent change la liste des ontologies
watch(() => props.ontologies, (nv) => {
  selectedOntologies.value = [...(nv ?? [])]
}, { immediate: false })

// Lifecycle
onMounted(async () => {
  service = opensilex.getService('opensilex.AgroportalAPIService')
  try {
    const http: any = await service.pingAgroportal()
    isAgroportalReachable.value = !!http?.response?.result
  } catch {
    isAgroportalReachable.value = false
  }
})

// Event handlers
function onSearchTextChange(text: string) {
  searchText.value = text
  searchResults.value?.search?.(text, useAllOntologies.value, selectedOntologies.value)
}

function onInputValueChanged(text: string) {
  searchText.value = text
  emit('inputValueHasChanged', text)
}

function onImport(term: AgroportalTermDTO) {
    console.log("event import reçu dans AgroportalResults > AgroportalTermSelector")
  emit('import', term)
}

function onImportMapping(rel: UriSkosRelation) {
  emit('importMapping', rel)
}

function setSearchText(text: string) {
  searchComponent.value?.setSearchText?.(text)
}
function selectFirstItem() {
  searchResults.value?.selectItem?.(0)
}
function selectAndImportFirstItem() {
  searchResults.value?.selectAndImportFirstItem?.()
}
function selectAndMapFirstItem() {
  searchResults.value?.selectAndMapFirstItem?.()
}

defineExpose({
  setSearchText,
  selectFirstItem,
  selectAndImportFirstItem,
  selectAndMapFirstItem
})
</script>

<style scoped>
</style>
