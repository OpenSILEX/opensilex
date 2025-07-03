<template>
    <div>
        <opensilex-DataTableAsyncView
            ref="tableRef"
            :searchMethod="searchDataList"
            :countMethod="countDataList"
            :fields="fields"
            defaultSortBy="date"
            :defaultSortDesc="true"
        >
            <template v-slot:cell(target)="{ data }">
                <opensilex-UriLink
                    :uri="data.item.target"
                    :value="objects[data.item.target]"
                    :to="{
            path: $opensilex.getTargetPath(data.item.target, contextUri, objectsPath[data.item.target])
          }"
                ></opensilex-UriLink>
            </template>

            <template v-slot:cell(variable)="{ data }">
                <opensilex-UriLink
                    :uri="data.item.variable"
                    :value="getVariableName(data.item.variable)"
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
                        @click="showDataDetailsModal(data.item)"
                        label="DataView.list.details"
                        :small="true"
                    ></opensilex-DetailButton>
                </b-button-group>
            </template>

        </opensilex-DataTableAsyncView>

        <opensilex-DataProvenanceModalView
            ref="dataProvenanceModalView"
        ></opensilex-DataProvenanceModalView>
    </div>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ProvenanceGetDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {DataService} from "opensilex-core/api/data.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VariablesService} from "opensilex-core/api/variables.service";
import {BatchHistoryGetDTO} from "opensilex-core/model/batchHistoryGetDTO";
import {DataGetSearchDTO} from "opensilex-core/model/dataGetSearchDTO";
import DataProvenanceModalView from "./DataProvenanceModalView.vue"

@Component
export default class DataList extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    dataService: DataService;
    ontologyService: OntologyService;
    variablesService: VariablesService;
    disabled = false;

    visibleDetails: boolean = false;
    usedVariables: any[] = [];
    selectedProvenance: any = null;
    filterProvenanceLabel: string = null;

    @Prop({
        default: "",
    })
    contextUri: string;

    @PropSync("listFilter",
        {
            default: () => {
                return {
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
                    batch_uri: null
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
    @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: DataProvenanceModalView;
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

    getVariableName(variableUri: string): string {
        return this.variableNames[this.$opensilex.getLongUri(variableUri)];
    }

  refresh() {
    this.$opensilex.updateURLParameters(this.filter);
    this.tableRef.changeCurrentPage(1);
  }

    created() {
        this.dataService = this.$opensilex.getService("opensilex.DataService");
        this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
        this.variablesService = this.$opensilex.getService("opensilex.VariablesService");
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

    loadProvenance(selectedValue) {
        if (selectedValue != undefined) {
            this.$opensilex.getProvenance(selectedValue.id, this.dataService).then((prov) => {
                this.selectedProvenance = prov;
            });
        }
    }

    async showDataDetailsModal(item: DataGetSearchDTO) {
        this.$opensilex.enableLoader();
        try {
            const provenanceSearchResult = await this.$opensilex.getProvenance(item.provenance.uri, this.dataService);
            const batchSearchResult = await this.$opensilex.getBatch(item.batchUri, this.dataService);
            const value = {
                provenance: provenanceSearchResult,
                data: item,
                batch: batchSearchResult
            };
            this.dataProvenanceModalView.setProvenanceAndBatch(value);
            this.dataProvenanceModalView.show();
        } catch (error) {
            console.error("Failed to fetch provenance or Batch:", error);
        }
    }


    objects : {[key : string] : string} = {};
    objectsPath : {[key : string] : string} = {};
    variableNames = {};
    provenances = {};
    devices = {};
    facilities = {};
    operators = {};



   countDataList(options) {
       let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
       if (provUris != undefined) {
           provUris = [provUris];
       }

       return this.dataService.countData(
           // Count data, set limit to  since here we want the exact/total data count according the current filter
           this.$opensilex.prepareGetParameter(this.filter.start_date),
           this.$opensilex.prepareGetParameter(this.filter.end_date),
           undefined,
           this.filter.experiments,
           this.$opensilex.prepareGetParameter(this.filter.variables),
           this.$opensilex.prepareGetParameter(this.filter.devices),
           undefined,
           undefined,
           provUris,
           undefined,
           this.$opensilex.prepareGetParameter(this.filter.operators),
           this.filter.germplasm_group,
           this.filter.germplasm,
           0,
         this.filter.batch_uri,
           [].concat(
               this.filter.scientificObjects,
               this.filter.facilities,
               this.filter.targets) // targets & os & facilities

       )
    }


    searchDataList(options) {
        let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
        if (provUris != undefined) {
            provUris = [provUris];
        }

        return new Promise((resolve, reject) => {
            this.dataService.searchDataListByTargets(
                this.$opensilex.prepareGetParameter(this.filter.start_date),
                this.$opensilex.prepareGetParameter(this.filter.end_date),
                undefined,
                this.filter.experiments,
                this.$opensilex.prepareGetParameter(this.filter.variables),
                this.$opensilex.prepareGetParameter(this.filter.devices),
                undefined,
                undefined,
                provUris,
                undefined,
                this.filter.germplasm_group,
                this.$opensilex.prepareGetParameter(this.filter.operators),
                this.filter.germplasm,
                this.filter.batch_uri,
                options.orderBy,
                options.currentPage,
                options.pageSize,
                [].concat(this.filter.scientificObjects, this.filter.facilities, this.filter.targets) // targets & os & facilities
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
                            promiseArray.push(this.$opensilex.loadOntologyLabelsWithType(objectsToLoad, this.contextUri, this.objects, this.ontologyService));
                        }

                        if (variablesToLoad.length > 0) {
                            let promiseVariable = this.variablesService
                                .getVariablesByURIs(variablesToLoad)
                                .then((httpObj) => {
                                    for (let j in httpObj.response.result) {
                                        let variable = httpObj.response.result[j];
                                        this.variableNames[this.$opensilex.getLongUri(variable.uri)] = variable.name;
                                    }
                                })
                                .catch(reject);
                            promiseArray.push(promiseVariable);
                        }

                        if (provenancesToLoad.length > 0) {
                            let promiseProvenance = this.dataService
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
                            Promise.all([this.loadObjectsPath()]).then((value) => {
                                resolve(http);
                            })
                        });

                    } else {
                        resolve(http);
                    }
                })
                .catch(reject);
        });
    }

    /**
     * Construct paths for each target's UriLink components according to their type.
     */
    private loadObjectsPath(): Promise<unknown> {

        // ensure that at least one object has been loaded (in case where all data in the page have no target)
        let objectURIs = Object.keys(this.objects);
        if (!objectURIs || objectURIs.length == 0) {
            return Promise.resolve();
        }

        return this.ontologyService
            .getURITypes(objectURIs)
            .then((httpObj) => {
                for (let j in httpObj.response.result) {
                    let obj = httpObj.response.result[j];
                    this.objectsPath[obj.uri] =
                        this.$opensilex.getPathFromUriTypes(obj.rdf_types);
                }
            });
    }

}
</script>

<style scoped lang="scss">
.exportButton {
    margin-left: 15px;
}
</style>
