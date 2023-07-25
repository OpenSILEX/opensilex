<template>
  <opensilex-SelectForm
    :label="label"
    :required="required"
    :selected.sync="organizationsURI"
    :multiple="multiple"
    :options="organizationsOptions"
    placeholder="OrganizationTree.filter-placeholder"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import { ResourceDagDTO } from 'opensilex-core/index';

@Component
export default class OrganizationSelector extends Vue {
  $opensilex: any;

  @PropSync("organizations")
  organizationsURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  excludeOrganizationURI: string;

  @Prop({
    default: false
  })
  required: boolean;

  organizationsOptions = [];
  mounted() {
    this.init();
  }

  init() {
    this.$opensilex
      .getService("opensilex-core.OrganizationsService")
      .searchOrganizations()
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceDagDTO>>>) => {
        this.organizationsOptions = this.$opensilex.buildTreeFromDag(
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
