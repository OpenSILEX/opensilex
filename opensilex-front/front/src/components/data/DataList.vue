<template>
  <div>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDataList"
      :fields="fields"
      defaultSortBy="name"
    >

      <template v-slot:export>
        <b-button
          @click="exportModal.show()"
          variant="secondary"
        >export
        </b-button>

        <opensilex-DataExportModal
          ref="exportModal"
          :filter="filter"
        ></opensilex-DataExportModal>
      </template>

      <template v-slot:cell(target)="{ data }">
        <opensilex-UriLink
          :uri="data.item.target"
          :value="objects[data.item.target]"
          :to="{
            path:
              '/scientific-objects/details/' +
              encodeURIComponent(data.item.target),
          }"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(variable)="{ data }">
        <opensilex-UriLink
          :uri="data.item.variable"
          :value="variables[data.item.variable]"
          :to="{
            path: '/variable/details/' + encodeURIComponent(data.item.variable),
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

    <opensilex-DataProvenanceModalView 
      ref="dataProvenanceModalView"        
    ></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class DataList extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  filterProvenanceLabel: string = null;

  @PropSync("listFilter", 
  {
    default: () => {
      return {
        start_date: null,
        end_date: null,
        variables: [],
        provenance: null,
        experiments: [],
        scientificObjects: []
      };
    },
  })
  filter: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("templateForm") readonly templateForm!: any;
  @Ref("tableRef") readonly tableRef!: any;
  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;
  @Ref("exportModal") readonly exportModal!: any;

  get fields() {
    let tableFields: any = [
      {
        key: "target",
        label: "DataView.list.object",
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
    return tableFields;
  }

  refresh() {    
    this.tableRef.refresh();
    this.$nextTick(() => {
      this.$opensilex.updateURLParameters(this.filter);
    });    
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
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

    return new Promise((resolve, reject) => {
      this.service.searchDataList(
        this.$opensilex.prepareGetParameter(this.filter.start_date), // start_date
        this.$opensilex.prepareGetParameter(this.filter.end_date), // end_date
        undefined, // timezone,
        this.filter.experiments, // experiments
        this.filter.scientificObjects, // targets
        this.$opensilex.prepareGetParameter(this.filter.variables), // variables,
        undefined, // devices
        undefined, // min_confidence
        undefined, // max_confidence
        provUris, // provenance
        undefined, // metadata
        options.orderBy, // order_by
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

            let objectURI = http.response.result[i].target;
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
              .getService("opensilex.OntologyService")
              .getURILabelsList(objectsToLoad)
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

<<<<<<< 798e69cb9121a4712e03e533776c5d978881cee9
=======
  exportData(mode: string) {
    let path = "/core/data/export";
    let today = new Date();
    let filename =
      "export_data_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");

    let params = {
      start_date: this.filter.start_date,
      end_date: this.filter.end_date,
      targets: this.filter.scientificObjects,
      experiments: this.filter.experiments,
      variables: this.filter.variables,
      provenances: [this.filter.provenance],
      mode: mode
    }
    this.$opensilex.downloadFilefromService(path, filename, "csv", params);
  }

>>>>>>> update Data interface with new dataModel
}
</script>

<style scoped lang="scss">
</style>
