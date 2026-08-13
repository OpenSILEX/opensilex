<template>
  <div class="container-fluid">

    <PageContent class="pagecontent">
      <template #default>
        <n-layout has-sider class="datafiles-layout">
          <!-- Search button -->
          <n-space class="mb-2 me-1" align="start">
            <n-button
              quaternary
              circle
              @click="searchFiltersVisible = !searchFiltersVisible"
              :title="t('searchfilter.label')"
              :class="{ greenThemeColor: searchFiltersVisible }"
              class="globalFiltersSearchButton"
            >
              <i class="bi bi-search filtersGlobalSearchIcon"></i>

              <div
                v-show="searchFiltersVisible && activeFiltersCount > 0"
                class="filters-count-badge"
              >
                ( {{ activeFiltersCount }} )
              </div>
            </n-button>
          </n-space>

          <!-- Sidebar filters -->
          <n-layout-sider
            v-model:collapsed="searchFiltersVisible"
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
                <!-- Type -->
                <n-form-item class="compact-form-item">

                  <TypeForm
                    v-model:type="filter.rdf_type"
                    :baseType="Oeso.DATAFILE_TYPE_URI"
                    :ignoreRoot="false"
                    :placeholder="t('component.datafile.filters.rdfType-placeholder')"
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
                    class="searchFilter"
                  />
                </n-form-item>

                <!-- Start Date -->
                <n-form-item class="compact-form-item">
                  <DateTimeForm
                    v-model:value="filter.start_date"
                    label="component.common.date-time.begin"
                    name="startDate"
                    :max-date="filter.end_date ? filter.end_date : undefined"
                    class="searchFilter"
                  />
                </n-form-item>

                <!-- End Date -->
                <n-form-item class="compact-form-item">
                  <DateTimeForm
                    v-model:value="filter.end_date"
                    label="component.common.date-time.end"
                    name="endDate"
                    :min-date="filter.start_date ? filter.start_date : undefined"
                    :maxDate="filter.end_date"
                    class="searchFilter"
                  />
                </n-form-item>

                <!-- Provenance -->
                <n-form-item class="compact-form-item">
                  <DatafileProvenanceSelector
                    v-model:provenances="filter.provenance"
                    :label="t('component.datafile.filters.provenance')"
                    @select="loadProvenance"
                    :targets="[uri]"
                    :experiments="filter.experiments"
                    :multiple="false"
                    :viewHandler="showProvenanceDetails"
                    :viewHandlerDetailsVisible="provenanceDetailsAreVisible"
                    :showURI="false"
                    :key="refreshKey"
                    class="searchFilter"
                    @handlingEnterKey="refresh"
                  />
                </n-form-item>

                <ProvenanceDetails
                  v-if="selectedProvenance && provenanceDetailsAreVisible"
                  :provenance="selectedProvenance"
                  class="provenanceDetails"
                />

                <n-space justify="end" class="mt-2">
                  <Button
                    class="resetButton"
                    :label="t('component.common.search.clear-button')"
                    icon="bi-x-lg"
                    @click="clear"
                  />
                  <Button
                    class="greenThemeColor"
                    :label="t('component.common.search.search-button')"
                    icon="bi-search"
                    @click="refresh"
                  />
                </n-space>
              </n-form>
            </n-space>
          </n-layout-sider>

          <!-- List content -->
          <n-layout-content class="datafiles-content">

            <DataFilesList
              key="list-view"
              ref="datafilesList"
              :filter="filter"
              class="datafilesList"
              @redirectToDetail = "emit('redirectToDetail')"
            />

          </n-layout-content>
        </n-layout>
      </template>
    </PageContent>
  </div>

</template>

<script setup lang="ts">

