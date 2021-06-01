<template>
  <div class="row">
    <div class="col col-xl-12">
      <opensilex-Card icon>
        <template v-slot:header>
          <opensilex-CreateButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DATA_MODIFICATION_ID) && user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
            @click="dataForm.showCreateForm()"
            label="OntologyCsvImporter.import"
          ></opensilex-CreateButton>
        </template>
        <template v-slot:body>
          <opensilex-SearchFilterField
            ref="searchField"
            :withButton="true"
            label="DataView.filter.label"
            :showTitle="true"
            @search="updateFiltersAndSearch()"
            @clear="clear()"
          >
            <template v-slot:filters>
              <b-row>
                <opensilex-FilterField class="col col-xl-3 col-sm-6 col-12">
                  <!-- Start Date -->
                  <opensilex-DateTimeForm
                      :value.sync="filter.start_date"
                      label="component.common.begin"
                      name="startDate"
                  ></opensilex-DateTimeForm>
                </opensilex-FilterField>

                <opensilex-FilterField class="col col-xl-3 col-sm-6 col-12">
                  <!-- End Date -->
                  <opensilex-DateTimeForm
                      :isTime="true"
                      :value.sync="filter.end_date"
                      label="component.common.end"
                      name="endDate"
                  ></opensilex-DateTimeForm>
                </opensilex-FilterField>

                <opensilex-FilterField :halfWidth="true">
                  <opensilex-SelectForm
                    label="Variable"
                    :multiple="true"
                    :selected.sync="filter.variables"
                    :options="usedVariables"
                    placeholder="component.experiment.form.selector.variables.placeholder-multiple"
                  ></opensilex-SelectForm>
                </opensilex-FilterField>
              </b-row>
              <b-row>
                <opensilex-FilterField :halfWidth="true">
                  <opensilex-ProvenanceSelector
                    ref="provSelector"
                    :provenances.sync="filter.provenance"
                    :filterLabel="filterProvenanceLabel"
                    label="ExperimentData.provenance"
                    @select="loadProvenance"
                    @clear="filterLabel = null"
                    :experiment="uri"
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
              </b-row>
            </template>
          </opensilex-SearchFilterField>
          <opensilex-TableAsyncView
            v-if="searchVisible"
            ref="dataRef"
            :searchMethod="searchData"
            :fields="fields"
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
            <template v-slot:cell(uri)="{ data }">
                <div  v-if="data.item.scientific_objects != null" >
                  <opensilex-UriLink  
                  :uri="data.item.scientific_object"
                  :value="objects[data.item.scientific_object]"
                  :to="{
                    path:
                      '/scientific-objects/details/' +
                      encodeURIComponent(data.item.scientific_object),
                  }"
                ></opensilex-UriLink>
                </div>
                <div v-else  >
               </div>
            </template>

            <template v-slot:cell(provenance)="{ data }">
              {{ provenances[data.item.provenance.uri] }}
            </template>
            <template v-slot:cell(variable)="{ data }">
              <opensilex-UriLink
                :uri="data.item.variable"
                :value="variables[data.item.variable]"
                :to="{
                  path:
                    '/variable/details/' +
                    encodeURIComponent(data.item.variable),
                }"
              ></opensilex-UriLink>
            </template>
            <template v-slot:cell(value)="{ data }">
              <div>{{ data.item.value }}</div>
            </template>
          </opensilex-TableAsyncView>
        </template>
      </opensilex-Card>
    </div>

    <opensilex-ModalForm
      ref="dataForm"
      :initForm="initFormData"
      createTitle="DatasetForm.create"
      editTitle="DatasetForm.update"
      component="opensilex-ExperimentDatasetForm"
      icon="fa#vials"
      modalSize="xl"
      @onCreate="afterCreateData"
      :successMessage="successMessage"
    ></opensilex-ModalForm>

    <opensilex-ResultModalView ref="resultModal" @onHide="refreshDataAfterImportation()"> </opensilex-ResultModalView>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ExperimentData extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;
  uri = null;
  visibleDetails: boolean = false;
  searchVisible: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;

  filterProvenanceLabel: string = null;

  filter = {
    start_date: null,
    end_date: null,
    provenance: null,
    variables: [],
    mode: null,
  };

  @Ref("dataRef") readonly dataRef!: any;

  @Ref("dataForm") readonly dataForm!: any;

  @Ref("searchField") readonly searchField!: any;

  @Ref("provSelector") readonly provSelector!: any;

  @Ref("resultModal") readonly resultModal!: any;

  get credentials() {
    return this.$store.state.credentials;
  }

  get user() {
    return this.$store.state.user;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    let query: any = this.$route.query;

    this.resetFilters();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
  }

  resetFilters() {
    this.filter = {
      provenance: null,
      variables: [],
      mode: null,
      start_date: null,
      end_date: null,
    };
    // Only if search and reset button are use in list
  }

  updateFiltersAndSearch() {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }
    this.refresh();
  }

  successMessage(form) {
    return this.$t("ResultModalView.data-imported");
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }

  refreshDataAfterImportation(){
    this.loadProvenance({id: this.filter.provenance})
    this.updateFiltersAndSearch();
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
        this.refreshVariables();
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
      this.refreshVariables();
      this.provSelector.refresh();
      this.loadProvenance({id:results.form.provenance.uri}) 
    }
    
  }

  initFormData(form) {
    form.experiment = this.uri;
    return form;
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  exportData(mode: string) {
    let path =
      "/core/experiments/" + encodeURIComponent(this.uri) + "/data/export";
    let today = new Date();
    let filename =
      "export_data_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");
    this.filter.mode = mode;
    this.$opensilex.downloadFilefromService(path, filename, "csv", this.filter);
    this.filter.mode = null;
  }

  clear() {
    this.searchVisible = false;
    this.selectedProvenance = null;
    this.resetFilters();
    this.filterProvenanceLabel = null;
  }

  mounted() {
    this.searchVisible = false;
    this.refreshVariables();
  }

  refreshVariables() {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getUsedVariables(this.uri)
      .then((http) => {
        let variables = http.response.result;
        this.usedVariables = [];
        for (let i in variables) {
          let variable = variables[i];
          this.usedVariables.push({
            id: variable.uri,
            label: variable.name,
          });
        }
      });
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

  fields = [
    {
      key: "uri",
      label: "ExperimentData.object",
    },
    {
      key: "date",
      label: "ExperimentData.date",
      sortable: true,
    },
    {
      key: "variable",
      label: "ExperimentData.variable",
      sortable: true,
    },
    {
      key: "value",
      label: "ExperimentData.value",
      sortable: true,
    },
    {
      key: "provenance",
      label: "ExperimentData.provenance",
      sortable: true,
    },
  ];

  objects = {};
  variables = {};
  provenances = {};

  searchData(options) {
    let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
    if (provUris != undefined) {
      provUris = [provUris];
    }
    let varUris = this.$opensilex.prepareGetParameter(this.filter.variables);

    let start_date = this.$opensilex.prepareGetParameter(
      this.filter.start_date
    );
    let end_date = this.$opensilex.prepareGetParameter(this.filter.end_date);

    return new Promise((resolve, reject) => {
      this.$opensilex
        .getService("opensilex.ExperimentsService")
        .searchExperimentDataList(
          this.uri,
          start_date, // start_date
          end_date, // end_date
          undefined, // timezone
          undefined, // objectUri
          varUris, // variableUri
          undefined, // min_confidence
          undefined, // max_confidence
          provUris, // provenance_uri
          undefined, // metadata
          options.orderBy, // order_by
          options.currentPage,
          options.pageSize
        )
        .then((http) => {
          let promiseArray = [];

          let objectToLoad = [];
          let variablesToLoad = [];
          let provenancesToLoad = [];
          if (http.response.result.length > 0) {
            for (let i in http.response.result) {
              let objectURI = http.response.result[i].scientific_object;
              if (objectURI != null && !objectToLoad.includes(objectURI)) {
                objectToLoad.push(objectURI);
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

            if (
              variablesToLoad.length > 0 ||
              provenancesToLoad.length > 0
            ) {
              if (objectToLoad.length > 0) {
                let promiseObject = this.$opensilex
                  .getService("opensilex.ScientificObjectsService")
                  .getScientificObjectsListByUris(this.uri, objectToLoad)
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
                    promiseArray.push(promiseProvenance);
                  })
                  .catch(reject);
              }
              Promise.all(promiseArray).then((values) => {
                resolve(http);
              });
            }
          } else {
            resolve(http);
          }
        })
        .catch(reject);
    });
  }

  refresh() {
    this.searchVisible = true;
    this.searchField.validatorRef.validate().then((isValid) => {
      if (isValid && this.dataRef) {
        this.dataRef.refresh();
      }
    });
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ExperimentData:
        object: Scientific Object
        date: Date
        value: Value
        variable : Variable
        provenance: Provenance
        export : Export
        export-wide : Wide format
        export-wide-help : A given date, provenance, scientific object of an observation represents a row and each variable value is in a specific column.
        export-long : Long format
        export-long-help : Each line represent an observation (Same as the result table)
fr:
    ExperimentData:
        object: Objet Scientifique
        date: Date
        value: Valeur
        variable : Variable
        provenance: Provenance
        export : Exporter
        export-wide : Format large
        export-wide-help : Une date, une provenance, un objet scientifique donné d'une observation représente une ligne et chaque valeur de variable est dans une colonne spécifique.
        export-long : Format long
        export-long-help : Une ligne représente une observation (identique au tableau de résultat)

</i18n>
