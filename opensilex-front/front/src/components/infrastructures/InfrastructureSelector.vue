<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="infrastructuresURI"
    :multiple="multiple"
    :options="infrastructuresOptions"
    placeholder="InfrastructureTree.filter-placeholder"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {ResourceDagDTO} from "opensilex-core/model/resourceDagDTO";

@Component
export default class InfrastructureSelector extends Vue {
  $opensilex: any;

  @PropSync("infrastructures")
  infrastructuresURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  excludeInfrastructureURI: string;

  infrastructuresOptions = [];
  mounted() {
    this.init();
  }

  init() {
    this.$opensilex
      .getService("opensilex-core.OrganizationsService")
      .searchInfrastructures()
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceDagDTO>>>) => {
        this.infrastructuresOptions = this.$opensilex.buildTreeFromDag(
          http.response.result
        );
      })
      .catch(this.$opensilex.errorHandler);
  }

  reset() {
    this.init();
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
