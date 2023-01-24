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

@Component
export default class ScientificObjectDetailView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  selected = null;

  objectByContext = [];

  uri;

  service: ScientificObjectsService;

  // bind each tab with a path
  pathTabMap: Array<{tab: string, path: string}> = [
    {tab: ScientificObjectDetail.DETAILS_TAB, path: "/scientific-objects/details/"},
    {tab: ScientificObjectDetail.VISUALIZATION_TAB, path: "/scientific-objects/visualization/"},
    {tab: ScientificObjectDetail.DOCUMENTS_TAB, path: "/scientific-objects/documents/"},
    {tab: ScientificObjectDetail.ANNOTATIONS_TAB, path: "/scientific-objects/annotations/"},
    {tab: ScientificObjectDetail.EVENTS_TAB, path: "/scientific-objects/events/"},
    {tab: ScientificObjectDetail.POSITIONS_TAB, path: "/scientific-objects/positions/"},
    {tab: ScientificObjectDetail.DATAFILES_TAB, path: "/scientific-objects/datafiles/"},
  ];

  created() {
    this.service =this.$opensilex.getService("opensilex.ScientificObjectsService");
    this.refresh();
  }

  refresh() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    if (this.uri) {
      this.service.getScientificObjectDetailByExperiments(this.uri).then((http) => {
        this.objectByContext = [];
        if (http.response.result.length == 1) {
          this.selected = http.response.result[0];
        } else {
          http.response.result.forEach((scientificObject) => {
            if (scientificObject.experiment == null) {
              scientificObject.relations = [];
              this.selected = scientificObject;
            } else {
              this.objectByContext.push(scientificObject);
            }
          });
          this.selected.geometry= this.objectByContext[0].geometry;
        }
      });
    }
  }

  // on click on a tab, search for a match by path between "tab" from children component and one of the elements from pathTabMap[]
  // Update the URL using the history.pushState(state, title, url) method that adds an entry to the web
  // browser's session history stack
  onTabChanged(tab: string){
    let path = this.pathTabMap.find(pathTab => pathTab.tab === tab).path;
    history.pushState({}, null, this.$router.resolve({path: path + encodeURIComponent(this.uri)}).href);
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
