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
          @click="modalDataForm.showCreateForm()"
          label="OntologyCsvImporter.import"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-ModalForm
      ref="modalDataForm"
      createTitle="DatasetForm.create"
      editTitle="DatasetForm.update"
      component="opensilex-DataImportForm"
      icon="fa#vials"
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
              <opensilex-FilterField>
                <opensilex-UsedVariableSelector
                label="DataView.filter.variables"
                :multiple="true"
                :variables.sync="filter.variables"
                ></opensilex-UsedVariableSelector>
              </opensilex-FilterField>
            
              <!-- Experiments -->
              <opensilex-FilterField>
                <opensilex-ExperimentSelector
                  label="DataView.filter.experiments"
                  :experiments.sync="filter.experiments"
                  :multiple="true"
                  @select="updateSOFilter"
                  @clear="updateSOFilter"
                ></opensilex-ExperimentSelector>
              </opensilex-FilterField> 
            
              <!-- Scientific objects -->
              <opensilex-FilterField halfWidth="true">
                <opensilex-SelectForm
                  ref="soSelector"
                  label="DataView.filter.scientificObjects"
                  placeholder="DataView.filter.scientificObjects-placeholder"
                  :selected.sync="filter.scientificObjects"
                  :conversionMethod="soGetDTOToSelectNode"
                  modalComponent="opensilex-ScientificObjectModalList"
                  :itemLoadingMethod="loadSO"
                  :filter.sync="soFilter"
                  :isModalSearch="true"
                  :clearable="true"
                  :multiple="true"
                  @clear="refreshSoSelector"
                ></opensilex-SelectForm>
              </opensilex-FilterField>

              <opensilex-FilterField>
                <!-- Start Date -->
                <opensilex-DateTimeForm
                    :value.sync="filter.start_date"
                    label="component.common.begin"
                    name="startDate"
                    :max-date="filter.end_date ? filter.end_date : undefined"                
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <opensilex-FilterField>
                <!-- End Date -->
                <opensilex-DateTimeForm
                    :value.sync="filter.end_date"
                    label="component.common.end"
                    name="endDate"
                    :min-date="filter.start_date ? filter.start_date : undefined"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <!-- Provenance -->
              <opensilex-FilterField halfWidth="true">
                <opensilex-UsedProvenanceSelector
                  ref="provSelector"
                  :provenances.sync="filter.provenance"
                  label="ExperimentData.provenance"
                  @select="loadProvenance"
                  :multiple="false"
                  :viewHandler="showProvenanceDetails"
                  :viewHandlerDetailsVisible="visibleDetails"
                  :showURI="false"
                ></opensilex-UsedProvenanceSelector>

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
import { Prop, Component, Ref } from "vue-property-decorator";
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

  filter = {
    start_date: null,
    end_date: null,
    variables: [],
    provenance: null,
    experiments: [],
    scientificObjects: []
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
  }

  resetFilter() {
    this.filter = {
      start_date: null,
      end_date: null,
      variables: [],
      provenance: null,
      experiments: [],
      scientificObjects: []
    };

    this.soSelector.refreshModalSearch();
  }

  updateSOFilter() {
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
          res.validation.dataErrors.nb_lines_imported
        );
        this.resultModal.setProvenance(res.form.provenance);
        this.resultModal.show();
        this.clear();
        this.filter.provenance = res.form.provenance.uri;
        this.provSelector.refresh();
        this.loadProvenance({id:res.form.provenance.uri})
      });
    }else{ 
      this.resultModal.setNbLinesImported(
        results.validation.dataErrors.nb_lines_imported
      );
      this.resultModal.setProvenance(results.form.provenance);
      this.resultModal.show();
      this.clear();
      this.filter.provenance = results.form.provenance.uri;
      this.provSelector.refresh();
      this.loadProvenance({id:results.form.provenance.uri}) 
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
    this.loadProvenance({id: this.filter.provenance})
  }

  loadSO(scientificObjectsURIs) {
      return this.$opensilex.getService("opensilex.ScientificObjectsService")
        .getScientificObjectsListByUris(undefined,scientificObjectsURIs)
        .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
            return (http && http.response) ? http.response.result : undefined
    }).catch(this.$opensilex.errorHandler);
  }

  soGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name
      };
    }
    return null;
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
      object: Object
      provenance: provenance
      details: view data details
    filter:
      label: Search data
      experiments:  Experiment(s)
      variables: Variable(s)
      scientificObjects: scientific object(s)
      scientificObjects-placeholder: Click to select scientific objects
      provenance: Provenance

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
      object: objet
      provenance: provenance
      details: Voir les détails de la donnée
    filter:
      label: Rechercher des données
      experiments:  Expérimentation(s)
      variables: Variable(s)
      scientificObject: Objet(s) scientifique(s)
      scientificObjects-placeholder: cliquer pour sélectionner des objets scientifiques
      provenance: Provenance      
  
</i18n>
