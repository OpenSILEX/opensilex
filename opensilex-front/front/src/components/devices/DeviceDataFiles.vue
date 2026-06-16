<template>
  <n-layout has-sider class="datafiles-layout">
    <!-- Bouton loupe -->
    <n-space class="mb-2 me-1" align="top">
      <n-button
        quaternary
        circle
        @click="filtersCollapsed = !filtersCollapsed"
        :title="t('searchfilter.label')"
        :class="{ greenThemeColor: filtersCollapsed }"
        class="globalFiltersSearchButton"
      >
        <i class="bi bi-search filtersGlobalSearchIcon"></i>

        <div v-show="filtersCollapsed && activeFiltersCount > 0" class="filters-count-badge">
          ( {{ activeFiltersCount }} )
        </div>
      </n-button>
    </n-space>

    <!-- Sidebar / Filtres -->
    <n-layout-sider
      v-model:collapsed="filtersCollapsed"
      :collapsed-width="0"
      :width="360"
      collapse-mode="width"
      show-trigger
      bordered
      class="datafiles-sider"
    >
      <n-space class="p-3" vertical>
        <n-form label-placement="top" size="small" @submit.prevent.stop="refresh">
          <!-- Type -->
          <n-form-item class="compact-form-item">
            <opensilex-TypeForm
              v-model:type="filter.rdf_type"
              :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
              :ignoreRoot="false"
              :placeholder="t('DeviceDataFiles.filter.rdfType-placeholder')"
              class="searchFilter"
            />
          </n-form-item>

          <!-- Experiments -->
          <n-form-item class="compact-form-item">
            <opensilex-ExperimentSelector
              v-model:experiments="filter.experiments"
              :label="t('DeviceDataFiles.filter.experiments')"
              :multiple="true"
              @select="updateSOFilter"
              @clear="updateSOFilter"
              class="searchFilter"
            />
          </n-form-item>

          <!-- Scientific objects -->
          <n-form-item class="compact-form-item">
            <opensilex-ModalFormSelector
              ref="soSelector"
              v-model:selected="filter.scientificObjects"
              v-model:filter="soFilter"
              :label="t('DeviceDataFiles.filter.scientificObjects')"
              :placeholder="t('DeviceDataFiles.filter.scientificObjects-placeholder')"
              modalComponent="opensilex-ScientificObjectModalList"
              :clearable="true"
              :multiple="true"
              :limit="1"
              class="searchFilter"
              @clear="refreshSoSelector"
              @onValidate="refreshProvComponent"
              @onClose="refreshProvComponent"
            />
          </n-form-item>

          <!-- Start Date -->
          <n-form-item class="compact-form-item">
            <opensilex-DateTimeForm
              v-model:value="filter.start_date"
              label="component.common.date-time.begin"
              :max-date="filter.end_date ? filter.end_date : undefined"
              class="searchFilter"
            />
          </n-form-item>

          <!-- End Date -->
          <n-form-item class="compact-form-item">
            <opensilex-DateTimeForm
              v-model:value="filter.end_date"
              label="component.common.date-time.end"
              :min-date="filter.start_date ? filter.start_date : undefined"
              class="searchFilter"
            />
          </n-form-item>

          <!-- Provenance -->
          <n-form-item class="compact-form-item">
            <opensilex-DatafileProvenanceSelector
              ref="provSelector"
              v-model:provenances="filter.provenance"
              :devices="[uri]"
              :targets="filter.scientificObjects"
              :experiments="filter.experiments"
              :multiple="false"
              :viewHandler="showProvenanceDetails"
              :viewHandlerDetailsVisible="visibleDetails"
              :key="refreshKey"
              :label="t('DeviceDataFiles.filter.provenance')"
              class="searchFilter"
              @select="loadProvenance"
            />
          </n-form-item>

          <opensilex-ProvenanceDetails 
            v-if="selectedProvenance && visibleDetails"
            :provenance="selectedProvenance" 
          />

          <n-space justify="end" class="mt-2">
            <opensilex-Button
              class="resetButton"
              :label="t('component.common.search.clear-button')"
              icon="bi-x-lg"
              @click="clear"
            />
            <opensilex-Button
              class="greenThemeColor"
              :label="t('component.common.search.search-button')"
              icon="bi-search"
              @click="refresh"
            />
          </n-space>
        </n-form>
      </n-space>
    </n-layout-sider>

    <!-- Contenu Liste -->
    <n-layout-content class="datafiles-content">
      <opensilex-DataFilesList
        ref="datafilesList"
        :filter="filter"
        :device="uri"
        :hideTarget="true"
        class="datafilesList"
      />
    </n-layout-content>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import {
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NSpace,
  NButton
} from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { ProvenanceGetDTO, ResourceTreeDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import Oeso from '../../ontologies/Oeso'

const { t } = useI18n()
const route = useRoute()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const uri = decodeURIComponent(route.params.uri as string)

const datafilesList = ref<any>(null)
const provSelector = ref<any>(null)
const soSelector = ref<any>(null)

const filtersCollapsed = ref(true)
const refreshKey = ref(0)
const visibleDetails = ref(false)
const selectedProvenance = ref<ProvenanceGetDTO | null>(null)
const filterProvenanceLabel = ref<string | null>(null)

const images_rdf_types = ref<string[]>([])
const rdf_types = ref<Record<string, string>>({})

const filter = ref({
  start_date: null as string | null,
  end_date: null as string | null,
  rdf_type: null as string | null,
  provenance: null as any,
  experiments: [] as any[],
  scientificObjects: [] as any[],
  devices: [uri] as string[]
})

const soFilter = ref({
  name: undefined as string | undefined,
  experiment: undefined as string | undefined,
  germplasm: undefined as string | undefined,
  factorLevels: [] as any[],
  types: [] as any[],
  existenceDate: undefined as string | undefined,
  creationDate: undefined as string | undefined
})

const activeFiltersCount = computed(() => {
  return [
    filter.value.rdf_type,
    filter.value.provenance,
    filter.value.start_date,
    filter.value.end_date,
    ...(filter.value.experiments || []),
    ...(filter.value.scientificObjects || [])
  ].filter(v => {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && String(v).trim() !== ''
  }).length
})

function resetFilters() {
  filter.value = {
    start_date: null,
    end_date: null,
    rdf_type: null,
    provenance: null,
    experiments: [],
    scientificObjects: [],
    devices: [uri]
  }
}

function refreshProvComponent() {
  refreshKey.value += 1
}

function refreshSoSelector() {
  refreshProvComponent()
  soSelector.value?.refreshModalSearch?.()
}

function updateSOFilter() {
  refreshProvComponent()
  soFilter.value.experiment = filter.value.experiments?.[0]
  soSelector.value?.refreshModalSearch?.()
}

function showProvenanceDetails() {
  if (selectedProvenance.value != null) {
    visibleDetails.value = !visibleDetails.value
  }
}

function clear() {
  selectedProvenance.value = null
  filterProvenanceLabel.value = null
  visibleDetails.value = false
  resetFilters()
  nextTick(() => {
    datafilesList.value?.refresh?.()
  })
}

function refresh() {
  nextTick(() => {
    datafilesList.value?.refresh?.()
  })
  filtersCollapsed.value = true
}

async function getProvenance(uri: string) {
  if (!uri) return null

  const http = await $opensilex
    .getService('opensilex.DataService')
    .getProvenance(uri) as HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>

  return http.response.result
}

async function loadProvenance(selectedValue: any) {
  if (selectedValue?.id) {
    selectedProvenance.value = await getProvenance(selectedValue.id)
  }
}

function loadTypes() {
  $opensilex.getService('opensilex.OntologyService')
    .getSubClassesOf(Oeso.DATAFILE_TYPE_URI, false)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      images_rdf_types.value = []
      rdf_types.value = {}

      const parentType = http.response.result[0]
      if (!parentType) return

      rdf_types.value[parentType.uri] = parentType.name

      for (const child of parentType.children || []) {
        rdf_types.value[child.uri] = child.name

        if (Oeso.checkURIs(child.uri, Oeso.IMAGE_TYPE_URI)) {
          images_rdf_types.value.push(child.uri)
          for (const imageChild of child.children || []) {
            rdf_types.value[imageChild.uri] = imageChild.name
            images_rdf_types.value.push(imageChild.uri)
          }
        } else {
          for (const subChild of child.children || []) {
            rdf_types.value[subChild.uri] = subChild.name
          }
        }
      }
    })
    .catch($opensilex.errorHandler)
}

