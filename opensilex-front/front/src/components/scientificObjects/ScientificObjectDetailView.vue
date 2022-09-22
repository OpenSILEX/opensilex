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
      icon="ik#ik-target"
      @onUpdate="refresh"
    ></opensilex-ScientificObjectDetail>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";

@Component
export default class ScientificObjectDetailView extends Vue {
  $opensilex: any;

  selected = null;

  objectByContext = [];

  uri;

  service: ScientificObjectsService;

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
}
</script>

<style scoped lang="scss">
</style>


<i18n>
</i18n>
