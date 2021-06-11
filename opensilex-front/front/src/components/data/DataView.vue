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

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="DataView.filter.label"
      :showTitle="false"
    >
      <template v-slot:filters>

          <!-- Variables -->
          <opensilex-FilterField>
            <opensilex-VariableSelector
            label="DataView.filter.variables"
            :multiple="true"
            :variables.sync="filter.variables"
            ></opensilex-VariableSelector>
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
              :searchModalFilter="soFilter"
              :isModalSearch="true"
              :clearable="false"
            ></opensilex-SelectForm>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <!-- Start Date -->
            <opensilex-DateTimeForm
                :value.sync="filter.start_date"
                label="component.common.begin"
                name="startDate"
            ></opensilex-DateTimeForm>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <!-- End Date -->
            <opensilex-DateTimeForm
                :isTime="true"
                :value.sync="filter.end_date"
                label="component.common.end"
                name="endDate"
            ></opensilex-DateTimeForm>
          </opensilex-FilterField>

          <!-- Provenance -->
          <opensilex-FilterField halfWidth="true">
            <opensilex-ProvenanceSelector
              ref="provSelector"
              :provenances.sync="filter.provenance"
              :filterLabel="filterProvenanceLabel"
              label="ExperimentData.provenance"
              @select="loadProvenance"
              @clear="filterLabel = null"
              :multiple="false"
              :viewHandler="showProvenanceDetails"
              :viewHandlerDetailsVisible="visibleDetails"
              :showURI="false"
            ></opensilex-ProvenanceSelector>
          </opensilex-FilterField>

          <opensilex-FilterField>
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

    <opensilex-PageContent>
      <template v-slot></template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      ref="modalDataForm"
      createTitle="DatasetForm.create"
      editTitle="DatasetForm.update"
      component="opensilex-DataForm"
      icon="fa#vials"
      modalSize="xl"
      @onCreate="afterCreateData"
      :successMessage="successMessage"
    ></opensilex-ModalForm>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDataList"
      :fields="fields"
      defaultSortBy="name"
    >

      <template v-slot:export>
        <b-dropdown
          dropup
          :small="false"
          :text="$t('ExperimentData.export')"
        >
          <b-dropdown-item-button @click="exportData('long')">
            {{ $t("ExperimentData.export-long") }}
            <opensilex-FormInputLabelHelper
              :helpMessage="$t('ExperimentData.export-long-help')"
            >
            </opensilex-FormInputLabelHelper>
          </b-dropdown-item-button>
          <b-dropdown-item-button @click="exportData('wide')"
            >{{ $t("ExperimentData.export-wide") }}
            <opensilex-FormInputLabelHelper
              :helpMessage="$t('ExperimentData.export-wide-help')"
            >
            </opensilex-FormInputLabelHelper
          ></b-dropdown-item-button>
        </b-dropdown>
      </template>

      <template v-slot:cell(scientific_object)="{ data }">
          <opensilex-UriLink
            :uri="data.item.scientific_object"
            :value="objects[data.item.scientific_object]"
            :to="{
              path:
                '/scientific-objects/details/' +
                encodeURIComponent(data.item.scientific_object),
            }"
          ></opensilex-UriLink>
      </template>

      <template v-slot:cell(variable)="{ data }">
        <opensilex-UriLink
          :uri="data.item.variable"
          :value="data.item.variable"
          :to="{
            path: '/variable/' + encodeURIComponent(data.item.variable),
          }"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(provenance)="{ data }">
        <opensilex-UriLink
          :uri="data.item.provenance.uri"
          :value="provenances[data.item.provenance.uri]"
          :to="{
            path: '/provenances/details/' +
              encodeURIComponent(data.item.provenance.uri),
          }"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
              @click="showDataProvenanceDetailsModal(data.item)"
              label="DataView.details"
              :small="true"
          ></opensilex-DetailButton>
        </b-button-group>
      </template>

    </opensilex-TableAsyncView>

    <opensilex-ResultModalView ref="resultModal" @onHide="refreshDataAfterImportation()"> </opensilex-ResultModalView>

    <opensilex-DataProvenanceModalView 
      ref="dataProvenanceModalView"        
    ></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import VueI18n from "vue-i18n";
