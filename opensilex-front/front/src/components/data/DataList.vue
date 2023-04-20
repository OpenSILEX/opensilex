<template>
    <div>
        <opensilex-TableAsyncView
            ref="tableRef"
            :searchMethod="searchDataList"
            :fields="fields"
            defaultSortBy="date"
            :defaultSortDesc="true"
        >
            <template v-slot:cell(target)="{ data }">
                <opensilex-UriLink
                    :uri="data.item.target"
                    :value="objects[data.item.target]"
                    :to="{
            path: getTargetPath(data.item.target)
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
                        @click="showDataProvenanceDetailsModal(data.item)"
                        label="DataView.list.details"
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
import {Prop, Component, Ref, PropSync} from "vue-property-decorator";
import Vue from "vue";
import {ProvenanceGetDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {DataService} from "opensilex-core/api/data.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataGetDTO} from "opensilex-core/model/dataGetDTO";

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
                    operators: []
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

    getVariableName(variableUri: string): string {
        return this.variableNames[this.$opensilex.getLongUri(variableUri)];
    }

    refresh() {
        this.tableRef.refresh();
        this.$nextTick(() => {
            this.$opensilex.updateURLParameters(this.filter);
        });
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

    getProvenance(uri) {
        if (uri != undefined && uri != null) {
            return this.dataService
                .getProvenance(uri)
                .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
                    return http.response.result;
                });
        }
    }

    getTargetPath(uri: string) {
        let defaultOsPath: string = this.objectsPath[uri];
        if(! defaultOsPath){
            return "";
        }

        let osPath = defaultOsPath.replace(':uri', encodeURIComponent(uri))

        // pass encoded experiment inside OS path URL
        if(this.contextUri && this.contextUri.length > 0){
            return osPath.replace(':experiment', encodeURIComponent(this.contextUri));
        }else{ // no experiment passed
            return osPath.replace(':experiment', "");
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
    objectsPath = {};
    variableNames = {};
    provenances = {};
    devices = {};
    facilities = {};
    operators = {};

    searchDataList(options) {
        let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
        if (provUris != undefined) {
            provUris = [provUris];
        }

        return new Promise((resolve, reject) => {
            this.dataService.getDataListByTargets(
                this.$opensilex.prepareGetParameter(this.filter.start_date), // start_date
                this.$opensilex.prepareGetParameter(this.filter.end_date), // end_date
                undefined, // timezone,
                this.filter.experiments, // experiments
                this.$opensilex.prepareGetParameter(this.filter.variables), // variables,
                this.$opensilex.prepareGetParameter(this.filter.devices), // devices
                undefined, // min_confidence
                undefined, // max_confidence
                provUris, // provenance
                undefined, // metadata
                this.$opensilex.prepareGetParameter(this.filter.operators),
                options.orderBy, // order_by
                options.currentPage,
                options.pageSize,
                [].concat(this.filter.scientificObjects, this.filter.facilities, this.filter.targets), // targets & os & facilities
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
                            let promiseObject = this.ontologyService
                                .getURILabelsList(objectsToLoad, this.contextUri)
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

                        let promiseFacility = this.dataService
                            .searchDataList(
                                undefined,
                                undefined,
                                undefined,
                                undefined,
                                [this.filter.facilities]
                            ).then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
                                if (http && http.response){
                                    for (let j in http.response.result){
                                        let facility = http.response.result[j];
                                        this.facilities[facility.uri] = facility.uri;
                                    } }
                            })
                            .catch(reject);
                            promiseArray.push(promiseFacility)
                        

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
    loadObjectsPath() {

        // ensure that at least one object has been loaded (in case where all data in the page have no target)
        let objectURIs = Object.keys(this.objects);
        if (!objectURIs || objectURIs.length == 0) {
            return;
        }

        return this.ontologyService
            .getURITypes(objectURIs)
            .then((httpObj) => {
                for (let j in httpObj.response.result) {
                    let obj = httpObj.response.result[j];
                    this.objectsPath[obj.uri] =
                        this.$opensilex.getPathFromUriTypes(obj.rdfTypes);
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
