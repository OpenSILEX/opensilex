<template>
  <div class="container-fluid">
    <opensilex-PageActions>
      <opensilex-CreateButton
          v-if="user.hasCredential(
              credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
          @click="modalDataForm.showCreateForm()"
          label="OntologyCsvImporter.import"
          class="createButton greenThemeColor"
      >
      </opensilex-CreateButton>
      <b-button
          @click="exportModal.show()"
          class="exportButton greenThemeColor createButton"
      >
        export
      </b-button>
    </opensilex-PageActions>


    <opensilex-DataExportModal
        ref="exportModal"
        :filter="filter"
    ></opensilex-DataExportModal>


    <opensilex-ModalForm
        ref="modalDataForm"
        createTitle="DataImportForm.create"
        editTitle="DataImportForm.update"
        component="opensilex-DataImportForm"
        icon="ik#ik-bar-chart-line"
        modalSize="xl"
        @onCreate="afterCreateData"
        :successMessage="successMessage"
    ></opensilex-ModalForm>

    <opensilex-PageContent
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

            <opensilex-SearchFilterField
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

                <!-- Germplasm Group -->
                <div>
                  <opensilex-FilterField>
                    <opensilex-GermplasmGroupSelector
                        label="GermplasmList.filter.germplasm-group"
                        :multiple="false"
                        :germplasmGroup.sync="filter.germplasm_group"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></opensilex-GermplasmGroupSelector>
                  </opensilex-FilterField>
                </div>
                <!-- Variables -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-VariableSelectorWithFilter
                        placeholder="VariableSelectorWithFilter.placeholder-multiple"
                        :variables.sync="filter.variables"
                        :withAssociatedData="true"
                        class="searchFilter"
                    ></opensilex-VariableSelectorWithFilter>
                  </opensilex-FilterField>
                </div>

                <!-- Experiments -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-ExperimentSelector
                        label="DataView.filter.experiments"
                        :experiments.sync="filter.experiments"
                        :multiple="true"
                        @select="updateSOFilter"
                        @clear="updateSOFilter"
                        class="searchFilter"
                    ></opensilex-ExperimentSelector>
                  </opensilex-FilterField>
                </div>

                <!-- Scientific objects -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-SelectForm
                        ref="soSelector"
                        label="DataView.filter.scientificObjects"
                        placeholder="DataView.filter.scientificObjects-placeholder"
                        :selected.sync="filter.scientificObjects"
                        modalComponent="opensilex-ScientificObjectModalList"
                        class="searchFilter"
                        :filter.sync="soFilter"
                        :isModalSearch="true"
                        :clearable="true"
                        :multiple="true"
                        @clear="refreshSoSelector"
                        @onValidate="refreshComponent"
                        @onClose="refreshComponent"
                        @select="refreshComponent"
                        :limit="1"
                    ></opensilex-SelectForm>
                  </opensilex-FilterField>
                </div>

                <!-- Devices in provenance-->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-DeviceSelector
                      ref="deviceSelector"
                      :multiple="true"
                      :value.sync='filter.devices'
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                    ></opensilex-DeviceSelector>
                  </opensilex-FilterField>
                </div>

                <!-- Operator -->
                <div>
                  <opensilex-FilterField>
                    <opensilex-UserSelector
                      :users.sync="filter.operators"
                      label="DataView.filter.operator"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                      :multiple="true"
                    ></opensilex-UserSelector>
                  </opensilex-FilterField>
                </div>

                <!-- Facility -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-FacilitySelector
                      ref="facilitySelector"
                      label="InfrastructureForm.form-facilities-label"
                      :facilities.sync="filter.facilities"
                      :multiple="true"
                      class="searchFilter"
                    ></opensilex-FacilitySelector>
                  </opensilex-FilterField>
                </div>

                <!-- Start Date -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-DateTimeForm
                        :value.sync="filter.start_date"
                        label="component.common.begin"
                        name="startDate"
                        :max-date="filter.end_date ? filter.end_date : undefined"
                        class="searchFilter"
                    ></opensilex-DateTimeForm>
                  </opensilex-FilterField>
                </div>

                <!-- End Date -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-DateTimeForm
                        :value.sync="filter.end_date"
                        label="component.common.end"
                        name="endDate"
                        :min-date="filter.start_date ? filter.start_date : undefined"
                        class="searchFilter"
                    ></opensilex-DateTimeForm>
                  </opensilex-FilterField>
                </div>

                <!-- Provenance -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-DataProvenanceSelector
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
                    ></opensilex-DataProvenanceSelector>

                    <b-collapse
                        v-if="selectedProvenance"
                        id="collapse-4"
                        v-model="visibleDetails"
                    >
                      <opensilex-ProvenanceDetails
                          :provenance="getSelectedProv"
                      ></opensilex-ProvenanceDetails>
                    </b-collapse>
                  </opensilex-FilterField>
                </div>
              </template>
              <template v-slot:advancedSearch>
                <!-- Targets -->
                <div>
                  <opensilex-FilterField quarterWidth="true">
                    <opensilex-TagInputForm
                        v-if="loadAdvancedSearchFilters"
                        class="searchFilter"
                        :value.sync="filter.targets"
                        label="DataView.filter.targets"
                        helpMessage="DataView.filter.targets-help"
                        type="text"
                        @handlingEnterKey="refresh()"
                    ></opensilex-TagInputForm>
                  </opensilex-FilterField>
                </div>
              </template>
            </opensilex-SearchFilterField>
          </div>
        </Transition>

        <opensilex-DataList
            ref="dataList"
            :listFilter.sync="filter"
            class="dataList">
        </opensilex-DataList>

      </template>
    </opensilex-PageContent>

    <opensilex-ResultModalView ref="resultModal" @onHide="refreshDataAfterImportation()"></opensilex-ResultModalView>

    <opensilex-DataProvenanceModalView
        ref="dataProvenanceModalView"
    ></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ProvenanceGetDTO} from "opensilex-core/index";
import {ScientificObjectNodeDTO} from "opensilex-core/model/scientificObjectNodeDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataService} from "opensilex-core/api/data.service";

@Component
export default class DataView extends Vue {
  $opensilex: any;
  $store: any;
  dataService: DataService;
  disabled = false;

  visibleDetails: boolean = false;
  selectedProvenance: ProvenanceGetDTO = null;
  filterProvenanceLabel: string = null;
  refreshKey = 0;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get lang() {
    return this.$store.getters.language;
  }

  @Ref("templateForm") readonly templateForm!: any;
  @Ref("dataList") readonly dataList!: any;
  @Ref("modalDataForm") readonly modalDataForm!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("resultModal") readonly resultModal!: any;
  @Ref("soSelector") readonly soSelector!: any;
  @Ref("varSelector") readonly varSelector!: any;
  @Ref("exportModal") readonly exportModal!: any;
  @Ref("deviceSelector") readonly deviceSelector!: any;
  @Ref("facilitySelector") readonly facilitySelector!: any;

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
    operators: []
  };

  soFilter = {
    name: "",
    experiment: undefined,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  };

  data() {
    return {
      SearchFiltersToggle: false,
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
      operators: []
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

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      this.dataService
          .getProvenance(uri)
          .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
            this.selectedProvenance = http.response.result;
          });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
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

fr:
  DataView:
    buttons:
      create-data: Ajouter un jeu de données
      generate-template: Générer un gabarit
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

</i18n>
