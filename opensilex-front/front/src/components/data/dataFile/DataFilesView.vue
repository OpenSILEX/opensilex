<template>
  <div class="container-fluid">
    <PageActions>
      <CreateButton
          @click="datafileForm.showCreateForm()"
          label="DataFilesList.add"
          class="createButton"
      ></CreateButton>
    </PageActions>

    <PageContent
        class="pagecontent">
      <template v-slot>

        <!-- Toggle Sidebar-->
        <div class="searchMenuContainer"
             v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
             :title="searchFiltersPannel()">
          <div class="searchMenuIcon">
            <i class="icon ik ik-search"></i>
          </div>
        </div>
        <!-- FILTERS -->
        <Transition>
          <div v-show="SearchFiltersToggle">

            <SearchFilterField
                @search="refresh()"
                @clear="reset()"
                label="DataView.filter.label"
                :showTitle="false"
                class="searchFilterField"
                advanced-search-label="" show-advanced-search>
              <template v-slot:filters>
                <!-- FileName -->
                <div>
                  <FilterField>
                    <label for="name">{{ $t("DataFilesView.fileName") }}</label>
                    <StringFilter
                        id="name"
                        :filter.sync="filter.name"
                        placeholder="DataFilesView.fileName-placeholder"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></StringFilter>
                    <br>
                  </FilterField>
                </div>

                <!-- Type -->
                <div>
                  <FilterField>
                    <TypeForm
                        v-if="filter.imagesView"
                        :type.sync="filter.rdf_type"
                        :baseType="opensilex.Oeso.IMAGE_TYPE_URI"
                        :ignoreRoot="false"
                        placeholder="ScientificObjectDataFiles.rdfType-placeholder"
                        class="searchFilter"
                        key="imageTypeForm"
                        @handlingEnterKey="refresh()"
                    ></TypeForm>

                    <TypeForm
                        v-else
                        :type.sync="filter.rdf_type"
                        :baseType="opensilex.Oeso.DATAFILE_TYPE_URI"
                        :ignoreRoot="false"
                        placeholder="ScientificObjectDataFiles.rdfType-placeholder"
                        key="datafileTypeForm"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></TypeForm>
                  </FilterField>
                </div>

                <!-- Experiments -->
                <div>
                  <FilterField>
                    <ExperimentSelector
                        label="DataView.filter.experiments"
                        :experiments.sync="filter.experiments"
                        :multiple="true"
                        @select="updateSOFilter"
                        @clear="updateSOFilter"
                        class="searchFilter"
                        :key="resetExperimentSelectorKey"
                    ></ExperimentSelector>
                  </FilterField>
                </div>

                <!-- Scientific objects -->
                <div>
                  <FilterField>
                    <ModalFormSelector
                        ref="soSelector"
                        label="DataView.filter.scientificObjects"
                        placeholder="DataView.filter.scientificObjects-placeholder"
                        :selected.sync="filter.scientificObjects"
                        modalComponent="ScientificObjectModalList"
                        :filter.sync="soFilter"
                        :clearable="true"
                        :multiple="true"
                        @onValidate="refreshProvComponent"
                        @onClose="refreshProvComponent"
                        @clear="refreshSoSelector"
                        :limit="1"
                        class="searchFilter"
                    ></ModalFormSelector>
                  </FilterField>

                </div>


                <!-- Start Date -->
                <div>
                  <FilterField>
                    <DateTimeForm
                        :value.sync="filter.start_date"
                        label="component.common.begin"
                        name="startDate"
                        :max-date="filter.end_date ? filter.end_date : undefined"
                        class="searchFilter"
                    ></DateTimeForm>
                  </FilterField>
                </div>

                <!-- End Date -->
                <div>
                  <FilterField>
                    <DateTimeForm
                        :value.sync="filter.end_date"
                        label="component.common.end"
                        name="endDate"
                        :min-date="filter.start_date ? filter.start_date : undefined"
                        :minDate="filter.start_date"
                        :maxDate="filter.end_date"
                        class="searchFilter"
                    ></DateTimeForm>
                  </FilterField>
                </div>

                <!-- Provenance -->
                <div>
                  <FilterField>
                    <DatafileProvenanceSelector
                        ref="provSelector"
                        :provenances.sync="filter.provenance"
                        label="ExperimentData.provenance"
                        @select="loadProvenance"
                        :targets="filter.scientificObjects"
                        :experiments="filter.experiments"
                        :multiple="false"
                        :viewHandler="showProvenanceDetails"
                        :viewHandlerDetailsVisible="visibleDetails"
                        :showURI="false"
                        :key="refreshKey"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></DatafileProvenanceSelector>

                    <b-collapse
                        v-if="selectedProvenance"
                        id="collapse-4"
                        v-model="visibleDetails"
                        class="mt-2"
                    >
                      <ProvenanceDetails
                          :provenance="getSelectedProv"
                      ></ProvenanceDetails>
                    </b-collapse>
                  </FilterField>
                </div>

                <!-- Images -->
                <FilterField>
                  <b-form-checkbox v-model="filter.imagesView" name="check-button" switch>
                    <b> {{ $t("DataFilesView.imagesView") }}</b>
                  </b-form-checkbox>
                </FilterField>

              </template>
            </SearchFilterField>
          </div>
        </Transition>

        <DataFilesImagesList
            v-if="filter.imagesView"
            ref="datafilesImagesList"
            :filter="filter"
            class="imagesList">
        </DataFilesImagesList>

        <DataFilesList
            v-else
            ref="datafilesList"
            :filter="filter"
            class="datafilesList">
        </DataFilesList>

        <Modal
            ref="datafileForm"
            component="DataFileForm"
            editTitle="update"
            createTitle="DataFilesList.add"
            icon="ik#ik-file-text"
            modalSize="lg"
            @onCreate="refresh()"
        >
        </Modal>

      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {DataService} from "opensilex-core/api/data.service";
