<template>
  <div class="container-fluid">
    <PageActions>
      <!-- Create Button -->
      <CreateButton
          v-if="user.hasCredential(
              credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
          @click="showImportForm()"
          label="OntologyCsvImporter.import"
          class="createButton greenThemeColor"
      >
      </CreateButton>
      <!-- Export button-->
      <b-button
          @click="exportModal.show()"
          class="exportButton greenThemeColor createButton"
      >
        export
      </b-button>
      <!-- Delete by batch button -->
      <Button
        @click="deleteByBatchModal.show()"
        class="createButton greenThemeColor"
        icon="fa#trash-alt"
        :small="false"
        label="DataView.buttons.delete-by-batch"
        :disabled="false"
      ></Button>
    </PageActions>


    <DataExportModal
        ref="exportModal"
        :filter="filter"
    ></DataExportModal>

    <DeleteByBatchModal
      ref="deleteByBatchModal"
      @deleted="refresh"
    ></DeleteByBatchModal>

      <!-- Toggle Sidebar-->

    <Modal
        v-if="renderImportForm"
        ref="modalDataForm"
        createTitle="DataImportForm.create"
        editTitle="DataImportForm.update"
        component="DataImportForm"
        icon="ik#ik-bar-chart-line"
        modalSize="xl"
        @onCreate="afterCreateData"
        :successMessage="successMessage"
    ></Modal>

    <PageContent
        class="pagecontent">
      <template v-slot>

        <!-- Toggle Sidebar-->
        <div class="searchMenuContainer"
             v-on:click="toggleFilter()"
             :title="searchFiltersPannel()">
          <div class="searchMenuIcon">
            <i class="icon ik ik-search"></i>
          </div>
        </div>
        <!-- FILTERS -->
        <Transition>
          <div v-show="toggleSearchFilters">

            <SearchFilterField v-if="loadSearchFilters"
                ref="searchField"
                @search="refresh()"
                @clear="reset()"
                @toggleAdvancedSearch="loadAdvancedFilter"
                :showAdvancedSearch="true"
                label="DataView.filter.label"
                :showTitle="false"
                class="searchFilterField"
            >
              <template v-slot:filters>

                <!-- Variables -->
                <div>
                  <FilterField quarterWidth="true">
                    <VariableSelectorWithFilter
                        placeholder="VariableSelectorWithFilter.placeholder-multiple"
                        :variables.sync="filter.variables"
                        :withAssociatedData="true"
                        class="searchFilter"
                    ></VariableSelectorWithFilter>
                  </FilterField>
                </div>

                <!-- Experiments -->
                <div>
                  <FilterField quarterWidth="true">
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
                  <FilterField quarterWidth="true">
                    <ModalFormSelector
                        ref="soSelector"
                        label="DataView.filter.scientificObjects"
                        placeholder="DataView.filter.scientificObjects-placeholder"
                        :selected.sync="filter.scientificObjects"
                        modalComponent="ScientificObjectModalList"
                        class="searchFilter"
                        :filter.sync="soFilter"
                        :clearable="true"
                        :multiple="true"
                        @clear="refreshSoSelector"
                        @onValidate="refreshComponent"
                        @onClose="refreshComponent"
                        @select="refreshComponent"
                        :limit="1"
                    ></ModalFormSelector>
                  </FilterField>
                </div>

                <!-- Germplasm Group -->
                <div>
                  <FilterField>
                    <GermplasmGroupSelector
                        label="GermplasmList.filter.germplasm-group"
                        :multiple="false"
                        :germplasmGroup.sync="filter.germplasm_group"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></GermplasmGroupSelector>
                  </FilterField>
                </div>

                <!-- germplasm -->
                <div>
                  <FilterField quarterWidth="false">
                    <GermplasmSelectorWithFilter
                        :germplasmsUris.sync="filter.germplasm"
                    ></GermplasmSelectorWithFilter>
                  </FilterField>
                </div>

                <!-- Devices in provenance-->
                <div>
                  <FilterField quarterWidth="true">
                    <DeviceSelector
                      ref="deviceSelector"
                      :multiple="true"
                      :value.sync='filter.devices'
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                    ></DeviceSelector>
                  </FilterField>
                </div>

                <!-- Operator -->
                <div>
                  <FilterField>
                    <PersonSelector
                      :persons.sync="filter.operators"
                      label="DataView.filter.operator"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                      :multiple="true"
                    ></PersonSelector>
                  </FilterField>
                </div>

                <!-- Facility -->
                <div>
                  <FilterField quarterWidth="true">
                    <FacilitySelector
                      ref="facilitySelector"
                      label="OrganizationForm.form-facilities-label"
                      :facilities.sync="filter.facilities"
                      :multiple="true"
                      class="searchFilter"
                    ></FacilitySelector>
                  </FilterField>
                </div>

                <!-- Start Date -->
                <div>
                  <FilterField quarterWidth="true">
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
                  <FilterField quarterWidth="true">
                    <DateTimeForm
                        :value.sync="filter.end_date"
                        label="component.common.end"
                        name="endDate"
                        :min-date="filter.start_date ? filter.start_date : undefined"
                        class="searchFilter"
                    ></DateTimeForm>
                  </FilterField>
                </div>

                <!-- Provenance -->
                <div>
                  <FilterField quarterWidth="true">
                    <DataProvenanceSelector
                        ref="provSelector"
                        :provenances.sync="filter.provenance"
                        label="ExperimentData.provenance"
                        @select="loadProvenance"
                        :targets="filter.scientificObjects"
                        :experiments="filter.experiments"
                        :multiple="false"
                        :viewHandler="showProvenanceDetails"
                        :viewHandlerDetailsVisible="visibleDetails"
                        :key="refreshKey"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></DataProvenanceSelector>

                    <b-collapse
                        v-if="selectedProvenance"
                        id="collapse-4"
                        v-model="visibleDetails"
                    >
                      <ProvenanceDetails
                          :provenance="getSelectedProv"
                      ></ProvenanceDetails>
                    </b-collapse>
                  </FilterField>
                </div>
              </template>
              <template v-slot:advancedSearch>
                <!-- Targets -->
                <div>
                  <FilterField quarterWidth="true">
                    <TagInputForm
                        v-if="loadAdvancedSearchFilters"
                        class="searchFilter"
                        :value.sync="filter.targets"
                        label="DataView.filter.targets"
                        helpMessage="DataView.filter.targets-help"
                        type="text"
                        @handlingEnterKey="refresh()"
                    ></TagInputForm>
                  </FilterField>
                </div>
                <!-- Batch URI -->
                <div>
                  <FilterField>
                    <label>{{ $t('DataView.filter.batch-uri') }}</label>
                    <StringFilter
                      :filter.sync="filter.batch_uri"
                      placeholder="DataView.filter.uri-placeholder"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                    ></StringFilter>
                  </FilterField>
                  <br>
                </div>
              </template>
            </SearchFilterField>
          </div>
        </Transition>

        <DataList
            ref="dataList"
            :listFilter.sync="filter"
            class="dataList">
        </DataList>

      </template>
    </PageContent>

    <ResultModalView ref="resultModal" @onHide="refreshDataAfterImportation()"></ResultModalView>

    <DataProvenanceModalView
        ref="dataProvenanceModalView"
    ></DataProvenanceModalView>
  </div>
