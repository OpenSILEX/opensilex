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

  resourcesList:Array<SharedResourcesDTO>;

  loadSharedResources() {
    return this.$opensilex
        .getService("opensilex.OntologyService")
        .getAllSharedResources()
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<SharedResourcesDTO>>>) => {
              let localResourceDto;
              let defaultSelectedDto;
              for (let resource of http.response.result) {
                if (resource.isLocal) {
                  localResourceDto = resource;
                }
                if (resource.uri === this.resourcesURI) {
                  defaultSelectedDto = resource;
                }
              }
              if (!this.resourcesURI) {
                this.resourcesURI = localResourceDto.uri;
                defaultSelectedDto = localResourceDto;
              }
              this.$emit("loaded", defaultSelectedDto);
              this.resourcesList = http.response.result;
              return http.response.result;
            }
        );
  }

  sharedResourcesToSelectNode(dto: SharedResourcesDTO) {
    return {
      id: dto.uri,
      label: this.$t(dto.label)
    };
  }

  select(value) {
    for (let resourceDTO of this.resourcesList){
      if (resourceDTO.uri === value.id){
        this.$emit("select", resourceDTO);
      }
    }
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