import moment from "moment";
import { ProvenanceGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class DataView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  filterProvenanceLabel: string = null;

  soFilter = {
        name: "",
        experiment: undefined,
        germplasm: undefined,
        factorLevels: [],
        types: [],
        existenceDate: undefined,
        creationDate: undefined,
      };

  @Ref("templateForm") readonly templateForm!: any;

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("tableRef") readonly tableRef!: any;

  @Ref("modalDataForm") readonly modalDataForm!: any;

  @Ref("searchField") readonly searchField!: any;

  @Ref("provSelector") readonly provSelector!: any;

  @Ref("resultModal") readonly resultModal!: any;

  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;
  @Ref("soSelector") readonly soSelector!: any;

  filter = {
    start_date: null,
    end_date: null,
    variables: [],
    provenance: null,
    experiments: [],
    scientificObjects: []
  };

  resetFilter() {
    this.filter = {
      start_date: null,
      end_date: null,
      variables: [],
      provenance: null,
      experiments: [],
      scientificObjects: []
    };
  }

  updateSOFilter() {
    this.soFilter.experiment = this.filter.experiments[0];
    this.soSelector.refreshModalSearch();
  }

  updateFiltersFromURL() {
    let query: any = this.$route.query;
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        if (Array.isArray(this.filter[key])){
          this.filter[key] = decodeURIComponent(query[key]).split(",");
        } else {
          this.filter[key] = decodeURIComponent(query[key]);
        }        
      }
    }
  }  

  updateURLFilters() {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }    
  }


  get user() {
    return this.$store.state.user;
  }

  get fields() {
    let tableFields: any = [
      {
        key: "scientific_object",
        label: "ExperimentData.object",
      },
      {
        key: "date",
        label: "DataView.list.date",
        sortable: true,
      },
      {
        key: "variable",
        label: "DataView.list.variable",
        sortable: true,
      },
      {
        key: "value",
        label: "DataView.list.value",
        sortable: false,
      },
      {
        key: "provenance",
        label: "DataView.list.provenance",
        sortable: false
      },
      {
        key: "actions",
        label: "component.common.actions"
      }
    ];
    // if (!this.noActions) {
    //   tableFields.push({
    //     key: "actions",
    //     label: "component.common.actions",
    //   });
    // }
    return tableFields;
  }

  refresh() {
    this.tableRef.refresh();
  }

  reset() {
    this.resetFilter();
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    let query: any = this.$route.query;

    this.reset();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
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

  showDataProvenanceDetailsModal(item) {
    this.$opensilex.enableLoader();
    this.getProvenance(item.provenance.uri)
    .then(result => {
      let value = {
        provenance: result,
        data: item
      }
      this.dataProvenanceModalView.setProvenance(value);
      this.dataProvenanceModalView.show();
    });    
  }

  objects = {};
  variables = {};
  provenances = {};

  searchDataList(options) {
    let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
    if (provUris != undefined) {
      provUris = [provUris];
    }

    let scientificObjects = [];
    if (this.filter.scientificObjects) {
      for (let i in this.filter.scientificObjects) {
        scientificObjects.push(this.filter.scientificObjects[i].id);
      }
    }

    return new Promise((resolve, reject) => {
      this.service.searchDataList(
        this.$opensilex.prepareGetParameter(this.filter.start_date), // start_date
        this.$opensilex.prepareGetParameter(this.filter.end_date), // end_date
        undefined, // timezone,
        this.filter.experiments, // experiments
        scientificObjects, // scientific_object
        this.$opensilex.prepareGetParameter(this.filter.variables), // variables
        undefined, // min_confidence
        undefined, // max_confidence
        provUris, // provenance
        undefined, // metadata
        undefined, // order_by
        options.currentPage,
        options.pageSize
      )
      .then((http) => {
          
        let promiseArray = [];
        let objectsToLoad = [];
        let variablesToLoad = [];
        let provenancesToLoad = [];

        if (http.response.result.length > 0) {
          for (let i in http.response.result) {

            let objectURI = http.response.result[i].scientific_object;
            if (objectURI != null && !objectsToLoad.includes(objectURI)) {
              objectsToLoad.push(objectURI);
            }

            let variableURI = http.response.result[i].variable;
            if (!variablesToLoad.includes(variableURI)) {
              variablesToLoad.push(variableURI);
            }

            let provenanceURI = http.response.result[i].provenance.uri;
            if (!provenancesToLoad.includes(provenanceURI)) {
              provenancesToLoad.push(provenanceURI);
            }
          }
          
          if (objectsToLoad.length > 0) {
            let promiseObject = this.$opensilex
              .getService("opensilex.ScientificObjectsService")
              .getScientificObjectsListByUris(undefined, objectsToLoad)
              .then((httpObj) => {
                for (let j in httpObj.response.result) {
                  let obj = httpObj.response.result[j];
                  this.objects[obj.uri] =
                    obj.name + " (" + obj.rdf_type_name + ")";
                }
              })
              .catch(reject);
            promiseArray.push(promiseObject);
          }

          if (variablesToLoad.length > 0) {
            let promiseVariable = this.$opensilex
              .getService("opensilex.VariablesService")
              .getVariablesByURIs(variablesToLoad)
              .then((httpObj) => {
                for (let j in httpObj.response.result) {
                  let variable = httpObj.response.result[j];
                  this.variables[variable.uri] = variable.name;
                }
              })
              .catch(reject);
            promiseArray.push(promiseVariable);
          }

          if (provenancesToLoad.length > 0) {
            let promiseProvenance = this.$opensilex
              .getService("opensilex.DataService")
              .getProvenancesByURIs(provenancesToLoad)
              .then((httpObj) => {
                for (let j in httpObj.response.result) {
                  let prov = httpObj.response.result[j];
                  this.provenances[prov.uri] = prov.name;
                }
              })
              .catch(reject);
            promiseArray.push(promiseProvenance);
          }
          Promise.all(promiseArray).then((values) => {
            resolve(http);
          });

        } else {
          resolve(http);
        }
      })
      .catch(reject);
    });
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
      provenance: Provenance

      
  
</i18n>