</template>

<script setup lang="ts">
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataService} from "opensilex-core/api/data.service";
import DeleteByBatchModal from "./DeleteByBatchModal.vue";
import DataProvenanceModalView from "@/components/data/DataProvenanceModalView.vue";
import {useStore} from "vuex";
import { ref } from "process";
import {ProvenanceGetDTO} from "opensilex-core/lib";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {computed, inject, useTemplateRef} from "vue";
import Modal from "@/components/common/views/Modal.vue";
import PageContent from "@/components/layout/PageContent.vue";
import ExperimentSelector from "@/components/experiments/ExperimentSelector.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import PersonSelector from "@/components/persons/PersonSelector.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import TagInputForm from "@/components/common/forms/TagInputForm.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import DeviceSelector from "@/components/devices/DeviceSelector.vue";
import FacilitySelector from "@/components/facilities/FacilitySelector.vue";


const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
const dataService = opensilex.getService<DataService>('opensilex.DataSerice');
const disabled = ref<boolean>(false)
const visibleDetails = ref<boolean>(false)
const selectedProvenance = ref<ProvenanceGetDTO>(null)
const filterProvenanceLabel = ref<string>(null)
const refreshKey = ref<number>(0);
const resetExperimentSelectorKey = ref<number>(0);

const user = computed(() =>
{
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})

