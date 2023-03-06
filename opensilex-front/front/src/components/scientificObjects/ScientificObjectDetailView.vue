<template>
    <div class="container-fluid">

        <opensilex-PageHeader
            icon="ik#ik-target"
            description="component.menu.scientificObjects"
            :title="selected ? selected.name : ''"
            class="detail-element-header"
        ></opensilex-PageHeader>

        <opensilex-ScientificObjectDetail
            v-if="selected"
            :selected="selected"
            :objectByContext="objectByContext"
            :globalView="true"
            :withReturnButton="true"
            :scientificObjectURI="uri"
            :defaultTabsValue="defaultTabsValue"
            icon="ik#ik-target"
            @onUpdate="refresh"
            @tabChanged="onTabChanged"
        ></opensilex-ScientificObjectDetail>
    </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";
import ScientificObjectDetail from "./ScientificObjectDetail.vue"
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { ScientificObjectDetailByExperimentsDTO } from 'opensilex-core/index';
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import {ScientificObjectDetailDTO} from "opensilex-core/model/scientificObjectDetailDTO";
import {ExperimentGetDTO} from "opensilex-core/model/experimentGetDTO";

@Component
export default class ScientificObjectDetailView extends Vue {

    $opensilex: OpenSilexVuePlugin;

    selected: ScientificObjectDetailByExperimentsDTO = null;
    objectByContext: Array<ScientificObjectDetailByExperimentsDTO> = [];

    uri: string;
    experiment: string;

    service: ScientificObjectsService;
    xpService: ExperimentsService;

    // bind each tab with a path
    pathTabMap: Array<{ tab: string, path: string }> = [
        {tab: ScientificObjectDetail.DETAILS_TAB, path: "/scientific-objects/details/"},
        {tab: ScientificObjectDetail.VISUALIZATION_TAB, path: "/scientific-objects/visualization/"},
        {tab: ScientificObjectDetail.DOCUMENTS_TAB, path: "/scientific-objects/documents/"},
        {tab: ScientificObjectDetail.ANNOTATIONS_TAB, path: "/scientific-objects/annotations/"},
        {tab: ScientificObjectDetail.EVENTS_TAB, path: "/scientific-objects/events/"},
        {tab: ScientificObjectDetail.POSITIONS_TAB, path: "/scientific-objects/positions/"},
        {tab: ScientificObjectDetail.DATAFILES_TAB, path: "/scientific-objects/datafiles/"},
    ];

    created() {
        this.service = this.$opensilex.getService("opensilex.ScientificObjectsService");
        this.xpService = this.$opensilex.getService("opensilex.ExperimentsService");
        this.refresh();
    }

    refresh() {
        if(! this.$route.params.uri){
            return;
        }
        this.uri = decodeURIComponent(this.$route.params.uri);

        // handle the experiment in which object is viewed
        if(this.$route.params.experiment){

            // check that params.experiment is defined and not empty, before calling decodeURIComponent
            // since decodeURIComponent(undefined) return "undefined"
            this.experiment = decodeURIComponent(this.$route.params.experiment);
            this.getObjectFromExperiment();
        }else{
            this.getObjectFromAllExperiments();
        }
    }

    /**
     * Fetch all relations of the object for one experiment
     */
    getObjectFromExperiment(){

        // #TODO fetch OS and experiment in one API call (less HTTP I/O)

        // Perform two network call for OS and experiment
        Promise.all([
            this.service.getScientificObjectDetail(this.uri,this.experiment),
            this.xpService.getExperiment(this.experiment)
        ]).then((result => {

            // get OS and XP detail
            let objectDto: ScientificObjectDetailDTO = result[0].response.result;
            let xpDto: ExperimentGetDTO = result[1].response.result;

            // merge the OS part inside a new dto
            let objectByXpDto: ScientificObjectDetailByExperimentsDTO = Object.assign({}, objectDto);
            objectByXpDto.experiment = xpDto.uri;
            objectByXpDto.experiment_name = xpDto.name;

            this.selected = objectByXpDto;
            this.objectByContext.push(objectByXpDto);

        })).catch(this.$opensilex.errorHandler);
    }

    /**
     * Fetch all relations of the object for each experiment (including global)
     */
    getObjectFromAllExperiments(){
        this.service.getScientificObjectDetailByExperiments(this.uri).then((http) => {
            this.objectByContext = [];
            if (http.response.result.length == 1) {
                this.selected = http.response.result[0];
            } else {
                http.response.result.forEach((scientificObject) => {
                    if (scientificObject.experiment !== null) {
                        if (scientificObject.experiment.includes("set/scientific-object")) {
                            scientificObject.relations = [];
                            this.selected = scientificObject;
                        } else {
                            this.objectByContext.push(scientificObject);
                        }
                    }
                });
            }
        }).catch(this.$opensilex.errorHandler);
    }

    // on click on a tab, search for a match by path between "tab" from children component and one of the elements from pathTabMap[]
    // Update the URL using the history.pushState(state, title, url) method that adds an entry to the web
    // browser's session history stack
    onTabChanged(tab: string) {

        if(! tab){
            return;
        }

        let path = this.pathTabMap.find(pathTab => pathTab.tab === tab).path;
        let pathWithUri = path + encodeURIComponent(this.uri);

        // append experiment if defined
        // only handle it the case of the details tab, indeed for other tab, the :experiment path is not defined in global routes
        if(this.experiment && tab === ScientificObjectDetail.DETAILS_TAB){
            pathWithUri += "/" + encodeURIComponent(this.experiment);
        }

        history.pushState({}, null, this.$router.resolve({path: pathWithUri}).href);
    }

    // return by default details tab from ScientificObjectDetail
    get defaultTabsValue() {
        return this.pathTabMap.find(pathTab => this.$route.path.startsWith(pathTab.path)).tab;
    }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
</i18n>
