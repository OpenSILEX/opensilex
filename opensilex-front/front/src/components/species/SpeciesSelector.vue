<template>
  <opensilex-FormSelector
    ref="formSelector"
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
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Watch, Ref} from "vue-property-decorator";
import Vue from "vue";
import {SecurityService} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {SpeciesDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SpeciesService} from "opensilex-core/api/species.service";
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import FormSelector from "../common/forms/FormSelector.vue";

@Component
export default class SpeciesSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;

  service: SecurityService;

  refreshKey = 0;

  @PropSync("selected")
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

  @Ref("formSelector") readonly formSelector!: FormSelector;

  private tutorialLabels: Record<string, string> = {};
  
  private getTutorialSpeciesOptions(): SpeciesDTO[] {
    const uris = this.multiple ? (this.speciesURI || []) : (this.speciesURI ? [this.speciesURI] : []);
    const tutorialUris = (uris as any[]).map(String).filter(u => u.startsWith("__tutorial__:"));

    return tutorialUris.map(uri => ({
      uri,
      name: this.tutorialLabels[uri] || ""
    } as any));
  }


  setSelectedNode(node: { id: string; label: string }) {
    this.tutorialLabels[node.id] = node.label;
    this.formSelector.select(node);
    this.refreshKey += 1;
  }

  clearSelection() {
    if ((this.formSelector as any).clear) (this.formSelector as any).clear();
    else this.speciesURI = this.multiple ? [] : undefined;
  }

  clearTutorialState() {
    this.tutorialLabels = {};
    this.refresh()
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  refresh() {
    this.refreshKey += 1;
  }

  async loadSpecies(): Promise<Array<SpeciesDTO>> {
    const tutorialOptions = this.getTutorialSpeciesOptions();

    let baseOptions: Array<SpeciesDTO>;
    if (!this.experimentURI) {
      const http = await this.$opensilex
        .getService<SpeciesService>("opensilex.SpeciesService")
        .getAllSpecies(this.sharedResourceInstance);
      baseOptions = http.response.result;
    } else {
      const http = await this.$opensilex
        .getService<ExperimentsService>("opensilex.ExperimentsService")
        .getAvailableSpecies(this.experimentURI);
      baseOptions = http.response.result;
    }

    // éviter les doublons d'uri si jamais
    const byUri = new Map<string, SpeciesDTO>();
    for (const s of baseOptions) byUri.set(s.uri, s);
    for (const s of tutorialOptions) byUri.set(s.uri, s);

    return Array.from(byUri.values());
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
