<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="dimensionsURI"
    :multiple="multiple"
    :searchMethod="searchDimensions"
    :itemLoadingMethod="loadDimensions"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="DimensionSelector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
// @ts-ignore
import {DimensionGetDTO} from "opensilex-core/index";

@Component
export default class DimensionSelector extends Vue {
  $opensilex: any;

  @PropSync("dimensions")
  dimensionsURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  get placeholder() {
    return this.multiple
      ? "DimensionSelector.placeholder-multiple"
      : "DimensionSelector.placeholder";
  }

  loadDimensions(dimensions) {
    return this.$opensilex.getService("opensilex.VariablesService")
      .getDimensionsByURIs(dimensions)
      .then((http: HttpResponse<OpenSilexResponse<DimensionGetDTO>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler); 
  }

  searchDimensions(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchDimensions(name, ["name=asc"], 0, 10)    
    .then((http: HttpResponse<OpenSilexResponse<Array<DimensionGetDTO>>>) => {
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
  DimensionSelector: 
    placeholder : Select one dimension
    placeholder-multiple : Select one or more dimensions
    filter-search-no-result : No dimensions found
    
            
fr:
  DimensionSelector: 
    placeholder : Sélectionner une dimension
    placeholder-multiple : Sélectionner une ou plusieurs dimensions
    filter-search-no-result : Aucune dimension trouvée

</i18n>