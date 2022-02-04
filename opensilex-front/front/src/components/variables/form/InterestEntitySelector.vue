<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="interestEntityURI"
    :multiple="multiple"
    :searchMethod="searchInterestEntities"
    :itemLoadingMethod="loadInterestEntities"
    :conversionMethod="interestEntityToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.interestEntity.form.selector.filter-search-no-result"
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
import {InterestEntityGetDTO} from "opensilex-core/index";

@Component
export default class InterestEntitySelector extends Vue {
  $opensilex: any;

  @PropSync("interestEntity")
  interestEntityURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  get placeholder() {
    return this.multiple
      ? "component.interestEntity.form.selector.placeholder-multiple"
      : "component.interestEntity.form.selector.placeholder";
  }

  loadInterestEntities(interestEntities) {
    return this.$opensilex.getService("opensilex.VariablesService")
      .getInterestEntitiesByURIs(interestEntities)
      .then((http: HttpResponse<OpenSilexResponse<InterestEntityGetDTO>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  searchInterestEntities(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchInterestEntity(name, ["name=asc"], 0, 10)    
    .then((http: HttpResponse<OpenSilexResponse<Array<InterestEntityGetDTO>>>) => {
        return http;
    });
  }

  interestEntityToSelectNode(dto) {
    return {
      id: dto.uri,
      label: dto.name,
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
    interestEntity: 
        form:
          selector:
            placeholder : Select one entity of interest
            placeholder-multiple : Select one or more entities of interest
            filter-search-no-result : No entities of interest found
    
            
fr:
  component: 
    interestEntity: 
        form: 
          selector:
            placeholder : Sélectionner une entité d'intérêt
            placeholder-multiple : Sélectionner une ou plusieurs entités d'intérêt
            filter-search-no-result : Aucune entité d'intérêt trouvée

</i18n>
