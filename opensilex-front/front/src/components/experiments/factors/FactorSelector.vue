<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="factorsURI"
    :multiple="multiple"
    :searchMethod="loadOptions"
    :itemLoadingMethod="loadFactors"
    placeholder="component.factor.form.selector.placeholder"
    noResultsText="component.factor.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    :disableBranchNodes="true"
    :showCount="true"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import {
  FactorsService,
  FactorGetDTO,
  FactorCategoryGetDTO,
} from "opensilex-core/index";

@Component
export default class FactorSelector extends Vue {
  $opensilex: any;
  $i18n: any;
  service: FactorsService;
  unclassifiedId: string = "component.factor.unclassified";

  @PropSync("factors")
  factorsURI;

  @Prop({
    default: "component.menu.experimentalDesign.factors"
  })
  label;

  @Prop()
  multiple;

  filter: any = {
    name: null,
    uri: null,
  };
  categories: any = {};

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      () => {
        this.searchCategories();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    this.searchCategories();
  }

  searchCategories() {
    this.service
      .searchCategories(undefined, ["name=asc"], undefined, undefined)
      .then(
        (
          http: HttpResponse<OpenSilexResponse<Array<FactorCategoryGetDTO>>>
        ) => {
          if (http && http.response) {
            http.response.result.forEach((categoryDto) => {
              this.categories[categoryDto.uri] = categoryDto.name;
            });
          }
        }
      )
      .catch(this.$opensilex.errorHandler);
  }
  loadFactors(factorsURI) {
    return this.service
      .getFactorsByURIs(factorsURI)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) =>
          http.response.result
      )
      .then((result: FactorGetDTO[]) => {
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
      .searchFactors(
        this.$opensilex.prepareGetParameter(this.filter.name), // name
        undefined, // description
        undefined, // category
        undefined, // experiment
        undefined, // orderBy
        page, // page
        pageSize // pageSize
      )
      .then((http) => {
        let factorMapByCategory = this.sortFactorListByCategory(
          http.response.result
        );
        let factorsByCategoryNode = this.transformFactorListByCategoryInNodes(
          factorMapByCategory
        );
        http.response.size = http.response.result.length;
        http.response.result = factorsByCategoryNode;
        return http;
      });
  }

  sortFactorListByCategory(factorList): Map<string, any[]> {
    let factorMapByCategory: Map<string, any[]> = new Map();
    for (let index = 0; index < factorList.length; index++) {
      let factor: FactorGetDTO = factorList[index];
      if (factor.category == null) {
        factorMapByCategory[this.unclassifiedId] = [];
        factorMapByCategory[this.unclassifiedId].push(factor);
      } else {
        if (!(factor.category in factorMapByCategory)) {
          factorMapByCategory[factor.category] = [];
        }
        factorMapByCategory[factor.category].push(factor);
      }
    }
    return factorMapByCategory;
  }

  transformFactorListByCategoryInNodes(
    factorMapByCategory: Map<string, any[]>
  ) {
    let factorsByCategoryNode = [];

    for (var [category, factors] of Object.entries(factorMapByCategory)) {
      let categoryNode = {
        id: category,
        name:
          category == this.unclassifiedId
            ? this.$i18n.t(this.unclassifiedId)
            : this.categories[category],
        isDefaultExpanded: true,
        children: []
      };
      for (let factor of factors) {
        let factorNode = {
          id: factor.uri,
          name: factor.name + " <" + factor.uri + ">"
        };
        categoryNode.children.push(factorNode);
      }

      factorsByCategoryNode.push(categoryNode);
    }
    return factorsByCategoryNode;
  }

  factorToSelectNode(dto: FactorGetDTO) {
    return {
      id: dto.uri,
      name: dto.name // + " <" + dto.uri + ">"
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
  component: 
    factor: 
        form: 
          selector:
            placeholder: Select one or more factors
            filter-search-no-result: No factor found
        unclassified: Unclassified
            
fr:
  component: 
    factor: 
        form:
          selector:
            placeholder: Sélectionner un ou plusieurs facteurs
            filter-search-no-result: Aucun facteur trouvé
        unclassified: Non classé

</i18n>