<template>
  <div class="container-fluid">
    <opensilex-PageActions>
      <opensilex-CreateButton
        @click="showCreateDataFileForm"
        :label="t('DataFilesView.add')"
        class="createButton"
      />
    </opensilex-PageActions>

    <opensilex-PageContent class="pagecontent">
      <template #default>
        <n-layout has-sider class="datafiles-layout">
          <!-- Bouton loupe -->
          <n-space class="mb-2 me-1" align="top">
            <n-button
              quaternary
              circle
              @click="filtersCollapsed = !filtersCollapsed"
              :title="searchFiltersPannel"
              :class="{ greenThemeColor: filtersCollapsed }"
              class="globalFiltersSearchButton"
            >
                <i class="bi bi-search filtersGlobalSearchIcon"></i>

              <div
                v-show="filtersCollapsed && activeFiltersCount > 0"
                class="filters-count-badge"
              >
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
            class="project-sider"
          >
            <n-space class="p-3 searchFilterField" vertical>
              <n-form
                label-placement="top"
                size="small"
                @submit.prevent.stop="refresh"
              >
                <!-- FileName -->
                <n-form-item :label="t('DataFilesView.fileName')" class="compact-form-item">
                  <n-input
                    v-model:value="filter.name"
                    clearable
                    :placeholder="t('DataFilesView.fileName-placeholder')"
                    class="searchFilter"
                    @keydown.enter.prevent.stop="refresh"
                  />
                </n-form-item>

                <!-- Type -->
                <n-form-item class="compact-form-item">
                  <opensilex-TypeForm
                    v-if="filter.imagesView"
                    v-model:type="filter.rdf_type"
                    :baseType="Oeso.IMAGE_TYPE_URI"
                    :ignoreRoot="false"
                    :placeholder="t('DataFilesView.filter.rdfType-placeholder')"
                    class="searchFilter"
                    key="imageTypeForm"
                    @handlingEnterKey="refresh"
                  />

                  <opensilex-TypeForm
                    v-else
                    v-model:type="filter.rdf_type"
                    :baseType="Oeso.DATAFILE_TYPE_URI"
                    :ignoreRoot="false"
                    :placeholder="t('DataFilesView.filter.rdfType-placeholder')"
                    class="searchFilter"
                    key="datafileTypeForm"
                    @handlingEnterKey="refresh"
                  />
                </n-form-item>

                <!-- Experiments -->
                <n-form-item class="compact-form-item">
                  <opensilex-ExperimentSelector
                    :label="t('component.experiment.view.experiment-experiments')"
                    v-model:experiments="filter.experiments"
                    :multiple="true"
                    @select="updateSOFilter"
                    @clear="updateSOFilter"
                    class="searchFilter"
                    :key="resetExperimentSelectorKey"
                  />
                </n-form-item>

                <!-- Scientific objects -->
                <n-form-item class="compact-form-item">
                  <opensilex-ModalFormSelector
                    ref="soSelector"
                    :label="t('DataFilesView.filter.scientificObjects')"
                    :placeholder="t('DataFilesView.filter.scientificObjects-placeholder')"
                    v-model:selected="filter.scientificObjects"
                    modalComponent="opensilex-ScientificObjectModalList"
                    v-model:filter="soFilter"
                    :clearable="true"
                    :multiple="true"
                    @onValidate="refreshProvComponent"
                    @onClose="refreshProvComponent"
                    @clear="refreshSoSelector"
                    :limit="1"
                    class="searchFilter scientificObjectsSelector"
                  />
                </n-form-item>

                <!-- Start Date -->
                <n-form-item class="compact-form-item">
                  <opensilex-DateTimeForm
                    v-model:value="filter.start_date"
                    label="component.common.date-time.begin"
                    name="startDate"
                    :max-date="filter.end_date ? filter.end_date : undefined"
                    class="searchFilter"
                  />
                </n-form-item>

                <!-- End Date -->
                <n-form-item class="compact-form-item">
                  <opensilex-DateTimeForm
                    v-model:value="filter.end_date"
                    label="component.common.date-time.end"
                    name="endDate"
                    :min-date="filter.start_date ? filter.start_date : undefined"
                    :minDate="filter.start_date"
                    :maxDate="filter.end_date"
                    class="searchFilter"
                  />
                </n-form-item>

                <!-- Provenance -->
                <n-form-item class="compact-form-item">
                  <opensilex-DatafileProvenanceSelector
                    ref="provSelector"
                    v-model:provenances="filter.provenance"
                    :label="t('DataFilesView.filter.provenance')"
                    @select="loadProvenance"
                    @clear="clearProvenance"
                    :targets="filter.scientificObjects"
                    :experiments="filter.experiments"
                    :multiple="false"
                    :viewHandler="showProvenanceDetails"
                    :viewHandlerDetailsVisible="visibleDetails"
                    :showURI="false"
                    :key="refreshKey"
                    class="searchFilter"
                    @handlingEnterKey="refresh"
                  />
                </n-form-item>

                <opensilex-ProvenanceDetails
                  v-if="selectedProvenance && visibleDetails"
                  :provenance="selectedProvenance"
                  class="provenanceDetails"
                />

                <!-- Images -->
                <n-form-item class="compact-form-item">
                  <n-space align="center">
                    <n-switch v-model:value="filter.imagesView" />
                    <strong>{{ t('DataFilesView.imagesView') }}</strong>
                  </n-space>
                </n-form-item>

                <n-space justify="end" class="mt-2">
                  <opensilex-Button
                    class="resetButton"
                    :label="t('component.common.search.clear-button')"
                    icon="bi-x-lg"
                    @click="reset"
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
          <n-layout-content class="project-content">
            <opensilex-DataFilesImagesList
              key="images-view"
              v-if="filter.imagesView"
              ref="datafilesImagesList"
              :filter="filter"
              class="imagesList"
            />

            <opensilex-DataFilesList
              v-else
              key="list-view"
              ref="datafilesList"
              :filter="filter"
              class="datafilesList"
            />

            <opensilex-ModalForm
              ref="datafileForm"
              component="opensilex-DataFileForm"
              editTitle="update"
              :createTitle="t('DataFilesView.add')"
              icon="bi#bi-file-earmark-text"
              modalSize="lg"
              @onCreate="refresh"
            />
          </n-layout-content>
        </n-layout>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import {
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NInput,
  NButton,
  NSpace,
  NSwitch,
  NCollapseTransition
} from 'naive-ui'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { DataService, ProvenanceGetDTO } from 'opensilex-core/index'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/index'

