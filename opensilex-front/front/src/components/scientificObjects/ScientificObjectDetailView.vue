<template>
  <div class="container-fluid">

    <PageHeader
      icon="ik#ik-target"
      description="component.menu.scientificObjects"
      :title="selected ? selected.name : ''"
      class="detail-element-header"
    ></PageHeader>

    <ScientificObjectDetail
      v-if="selected"
      :selected="selected"
      :objectByContext="objectByContext"
      :globalView="true"
      :withReturnButton="true"
      :scientificObjectURI="uri"
      :experiment="experiment"
      icon="ik#ik-target"
      @onUpdate="refresh"
      @tabChanged="onForcedTabChange"
    ></ScientificObjectDetail>
  </div>
</template>

<script setup lang="ts">
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";
import ScientificObjectDetail from "./ScientificObjectDetail.vue"
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { ScientificObjectDetailByExperimentsDTO } from 'opensilex-core/index';
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import {ScientificObjectDetailDTO} from "opensilex-core/model/scientificObjectDetailDTO";
import {ExperimentGetDTO} from "opensilex-core/model/experimentGetDTO";
import {computed, inject, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import PageHeader from "@/components/layout/PageHeader.vue";

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service : ScientificObjectsService = $opensilex.getService("opensilex.ScientificObjectsService");
const xpService : ExperimentsService = $opensilex.getService("opensilex.ExperimentsService");
const $route = useRoute();
const $router = useRouter();
//#endregion

//#region Reactive data
const selected = ref<ScientificObjectDetailByExperimentsDTO>(null);
const objectByContext = ref<ScientificObjectDetailByExperimentsDTO[]>([]);
const uri = ref<string>(null);
const experiment = ref<string>(null);
//#endregion

//#region Hooks
onMounted(() => {
  console.log($router.getRoutes());
  refresh();
})
//#endregion

//#region Event handlers
/**
 * Handles a tab change event. Does this by receiving the route name and using that to call router.replace
 */
function onForcedTabChange(correspondingRouteName: string) {

  if(!correspondingRouteName){
    return;
  }

  $router.replace({
    name: 'ScientificObjectDetails',
    params: {
      uri: uri.value,
      experiment: experiment.value
    }
  });

  //TODO MAX delete if all good
  /*let path = this.pathTabMap.find(pathTab => pathTab.tab === tab).path;
  let pathWithUri = path + encodeURIComponent(this.uri);

  // append experiment if defined
  // only handle it the case of the details tab, indeed for other tab, the :experiment path is not defined in global routes
  if(this.experiment && tab === ScientificObjectDetail.DETAILS_TAB){
    pathWithUri += "/" + encodeURIComponent(this.experiment);
  }

  history.pushState({}, null, this.$router.resolve({path: pathWithUri}).href);*/
}
//#endregion

//#region Functions
function refresh() {
  if(! $route.params.uri){
    return;
  }
  uri.value = decodeURIComponent($route.params.uri as string);

  // handle the experiment in which object is viewed
  if($route.params.experiment){

    // check that params.experiment is defined and not empty, before calling decodeURIComponent
    // since decodeURIComponent(undefined) return "undefined"
    experiment.value = decodeURIComponent($route.params.experiment as string);
    getObjectFromExperiment(uri.value, experiment.value);
  }else{
    getObjectFromAllExperiments(uri.value);
  }
}

/**
 * Fills the selected and objectByContext ref values from an OS URI and an XP URI
 * Does this by calling the getScientificObjectDetail and getExperiment webservices and by placing the result in the badly named
 * ScientificObjectDetailByExperimentsDTO , which is basically an OS DTO but with a singular experiment URI and experiment name.
 */
function getObjectFromExperiment(objectUri: string, experimentUri: string){

  // #TODO MAX this was an old to do, see if it can easily be done: fetch OS and experiment in one API call (less HTTP I/O)

  // Perform two network call for OS and experiment
  Promise.all([
    service.getScientificObjectDetail(objectUri, experimentUri),
    xpService.getExperiment(experimentUri)
  ]).then((result => {

    // get OS and XP detail
    let objectDto: ScientificObjectDetailDTO = result[0].response.result;
    let xpDto: ExperimentGetDTO = result[1].response.result;

    // merge the OS part inside a new dto
    let objectByXpDto: ScientificObjectDetailByExperimentsDTO = Object.assign({}, objectDto);
    objectByXpDto.experiment = xpDto.uri;
    objectByXpDto.experiment_name = xpDto.name;

    selected.value = objectByXpDto;
    objectByContext.value.push(objectByXpDto);

  })).catch($opensilex.errorHandler);
}

/**
 * Fills the selected and objectByContext ref values from an OS URI
 * Does this by calling the getScientificObjectDetailByExperiments webservice and by placing the results in a list of the badly named
 * ScientificObjectDetailByExperimentsDTO , which is basically an OS DTO but with a singular experiment URI and experiment name.
 */
function getObjectFromAllExperiments(objectUri: string){
  service.getScientificObjectDetailByExperiments(objectUri).then((http) => {
    objectByContext.value = [];
    if (http.response.result.length == 1) {
      selected.value = http.response.result[0];
    } else {
      http.response.result.forEach((scientificObject) => {
        if (scientificObject.experiment !== null) {
          if (scientificObject.experiment.includes("set/scientific-object")) {
            scientificObject.relations = [];
            selected.value = scientificObject;
          } else {
            objectByContext.value.push(scientificObject);
          }
        }
      });
    }
  }).catch($opensilex.errorHandler);
}
//#endregion

</script>

<style scoped lang="scss">
</style>

