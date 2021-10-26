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
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
// @ts-ignore
import { ResourceTreeDTO } from "opensilex-core/index";

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
    this.$opensilex
      .getService("opensilex-core.OrganisationsService")
      .searchInfrastructures()
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        this.infrastructuresOptions = this.$opensilex.buildTreeFromDag(
          http.response.result
        );
      })
      .catch(this.$opensilex.errorHandler);
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
