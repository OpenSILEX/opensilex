<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="speciesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadSpecies"
    :conversionMethod="speciesToSelectNode"
    placeholder="component.experiment.form.placeholder.species"
    noResultsText="component.user.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import { SpeciesDTO } from "opensilex-core/index";

@Component
export default class SpeciesSelector extends Vue {
  $opensilex: any;
  service: SecurityService;

  @PropSync("species")
  speciesURI;

  @Prop({
    default: "component.experiment.species"
  })
  label;

  @Prop()
  multiple;

  loadSpecies() {
    return this.$opensilex
      .getService("opensilex.SpeciesService")
      .getAllSpecies()
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) =>
          http.response.result
      );
  }

  speciesToSelectNode(dto: SpeciesDTO) {
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
