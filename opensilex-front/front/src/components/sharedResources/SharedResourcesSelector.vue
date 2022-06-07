<template>
  <div>
    <opensilex-SelectForm
        :label="label"
        :selected.sync="resourcesURI"
        :multiple="multiple"
        :optionsLoadingMethod="loadSharedResources"
        :conversionMethod="sharedResourcesToSelectNode"
        placeholder="component.sharedResources.selector-placeholder"
        @clear="$emit('clear')"
        @select="select"
        @deselect="deselect"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProjectGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {SharedResourcesDTO} from "opensilex-core/model/sharedResourcesDTO";
import {SpeciesDTO} from "opensilex-core/model/speciesDTO";

@Component
export default class SharedResourcesSelector extends Vue {
  $opensilex: any;

  @PropSync("resources")
  resourcesURI;

  @Prop()
  label;

  @Prop({default: false})
  multiple;

  loadSharedResources() {
    return this.$opensilex
        .getService("opensilex.OntologyService")
        .getAllSharedResources()
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<SharedResourcesDTO>>>) =>
                http.response.result
        );
  }

  sharedResourcesToSelectNode(dto: SharedResourcesDTO) {
    return {
      id: dto.uri,
      label: dto.label
    };
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
