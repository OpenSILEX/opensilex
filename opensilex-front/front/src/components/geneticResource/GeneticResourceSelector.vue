<template>
  <opensilex-FormSelector
    :key="lang"
    ref="formSelector"
    :label="label"
    :selected.sync="geneticResourceURI"
    :multiple="multiple"
    :required="required"
    :searchMethod="search"
    :itemLoadingMethod="load"
    :conversionMethod="convertGeneticResourceDTO"
    :placeholder="placeholder"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {GeneticResourceService} from "opensilex-core/api/geneticResource.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {GeneticResourceGetAllDTO} from "opensilex-core/model/geneticResourceGetAllDTO";
import FormSelector from "../common/forms/FormSelector.vue";

@Component
export default class GeneticResourceSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;

  $service: GeneticResourceService;

  @PropSync("geneticResource")
  geneticResourceURI;

  @Prop({
    default: "GeneticResourceView.title",
  })
  label;

  @Prop({
    default: "GeneticResourceSelector.placeholder",
  })
  placeholder;

  @Prop()
  required;

  @Prop()
  multiple;

  @Prop()
  experiment;

  created() {
    this.$service = this.$opensilex.getService("opensilex.GeneticResourceService");
  }

  get lang() {
    return this.$store.getters.language;
  }

  @Ref("formSelector") readonly formSelector!: FormSelector;

  search(query, page, pageSize) {
    return this.$service
      .searchGeneticResource(
        undefined, //uri?: string
        undefined, //type?: string
        query, // name: string
        undefined, //code?: string
        undefined, //productionYear?: number
        undefined, //species?: string
        undefined, //variety?: string
        undefined, //accession?: string
        undefined, //GeneticResourceGroup?: String
        undefined, //institute?: string
        this.experiment || undefined, //experiment?: string
        [],
        [],
        [],
        undefined, //metadata?: string
        undefined, //is_public
        [], //orderBy?: Array<string>
        page, //page?: number
        pageSize //pageSize?
      );
  }

  /**
   * Method to load initial options
   *
   * @param uris URIs of initial options to load
   */
  load(uris: Array<string>) {
    return this.$service
        .getGeneticResourcesByURI(uris)
        .then((http: HttpResponse<OpenSilexResponse<Array<GeneticResourceGetAllDTO>>>) => {
          return http.response.result;
        }).catch(this.$opensilex.errorHandler);
  }

  convertGeneticResourceDTO(geneticResource) {
    let label = geneticResource.name + " (" + geneticResource.rdf_type_name;
    if (geneticResource.species != null) {
      label += " - " + geneticResource.species_name;
    }
    label += ")";

    return {
      id: geneticResource.uri,
      label: label,
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

<style scoped lang="scss">
</style>

<i18n>
en:
  GeneticResourceSelector:
    placeholder: Select a genetic resource

fr:
  GeneticResourceSelector:
    placeholder: Sélectionner une resource génétique

</i18n>