let langUnwatcher: (() => void) | undefined

onMounted(() => {
  loadTypes()
  resetFilters()

  const query = route.query
  for (const key of Object.keys(filter.value)) {
    const value = query[key]
    if (value !== undefined) {
      if (Array.isArray(filter.value[key as keyof typeof filter.value])) {
        filter.value[key as keyof typeof filter.value] = Array.isArray(value)
          ? value.map(v => decodeURIComponent(String(v)))
          : [decodeURIComponent(String(value))] as never
      } else {
        filter.value[key as keyof typeof filter.value] = decodeURIComponent(String(value)) as never
      }
    }
  }

  filter.value.devices = [uri]

  langUnwatcher = store.watch(
    () => store.getters.language,
    () => {
      loadTypes()
      refresh()
    }
  )
})

onBeforeUnmount(() => {
  langUnwatcher?.()
})

defineExpose({
  refresh,
  clear,
  resetFilters,
  refreshSoSelector,
  updateSOFilter
})
</script>

<style scoped lang="scss">
.datafiles-layout {
  background: transparent;
}

.datafiles-content {
  padding-left: 12px;
}

.globalFiltersSearchButton {
  width: 40px;
  height: 55px;
}

.globalFiltersSearchButton span {
  display: block !important;
}

.globalFiltersSearchButton div {
  margin-top: 5px;
}

.filtersGlobalSearchIcon {
  font-size: 1.2em;
}

.card-body {
  margin-bottom: -15px;
}

/* neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs */
:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}
</style>

<i18n>
en:
     DeviceDataFiles:
        filter:
            experiments: Experiment(s)
            scientificObjects: Scientific object(s)
            scientificObjects-placeholder: Select scientific objects
            provenance: Provenance
            rdfType-placeholder: Select the datafile type

fr:
    DeviceDataFiles:
        filter:
            experiments: Expérimentation(s)
            variables: Variable(s)
            scientificObjects: Objet(s) scientifique(s)
            scientificObjects-placeholder: Sélectionner des objets scientifiques
            provenance: Provenance
            rdfType-placeholder: Sélectionner le type de fichier
            
</i18n>