const lang = computed(() => {
  return store.getters.language;
})

const templateForm = useTemplateRef<any>('templateForm');
const dataList = useTemplateRef<InstanceType<typeof DataList>>('dataList');
const modalDataForm = useTemplateRef<InstanceType<typeof Modal>>('modalDataForm');
const searchField = useTemplateRef<any>('searchField');
const provSelector = useTemplateRef<any>('provSelector');
const resultModal = useTemplateRef<InstanceType<typeof ResultModalView>>('resultModal');
const soSelector = useTemplateRef<InstanceType<typeof ModalFormSelector>>('soSelector');
const exportModal = useTemplateRef<InstanceType<typeof DataExportModal>>('exportModal');
const deviceSelector = useTemplateRef<InstanceType<typeof DeviceSelector>>('deviceSelector');
const facilitySelector = useTemplateRef<InstanceType<typeof FacilitySelector>>('facilitySelector');
const deleteByBatchModal = useTemplateRef<InstanceType<typeof DeleteByBatchModal>>('deleteByBatchModal');

  filter = {
    germplasm_group: undefined,
    start_date: null,
    end_date: null,
    variables: [],
    provenance: null,
    experiments: [],
    scientificObjects: [],
    targets: [],
    devices: [],
    facilities: [],
    operators: [],
    germplasm: [],
    batch_uri: undefined
  };

  soFilter = {
    name: "",
    experiment: undefined,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
    criteriaDto: {criteria_list:[]}
  };

  toggleSearchFilters: boolean = false;
  loadSearchFilters: boolean = false;
  renderImportForm: boolean = false;

    /**
     * Show or hide the search filter (v-show) on the filter div
     * Trigger render of search filters selector (v-if).
     * This ensures that API methods corresponding with the selector are not executed
     * at the render of this component but only at the first toggle of the filter
     *
     */
    toggleFilter(){
        this.toggleSearchFilters = ! this.toggleSearchFilters;
        if(! this.loadSearchFilters){
            this.loadSearchFilters = true;
        }
    }

  refreshSoSelector() {

    this.soSelector.refreshModalSearch();
    this.refreshComponent();
  }

  refreshComponent() {
    this.refreshKey += 1
  }

  resetFilter() {
    this.filter = {
      germplasm_group: undefined,
      start_date: null,
      end_date: null,
      variables: [],
      provenance: null,
      experiments: [],
      scientificObjects: [],
      targets: [],
      devices: [],
      facilities: [],
      operators: [],
      germplasm: [],
      batch_uri: undefined
    };

    this.soSelector.refreshModalSearch();
  }

  updateSOFilter() {
    this.refreshComponent();
    this.soFilter.experiment = this.filter.experiments[0];
    this.soSelector.refreshModalSearch();
  }

  refresh() {
    this.dataList.refresh();
  }

  reset() {
    this.resetFilter();
    this.refresh();
    this.resetExperimentSelectorKey+=1;
  }

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  getProvenance(uri: string) {
    if (uri != undefined) {
      this.dataService
          .getProvenance(uri)
          .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
            this.selectedProvenance = http.response.result;
          });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined) {
      this.getProvenance(selectedValue.id);
    }
  }

  afterCreateData(results) {
      if (results instanceof Promise) {
        results.then((res) => {
          this.resultModal.setNbLinesImported(
              res.validation.dataErrors.nbLinesImported
          );
          let annotationsOnObjects : Array<any> = res.validation.dataErrors.annotationsOnObjects;
          if(annotationsOnObjects){
              this.resultModal.setNbAnnotationsImported(
                  annotationsOnObjects.length
              );
          }
          this.resultModal.setProvenance(res.form.provenance);
          this.resultModal.setBatch(res.validation.dataErrors.batchHistoryUri);
          this.resultModal.show();
          this.refreshKey += 1;
          this.clear();
          this.filter.provenance = res.form.provenance.uri;
        });
    } else {
      this.resultModal.setNbLinesImported(
          results.validation.dataErrors.nbLinesImported
      );
      let annotationsOnObjects : Array<any> = results.validation.dataErrors.annotationsOnObjects;
      if(annotationsOnObjects){
          this.resultModal.setNbAnnotationsImported(
              annotationsOnObjects.length
          );
      }
      this.resultModal.setBatch(results.validation.dataErrors.batchHistoryUri);
      this.resultModal.setProvenance(results.form.provenance);
      this.resultModal.show();
      this.refreshKey += 1;
      this.clear();
      this.filter.provenance = results.form.provenance.uri;
    }

  }

  clear() {
    this.selectedProvenance = null;
    this.resetFilter();
    this.filterProvenanceLabel = null;
  }

  successMessage(form) {
    return this.$t("ResultModalView.data-imported");
  }

  refreshDataAfterImportation() {
    this.refresh();
  }

  searchFiltersPannel() {
    return this.$t("searchfilter.label")
  }

  loadAdvancedSearchFilters: boolean = false;

  /**
   * Show or hide the advanced search filter (v-show) on the filter div
   * Trigger render of advanced search filters selector (v-if)
   * This ensures that API methods corresponding with the selector are not executed
   * at the render of this component but only at the first toggle of the advanced filter
   */
  loadAdvancedFilter(){
      if(! this.loadAdvancedSearchFilters){
          this.loadAdvancedSearchFilters = true;
      }
  }

  showImportForm(){
      this.renderImportForm = true;
      this.$nextTick(() => {
          this.modalDataForm.showCreateForm();
      });
  }