import PageActions from "@/components/layout/PageActions.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import SearchFilterField from "@/components/common/filters/SearchFilterField.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import ExperimentSelector from "@/components/experiments/ExperimentSelector.vue";
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import DatafileProvenanceSelector from "@/components/data/DatafileProvenanceSelector.vue";
import ProvenanceDetails from "@/components/data/ProvenanceDetails.vue";
import DataFilesImagesList from "@/components/data/DataFilesImagesList.vue";
import DataFilesList from "@/components/data/DataFilesList.vue";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import Modal from "@/components/common/views/Modal.vue";
import {validate} from "vee-validate";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {ProvenanceGetDTO} from "opensilex-core/lib";
import StringFilter from "@/components/common/filters/StringFilter.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const disabled = ref<boolean>(false);
const {t} = useI18n();
const refreshKey = ref<number>(0)
const visibleDetails = ref<boolean>(false)
const selectedProvenance = ref<any>(null)
const filterProvenanceLabel = ref<string>(null);
const resetExperimentSelectorKey = ref<number>(0);
const templateForm = ref();
const datafilesList = useTemplateRef<InstanceType<typeof DataFilesList>>('datafilesList')
const datafilesImagesList = useTemplateRef<InstanceType<typeof DataFilesImagesList>>('datafilesImagesList')
const provSelector = useTemplateRef<InstanceType<typeof DatafileProvenanceSelector>>('provSelector')
const soSelector = useTemplateRef<InstanceType<typeof ModalFormSelector>>('ModalFormSelector')
const datafileForm = useTemplateRef<InstanceType<typeof Modal>>('datafileForm')

const user = computed(() => {
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})

const getSelectedProv = computed(() => {
  return selectedProvenance;
})

const filter = ref({
  name: undefined,
  start_date: undefined,
  end_date: undefined,
  rdf_type: undefined,
  provenance: undefined,
  experiments: [],
  scientificObjects: [],
  imagesView: false
});

const soFilter = ref({
  name: undefined,
  experiment: undefined,
  germplasm: undefined,
  factorLevels: [],
  types: [],
  existenceDate: undefined,
  creationDate: undefined,
});

function resetFilter()
{
  filter.value = {
    name: undefined,
    start_date: undefined,
    end_date: undefined,
    rdf_type: undefined,
    provenance: undefined,
    experiments: [],
    scientificObjects: [],
    imagesView: false
  };
}

function data()
{
  return {
    SearchFiltersToggle: false,
  }
}

function refreshSoSelector()
{
  refreshProvComponent();
  soSelector.value.refreshModalSearch();
}

function refreshProvComponent()
{
  refreshKey.value += 1;
}

function updateSOFilter()
{
  soFilter.value.experiment = filter.value.experiments[0];
  refreshProvComponent();
  soSelector.value.refreshModalSearch();
}

function refresh()
{
  if (filter.value.start_date === "") {
    filter.value.start_date = undefined;
  }
  if (filter.value.end_date === "") {
    filter.value.end_date = undefined;
  }
  if (filter.value.imagesView) {
    datafilesImagesList.value.refresh();
  } else {
    datafilesList.value.refresh();
  }
}

function reset()
{
  resetFilter();
  refresh();
  if (filter.value.start_date === "") {
    filter.value.start_date = undefined;
  }
  if (filter.value.end_date === "") {
    filter.value.end_date = undefined;
  }
  this.resetExperimentSelectorKey++;
}


function showProvenanceDetails()
{
  if (selectedProvenance.value != null) {
    visibleDetails.value = !this.visibleDetails;
  }
}

 function getProvenance(uri)
{
  if (uri != undefined && uri != null) {
    let dataService: DataService = opensilex.getService<DataService>("opensilex.DataService");
    return dataService.getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
  }
}

function loadProvenance(selectedValue)
{
  if (selectedValue != undefined && selectedValue != null) {
    getProvenance(selectedValue.id).then((prov) => {
      selectedProvenance.value = prov;
    });
  }
}
function searchFiltersPannel()
{
  return t("searchfilter.label")
}

</script>


<style scoped lang="scss">
.imagesList {
  min-width: 70%;
  width: 100%;
}

.datafilesList {
  width: 100%
}

.createButton {
  margin-bottom: 10px;
  margin-top: -15px
}

</style>

<i18n>
en:
  DataFilesView:
    description: View datafiles
    details: view datafile metadata
    fileName: File Name
    fileName-placeholder: Enter file name
    imagesView: View

fr:
  DataFilesView:
    description: Voir les fichiers de données
    details: Voir les métadonnées du fichier
    fileName: Nom de fichier
    fileName-placeholder: Saisir un nom de fichier
    imagesView: Visualisation
</i18n>