const { t } = useI18n()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const Oeso = $opensilex.Oeso

const filtersCollapsed = ref(true)

const refreshKey = ref(0)
const visibleDetails = ref(false)
const selectedProvenance = ref<any>(null)
const resetExperimentSelectorKey = ref(0)

const datafilesList = ref<any>(null)
const datafilesImagesList = ref<any>(null)
const provSelector = ref<any>(null)
const soSelector = ref<any>(null)
const datafileForm = ref<any>(null)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

type SelectableItem = {
  id: string
  label: string
  isDisabled?: boolean
}

const scientificObjectsWithLabels = ref<SelectableItem[] | null>(null)

const filter = reactive({
  name: undefined as string | undefined,
  start_date: undefined as string | undefined,
  end_date: undefined as string | undefined,
  rdf_type: undefined as string | undefined,
  provenance: undefined as string | undefined,
  experiments: [] as string[],
  scientificObjects: [] as string[],
  imagesView: false
})

const soFilter = reactive({
  name: undefined as string | undefined,
  experiment: undefined as string | undefined,
  germplasm: undefined as string | undefined,
  factorLevels: [] as string[],
  types: [] as string[],
  existenceDate: undefined as string | undefined,
  creationDate: undefined as string | undefined
})

const searchFiltersPannel = computed(() => t('searchfilter.label'))

