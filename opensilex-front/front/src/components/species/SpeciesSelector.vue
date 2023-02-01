<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="speciesURI"
    :multiple="multiple"
    :required="required"
    :optionsLoadingMethod="loadSpecies"
    :conversionMethod="speciesToSelectNode"
    :placeholder="placeholder"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { SecurityService, UserGetDTO } from "opensilex-security/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
// @ts-ignore
import { SpeciesDTO } from "opensilex-core/index";

@Component
export default class SpeciesSelector extends Vue {
  $opensilex: any;
  $store: any;

  service: SecurityService;

  @PropSync("species")
  speciesURI;

  @Prop({
    default: "component.experiment.species"
  })
  label;

  @Prop({
    default: "component.experiment.form.placeholder.species"
  })
  placeholder;

  @Prop()
  required;

  @Prop()
  multiple;

  @Prop()
  experimentURI;

  @Ref("selectForm") readonly selectForm!: any;

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadSpecies();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  loadSpecies() {
    if (!this.experimentURI) {
      return this.$opensilex
        .getService("opensilex.SpeciesService")
        .getAllSpecies()
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) =>
            http.response.result
        );
    } else {
      return this.$opensilex
        .getService("opensilex.ExperimentsService")
        .getAvailableSpecies(this.experimentURI)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) =>
            http.response.result
        );
    }
  }

  speciesToSelectNode(dto: SpeciesDTO) {
    return {
      id: dto.uri,
      label: dto.name
    };
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }
}
</script>

<i18n>
en:
  SpeciesSelector:
    select-multiple-placeholder: "Select species"
    select-multiple: "Species"
fr:
  SpeciesSelector:
    select-multiple-placeholder: "Sélectionner des espèces"
    select-multiple: "Espèces"
</i18n>

<style scoped lang="scss">
</style>
