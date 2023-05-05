<template>
  <opensilex-SelectForm
    :key="lang"
    ref="selectForm"
    :label="label"
    :selected.sync="germplasmURI"
    :multiple="multiple"
    :required="required"
    :searchMethod="search"
    :itemLoadingMethod="load"
    :conversionMethod="convertGermplasmDTO"
    :placeholder="placeholder"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {GermplasmGetAllDTO} from "opensilex-core/model/germplasmGetAllDTO";
import SelectForm from "../common/forms/SelectForm.vue";

@Component
export default class GermplasmSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;

  $service: GermplasmService;

  @PropSync("germplasm")
  germplasmURI;

  @Prop({
    default: "GermplasmView.title",
  })
  label;

  @Prop({
    default: "GermplasmSelector.placeholder",
  })
  placeholder;

  @Prop()
  required;

  @Prop()
  multiple;

  @Prop()
  experiment;

  created() {
    this.$service = this.$opensilex.getService("opensilex.GermplasmService");
  }

  get lang() {
    return this.$store.getters.language;
  }

  @Ref("selectForm") readonly selectForm!: SelectForm;

  search(query, page, pageSize) {
    return this.$service
      .searchGermplasm(
        undefined, //uri?: string
        undefined, //type?: string
        query, // name: string
        undefined, //code?: string
        undefined, //productionYear?: number
        undefined, //species?: string
        undefined, //variety?: string
        undefined, //accession?: string
        undefined, //GermplasmGroup?: String
        undefined, //institute?: string
        this.experiment || undefined, //experiment?: string
        undefined, //metadata?: string
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
        .getGermplasmsByURI(uris)
        .then((http: HttpResponse<OpenSilexResponse<Array<GermplasmGetAllDTO>>>) => {
          return http.response.result;
        }).catch(this.$opensilex.errorHandler);
  }

  convertGermplasmDTO(germplasm) {
    let label = germplasm.name + " (" + germplasm.rdf_type_name;
    if (germplasm.species != null) {
      label += " - " + germplasm.species_name;
    }
    label += ")";

    return {
      id: germplasm.uri,
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
  GermplasmSelector:
    placeholder: Select a germplasm

fr:
  GermplasmSelector:
    placeholder: Sélectionner une resource génétique

</i18n>