</script>

<style scoped lang="scss">

.createButton {
  margin-bottom: 10px;
  margin-top: -15px
}
</style>

<i18n>

en:
  DataView:
    buttons:
      create-data: Add data
      generate-template: Generate template
      delete-by-batch: Delete by Batch
    description: View and export data
    list:
      date: Date
      variable: Variable
      value: Value
      object: Target
      provenance: provenance
      details: view data details
    filter:
      label: Search data
      experiments: Experiment(s)
      variables: Variable(s)
      scientificObjects: Scientific object(s)
      scientificObjects-placeholder: Select scientific objects
      devices: Device(s)
      devices-placeholder:  Select devices
      provenance: Provenance
      targets: Target(s)
      targets-help: Copy target's URI here
      operator: Operators
      batch-uri : Batch URI
      uri-placeholder: Enter a part of an uri

fr:
  DataView:
    buttons:
      create-data: Ajouter un jeu de données
      generate-template: Générer un gabarit
      delete-by-batch: Supprimer par Batch
    description: Visualiser et exporter des données
    list:
      date: Date
      variable: Variable
      value: Valeur
      object: Objet cible
      provenance: provenance
      details: Voir les détails de la donnée
    filter:
      label: Rechercher des données
      experiments: Expérimentation(s)
      variables: Variable(s)
      scientificObjects: Objet(s) scientifique(s)
      scientificObjects-placeholder: Sélectionner des objets scientifiques
      devices: Dispositifs
      devices-placeholder: Sélectionner les dispositifs
      provenance: Provenance
      targets: Cible(s)
      targets-help: Copier les URI des cibles ici
      operator: Opérateurs
      batch-uri : URI de Batch
      uri-placeholder: Entrer une partie d'une uri

</i18n>
