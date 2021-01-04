<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="germplasmURI"
    :multiple="multiple"
    :required="required"
    :searchMethod="loadOptions"
    :itemLoadingMethod="loadGermplasms"
    :conversionMethod="convertGermplasmDTO"
    :placeholder="placeholder"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import { SpeciesDTO, GermplasmGetSingleDTO } from "opensilex-core/index";

@Component
export default class GermplasmSelector extends Vue {
  $opensilex: any;
  $store: any;

  service: SecurityService;

  @PropSync("germplasm")
  germplasmURI;

  @Prop({
    default: "GermplasmView.title"
  })
  label;

  @Prop({
    default: "GermplasmSelector.placeholder"
  })
  placeholder;

  @Prop()
  required;

  @Prop()
  multiple;

  @Ref("selectForm") readonly selectForm!: any;

  loadGermplasms(germplasmURIs) {
    return this.$opensilex
      .getService("opensilex.GermplasmService")
      .getGermplasmsByURI(germplasmURIs)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<GermplasmGetSingleDTO>>>) =>
          http.response.result
      )
  }

  loadOptions(query, page, pageSize) {
    return this.$opensilex
      .getService("opensilex.GermplasmService")
      .searchGermplasmList(
        undefined, //uri?: string
        undefined, //type?: string
        query, // name: string
        undefined, //species?: string
        undefined, //variety?: string
        undefined, //accession?: string
        undefined, //institute?: string
        undefined, //productionYear?: number
        undefined, //experiment?: string
        undefined, //orderBy?: Array<string>
        page, //page?: number
        pageSize //pageSize?
      )
  }

  convertGermplasmDTO(germplasm) {
    let label = germplasm.name + " (" + germplasm.typeLabel;
    if (germplasm.species != null) {
      label += " - " + germplasm.speciesLabel;
    }
    label += ")";

    return {
      id: germplasm.uri,
      label: label
    }
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