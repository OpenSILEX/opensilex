<template>
  <opensilex-SelectForm
    :key="lang"
    ref="selectForm"
    :label="label"
    :selected.sync="germplasmURI"
    :multiple="multiple"
    :required="required"
    :searchMethod="loadOptions"
    :conversionMethod="convertGermplasmDTO"
    :placeholder="placeholder"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { SecurityService } from "opensilex-security/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
// @ts-ignore
import { GermplasmGetSingleDTO } from "opensilex-core/index";

@Component
export default class GermplasmSelector extends Vue {
  $opensilex: any;
  $store: any;

  service: SecurityService;

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

  get lang() {
    return this.$store.getters.language;
  }

  @Ref("selectForm") readonly selectForm!: any;

  loadOptions(query, page, pageSize) {
    return this.$opensilex
      .getService("opensilex.GermplasmService")
      .searchGermplasm(
        undefined, //uri?: string
        undefined, //type?: string
        query, // name: string
        undefined, //code?: string
        undefined, //productionYear?: number
        undefined, //species?: string
        undefined, //variety?: string
        undefined, //accession?: string
        undefined, //institute?: string
        this.experiment || undefined, //experiment?: string
        undefined, //metadata?: string
        [], //orderBy?: Array<string>
        page, //page?: number
        pageSize //pageSize?
      );
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