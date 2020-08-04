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
import Vue, { PropOptions } from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";
import {
  FactorsService,
  FactorGetDTO,
  FactorSearchDTO,
} from "opensilex-core/index";

import { ASYNC_SEARCH } from "@riophae/vue-treeselect";

import { FactorCategory } from "./FactorCategory";

@Component
export default class FactorSelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @PropSync("factors")
  factorsURI;

  @Prop({
    default: "component.menu.experimentalDesign.factors",
  })
  label;

  @Prop()
  multiple;

  filter: FactorSearchDTO = {
    name: "",
    uri: null,
  };

  factorCategoriesMap: Map<
    string,
    string
  > = FactorCategory.getFactorCategories();

  loadFactors(factorsURI) {
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

  loadOptions(query) {
    console.debug("search :", query);

    this.filter.name = query;
    this.filter.uri = query;
    console.log("filter :", this.filter);
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .searchFactors(null, 0, 10, this.filter)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) =>
          http.response.result
      )
      .then((result) => {
        let factorMapByCategory = this.sortFactorListByCategory(result);
        console.debug("factorMapByCategory : ", factorMapByCategory);
        let factorsByCategoryNode = this.transformFactorListByCategoryInNodes(
          factorMapByCategory
        );
        console.debug("factorsByCategoryNode : ", factorsByCategoryNode);
        return factorsByCategoryNode;
      });
  }

  sortFactorListByCategory(factorList): Map<string, any[]> {
    let factorMapByCategory: Map<string, any[]> = new Map();
    console.debug(factorList.length, factorList);
    for (let index = 0; index < factorList.length; index++) {
      let factor: FactorGetDTO = factorList[index];
      if (factor.category == null) {
        factorMapByCategory[FactorCategory.otherId] = [];
        factorMapByCategory[FactorCategory.otherId].push(factor);
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
        label:
          category == FactorCategory.otherId
            ? this.$i18n.t(FactorCategory.otherLabel)
            : this.$i18n.t(this.factorCategoriesMap[category]),
        isDefaultExpanded: true,
        children: [],
      };
      for (let factor of factors) {
        let factorNode = {
          id: factor.uri,
          label: factor.name + " <" + factor.uri + ">",
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
      label: dto.name + " <" + dto.uri + ">",
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
            placeholder  : Select one or more factors
            filter-search-no-result : No factor found
    
            
fr:
  component: 
    factor: 
        form:
          selector:
            placeholder : Selectionner un ou plusieurs facteurs
            filter-search-no-result : Aucun facteur trouvé

</i18n>