const activeFiltersCount = computed(() => {
  return [
    filter.name,
    filter.start_date,
    filter.end_date,
    filter.rdf_type,
    filter.provenance,
    filter.imagesView ? 'imagesView' : '',
    ...filter.experiments,
    ...filter.scientificObjects
  ].filter(v => v !== undefined && String(v).trim() !== '').length
})

const getSelectedProv = computed(() => selectedProvenance.value)


function showCreateDataFileForm() {
  datafileForm.value?.showCreateForm?.()
}

function resetFilter() {
  filter.name = undefined
  filter.start_date = undefined
  filter.end_date = undefined
  filter.rdf_type = undefined
  filter.provenance = undefined
  filter.experiments = []
  filter.scientificObjects = []
  filter.imagesView = false
}

function normalizeDates() {
  if (filter.start_date === '') {
    filter.start_date = undefined
  }

  if (filter.end_date === '') {
    filter.end_date = undefined
  }
}

function refreshSoSelector() {
  refreshProvComponent()
  soSelector.value?.refreshModalSearch?.()
}

function refreshProvComponent() {
  refreshKey.value += 1
}

function updateSOFilter() {
  soFilter.experiment = filter.experiments[0]
  refreshProvComponent()
  soSelector.value?.refreshModalSearch?.()
}

async function refresh() {
  normalizeDates()

  await nextTick()

  if (filter.imagesView) {
    datafilesImagesList.value?.refresh?.()
  } else {
    datafilesList.value?.refresh?.()
  }

  filtersCollapsed.value = true
}

function reset() {
  resetFilter()
  refresh()
  normalizeDates()
  resetExperimentSelectorKey.value += 1
}

function showProvenanceDetails() {
  if (selectedProvenance.value !== null) {
    visibleDetails.value = !visibleDetails.value
  }
}

function getProvenance(uri: string) {
  if (uri !== undefined && uri !== null) {
    const dataService = $opensilex.getService<DataService>('opensilex.DataService')

    return dataService.getProvenance(uri)
      .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
        return http.response.result
      })
  }

  return Promise.resolve(undefined)
}

function loadProvenance(selectedValue: any) {
  if (selectedValue !== undefined && selectedValue !== null) {
    getProvenance(selectedValue.id).then((prov) => {
      selectedProvenance.value = prov
    })
  }
}

defineExpose({
  refresh,
  reset
})
</script>

<style scoped lang="scss">
.imagesList {
  min-width: 70%;
  width: 100%;
}

.datafilesList {
  width: 100%;
}

.createButton {
  margin-bottom: 10px;
  margin-top: -15px;
}

.project-content {
  padding-left: 12px;
}

.datafiles-layout {
  background: transparent;
}

.filtersGlobalSearchIcon {
  font-size: 1.2em;
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

/* neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs */
:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}

:deep(.scientificObjectsSelector .chip-area) {
  max-width: 100%;
  overflow: hidden;
}

:deep(.scientificObjectsSelector .chip-area > *) {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<i18n>
en:
  DataFilesView:
    add: Add datafiles
    description: View datafiles
    details: view datafile metadata
    fileName: File Name
    fileName-placeholder: Enter file name
    imagesView: View
    filter:
      provenance: Provenance
      rdfType-placeholder: Select the datafile type
      scientificObjects: Scientific object(s)
      scientificObjects-placeholder: Select scientific objects


fr:
  DataFilesView:
    add: Ajouter un fichier de données
    description: Voir les fichiers de données
    details: Voir les métadonnées du fichier
    fileName: Nom de fichier
    fileName-placeholder: Saisir un nom de fichier
    imagesView: Visualisation
    filter:
      provenance: Provenance
      rdfType-placeholder: Sélectionner le type de fichier
      scientificObjects: Objet(s) scientifique(s)
      scientificObjects-placeholder: Sélectionner des objets scientifiques
</i18n>