<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="categoryString"
    :multiple="false"
    :options="internalCategories"
    placeholder="component.factors.form.placeholder.factors"
    noResultsText="component.user.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import { FactorCategory } from "./FactorCategory";

import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import {
  FactorsService,
  FactorGetDTO,
  FactorSearchDTO
} from "opensilex-core/index";

@Component
export default class FactorCategorySelector extends Vue {
  $opensilex: any;
  $i18n: any;

  @PropSync("category")
  categoryString;

  @Prop({
    default: "component.factors.category"
  })
  label;

  @Prop()
  multiple;

  get internalCategories() {
    let factorCategoriesMap: any = FactorCategory.getFactorCategories();
    let categoryArray: any[] = [];
    for (let category of Object.keys(factorCategoriesMap)) {
      categoryArray.push({
        id: category,
        label: this.$i18n.t(factorCategoriesMap[category])
      });
    }
    return categoryArray;
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
    factors:
        category : Category
        factor : Factors
        form: 
            placeholder:
                factors : Select one category
                filter-search-no-result : No category found    
fr:
  component: 
    factors:
        category : Category
        factor : Facteurs
        form: 
            placeholder:
                factors : Selectionner un catégorie
                filter-search-no-result : Aucun facteur trouvé

</i18n>