import { ProvenanceGetDTO, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import { useI18n } from "vue-i18n";
import {useRoute} from "vue-router";
import DataFilesList, {DatafileFilter} from "@/components/data/DataFilesList.vue";
import {useStore} from "vuex";
import DatafileProvenanceSelector from "@/components/data/DatafileProvenanceSelector.vue";
import {NButton, NForm, NFormItem, NInput, NLayout, NLayoutContent, NLayoutSider, NSpace, NSwitch} from "naive-ui";
import Button from "@/components/common/buttons/Button.vue";
import PageContent from "@/components/layout/PageContent.vue";
import ProvenanceDetails from "@/components/data/ProvenanceDetails.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import {DataService} from "opensilex-core/api/data.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import Oeso from "@/ontologies/Oeso";

//#region Constants & Services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const { t } = useI18n();
const $route = useRoute();
const $store = useStore();
const dataService: DataService = $opensilex.getService("opensilex.DataService");
const ontologyService: OntologyService = $opensilex.getService("opensilex.OntologyService");
//#endregion

//#region Emits
const emit = defineEmits<{
  (e: 'redirectToDetail'): void
}>()
//#endregion

//#region Reactive Data
const provenanceDetailsAreVisible = ref<boolean>(false);
const selectedProvenance = ref<ProvenanceGetDTO | null>(null);
const filterProvenanceLabel = ref<string>(null);
const searchFiltersVisible = ref<boolean>(false);
const uri = ref<string>(null);
const typeNamePerTypeURI = ref<Map<string, string>>(new Map());

const filter = ref<DatafileFilter>({
  start_date: null,
  end_date: null,
  rdf_type: null,
  provenance: null,
  experiments: [],
  scientificObjects: uri.value ? [uri.value] : []
});

//TODO MAX eliminate refesh key?
const refreshKey = ref(0);
//#endregion

//#region non-constants
let langUnwatcher: (() => void) | undefined;
//#endregion

//#region Computed

const activeFiltersCount = computed(() => {
  return [
    filter.value.rdf_type,
    filter.value.provenance,
    filter.value.start_date,
    filter.value.end_date,
    ...(filter.value.experiments || []),
  ].filter(v => {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && String(v).trim() !== ''
  }).length
});

const getSelectedProv = computed(() => {
  return selectedProvenance.value;
});
//#endregion

//#region Template refs
//TODO MAX elminate template refs?
const datafilesList = useTemplateRef<InstanceType<typeof DataFilesList>>('datafilesList');
//#endregion

//#region Hooks
onMounted(() => {
  uri.value = decodeURIComponent($route.params.uri as string);
  loadTypes()
  resetFilters()

  const query = $route.query
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

  filter.value.scientificObjects = [uri]

  langUnwatcher = $store.watch(
    () => $store.getters.language,
    () => {
      loadTypes();
      refresh();
    }
  )
});

onBeforeUnmount(() => {
  langUnwatcher?.()
});
//#endregion

//#region Functions
function resetFilters() {
  filter.value = {
    start_date: undefined,
    end_date: undefined,
    rdf_type: undefined,
    provenance: undefined,
    experiments: [],
    scientificObjects: [uri.value]
  };
}

function refreshComponents(){
  //TODO MAX remove key?
  refreshKey.value += 1;
}

function showProvenanceDetails() {
  if (selectedProvenance.value !== null) {
    provenanceDetailsAreVisible.value = !provenanceDetailsAreVisible.value;
  }
}

function clear() {
  selectedProvenance.value = null;
  resetFilters()
  filterProvenanceLabel.value = null;
  refresh();
}

function getProvenance(uri) {
  if (uri != undefined) {
    return dataService
      .getProvenance(uri)
      .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
        return http.response.result;
      });
  }
}

async function loadProvenance(selectedValue: any) {
  if (selectedValue?.id) {
    selectedProvenance.value = await getProvenance(selectedValue.id)
  }
}

function refresh() {
  //TODO MAX remove this shit?
  datafilesList.value.refresh();
}

function loadTypes() {
  ontologyService.getSubClassesOf(Oeso.DATAFILE_TYPE_URI, false)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      console.log(http.response.result);
      let parentType = http.response.result[0];
      let key = parentType.uri;
      typeNamePerTypeURI.value[key] = parentType.name;
      for (let i = 0; i < parentType.children.length; i++) {
        let key = parentType.children[i].uri;
        typeNamePerTypeURI.value[key] = parentType.children[i].name;
        if (Oeso.checkURIs(key, Oeso.IMAGE_TYPE_URI)) {
          let imageType = parentType.children[i];
          for (let i = 0; i < imageType.children.length; i++) {
            let key = imageType.children[i].uri;
            typeNamePerTypeURI.value[key] = imageType.children[i].name;
          }
        } else {
          let subType = parentType.children[i];
          for (let i = 0; i < subType.children.length; i++) {
            let key = subType.children[i].uri;
            typeNamePerTypeURI.value[key] = subType.children[i].name;
          }
        }
      }
    })
    .catch($opensilex.errorHandler);
}
//#endregion

</script>

<style scoped lang="scss">
.card-body {
  margin-bottom: -15px;
}
.datafiles-layout {
  background: transparent;
}
.datafiles-content {
  padding-left: 12px;
}

</style>

<i18n>
en:
  ScientificObjectDataFiles:
    datafiles: Datafiles
    rdfType: Type
    displayImage: Display image

fr:
  ScientificObjectDataFiles:
    datafiles: Fichiers de données
    rdfType : Type
    displayImage: Afficher l'image

</i18n>