<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-target"
      title="component.menu.scientificObjects"
      :description="selected ? selected.name : ''"
    ></opensilex-PageHeader>

    <opensilex-ScientificObjectDetail
      v-if="selected"
      :selected="selected"
      :objectByContext="objectByContext"
      :globalView="true"
      :withReturnButton="true"
      @onUpdate="refresh"
    ></opensilex-ScientificObjectDetail>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectDetailView extends Vue {
  $opensilex: any;

  selected = null;

  objectByContext = [];

  uri;

  created() {
    this.refresh();
  }

  refresh() {
    let service = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

    this.uri = decodeURIComponent(this.$route.params.uri);
    if (this.uri) {
      service.getScientificObjectDetailByExperiments(this.uri).then((http) => {
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
