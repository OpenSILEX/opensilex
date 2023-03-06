<template>
  <opensilex-SelectForm
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
    :key="refreshKey"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Watch} from "vue-property-decorator";
import Vue from "vue";
import {SecurityService} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {SpeciesDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SpeciesService} from "opensilex-core/api/species.service";
import {ExperimentsService} from "opensilex-core/api/experiments.service";

@Component
export default class SpeciesSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;

  service: SecurityService;

  refreshKey = 0;

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

  @Prop()
  sharedResourceInstance;

  @Watch("sharedResourceInstance")
  onResourceChange() {
    this.refreshKey += 1;
  }

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
        .getService<SpeciesService>("opensilex.SpeciesService")
        .getAllSpecies(this.sharedResourceInstance)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) =>
            http.response.result
        );
    } else {
      return this.$opensilex
        .getService<ExperimentsService>("opensilex.ExperimentsService")
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
