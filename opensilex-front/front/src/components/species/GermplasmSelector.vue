<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="speciesURI"
    :multiple="multiple"
    :required="required"
    :searchMethod="loadOptions"
    :itemLoadingMethod="loadGermplasms"    
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
import { SpeciesDTO } from "opensilex-core/index";

@Component
export default class GermplasmSelector extends Vue {
  $opensilex: any;
  $store: any;

  service: SecurityService;

  @PropSync("germplasm")
  speciesURI;

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

  @Prop()
  experimentURI;

  @Ref("selectForm") readonly selectForm!: any;

  loadGermplasms(factorsURI) {
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .getFactorsByURI(factorsURI)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) =>
          http.response.result
      )
      .then((result: any[]) => {
        let nodeList = [];
        for (let factor of result) {
          nodeList.push(this.factorToSelectNode(factor));
        }
        return nodeList;
      });
  }

  loadOptions(query, page, pageSize) {

    this.filter.name = query;
    this.filter.uri = query;
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .searchFactors(null, page, pageSize, this.filter)
      .then((http) => {
        let factorMapByCategory = this.sortFactorListByCategory(http.response.result);
        let factorsByCategoryNode = this.transformFactorListByCategoryInNodes(
          factorMapByCategory
        );
        http.response.result = factorsByCategoryNode;
        return http;
      });
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