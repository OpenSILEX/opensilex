<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="component.menu.data.label"
      description="DataView.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
          @click="modalDataForm.showCreateForm()"
          label="OntologyCsvImporter.import"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

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

    <opensilex-PageContent>
      <template v-slot>

        <opensilex-SearchFilterField
          ref="searchField"
          @search="refresh()"
          @clear="reset()"
          label="DataView.filter.label"
          :showTitle="false"
        >
          <template v-slot:filters>

              <!-- Variables -->
              <opensilex-FilterField  quarterWidth="true">
                <opensilex-VariableSelectorWithFilter
                  placeholder="VariableSelectorWithFilter.placeholder-multiple"
                  :variables.sync="filter.variables"
                  :withAssociatedData="true"
                ></opensilex-VariableSelectorWithFilter>
              </opensilex-FilterField>
            
              <!-- Experiments -->
              <opensilex-FilterField  quarterWidth="true">
                <opensilex-ExperimentSelector
                  label="DataView.filter.experiments"
                  :experiments.sync="filter.experiments"
                  :multiple="true"
                  @select="updateSOFilter"
                  @clear="updateSOFilter"
                ></opensilex-ExperimentSelector>
              </opensilex-FilterField> 

              <opensilex-FilterField quarterWidth="true">
                <!-- Start Date -->
                <opensilex-DateTimeForm
                    :value.sync="filter.start_date"
                    label="component.common.begin"
                    name="startDate"
                    :max-date="filter.end_date ? filter.end_date : undefined"                
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <opensilex-FilterField quarterWidth="true">
                <!-- End Date -->
                <opensilex-DateTimeForm
                    :value.sync="filter.end_date"
                    label="component.common.end"
                    name="endDate"
                    :min-date="filter.start_date ? filter.start_date : undefined"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>
              <!-- Scientific objects -->
              <opensilex-FilterField halfWidth="true">
                <opensilex-SelectForm
                  ref="soSelector"
                  label="DataView.filter.scientificObjects"
                  placeholder="DataView.filter.scientificObjects-placeholder"
                  :selected.sync="filter.scientificObjects"
                  modalComponent="opensilex-ScientificObjectModalList"
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

               <!-- targets -->
              <opensilex-FilterField halfWidth="true">
                <opensilex-TagInputForm
                  class="overflow-auto"
                  style="height: 90px"
                  :value.sync="filter.targets"
                  label="DataView.filter.targets"
                  helpMessage="DataView.filter.targets-help"
                  type="text"
                ></opensilex-TagInputForm>
              </opensilex-FilterField>
            

              <!-- Provenance -->
              <opensilex-FilterField >
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
                ></opensilex-DataProvenanceSelector>

                <b-collapse
                  v-if="selectedProvenance"
                  id="collapse-4"
                  v-model="visibleDetails"
                  class="mt-2"
                >
                  <opensilex-ProvenanceDetails
                    :provenance="getSelectedProv"
                  ></opensilex-ProvenanceDetails>
                </b-collapse>
              </opensilex-FilterField>
          </template>
        </opensilex-SearchFilterField>
    
        <opensilex-DataList 
          ref="dataList"
          :listFilter.sync="filter">
        </opensilex-DataList>
        
      </template>
    </opensilex-PageContent>

    <opensilex-ResultModalView ref="resultModal" @onHide="refreshDataAfterImportation()"> </opensilex-ResultModalView>

    <opensilex-DataProvenanceModalView 
      ref="dataProvenanceModalView"        
    ></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO } from "opensilex-core/index";
import {ScientificObjectNodeDTO} from "opensilex-core/model/scientificObjectNodeDTO";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class DataView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  visibleDetails: boolean = false;
  selectedProvenance: any = null;
  filterProvenanceLabel: string = null;
  refreshKey = 0;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("templateForm") readonly templateForm!: any;
  @Ref("dataList") readonly dataList!: any;
  @Ref("modalDataForm") readonly modalDataForm!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("resultModal") readonly resultModal!: any;
  @Ref("soSelector") readonly soSelector!: any;
  @Ref("varSelector") readonly varSelector!: any;

  filter = {
    start_date: null,
    end_date: null,
    variables: [],
    provenance: null,
    experiments: [],
    scientificObjects: [],
    targets: []
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


  refreshSoSelector() {

    this.soSelector.refreshModalSearch();
    this.refreshComponent();
  }

  refreshComponent(){
    this.refreshKey += 1
  }

  resetFilter() {
    this.filter = {
      start_date: null,
      end_date: null,
      variables: [],
      provenance: null,
      experiments: [],
      scientificObjects: [],
      targets: []
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
    this.service = this.$opensilex.getService("opensilex.DataService");
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
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.selectedProvenance = prov;
      });
    }
  }

  afterCreateData(results) {
    if(results instanceof Promise){
      results.then((res) => {
        this.resultModal.setNbLinesImported(
          res.validation.dataErrors.nbLinesImported
        );
        this.resultModal.setProvenance(res.form.provenance);
        this.resultModal.show();
        this.refreshKey += 1;
        this.clear();
        this.filter.provenance = res.form.provenance.uri;
      });
    }else{ 
      this.resultModal.setNbLinesImported(
        results.validation.dataErrors.nbLinesImported
      );
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

  refreshDataAfterImportation(){
    this.refresh();
  }
  
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DataView:
    buttons:
      create-data : Add data
      generate-template : Generate template
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
      experiments:  Experiment(s)
      variables: Variable(s)
      scientificObjects: Scientific object(s)
      scientificObjects-placeholder: Select scientific objects
      provenance: Provenance
      targets: Target(s)
      targets-help: Copy target's URI here

fr:
  DataView:
    buttons:
      create-data : Ajouter un jeu de données
      generate-template : Générer un gabarit
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
      experiments:  Expérimentation(s)
      variables: Variable(s)
      scientificObjects: Objet(s) scientifique(s)
      scientificObjects-placeholder: Sélectionner des objets scientifiques
      provenance: Provenance  
      targets: Cible(s)
      targets-help: Copier les URI des cibles ici    
  
</i18n>
