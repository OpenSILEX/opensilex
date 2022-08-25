<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="interestEntityURI"
    :multiple="multiple"
    :searchMethod="searchInterestEntities"
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
  
  searchInterestEntities(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchInterestEntity(name, ["name=asc"], 0, 10)    
    .then((http: HttpResponse<OpenSilexResponse<Array<InterestEntityGetDTO>>>) => {
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
  component: 
    interestEntity: 
        form:
          selector:
            placeholder : Select one observation level
            placeholder-multiple : Select one or more observation levels
            filter-search-no-result : No observation level found
    
            
fr:
  component: 
    interestEntity: 
        form: 
          selector:
            placeholder : Sélectionner un niveau d'observation
            placeholder-multiple : Sélectionner un ou plusieurs niveaux d'observation
            filter-search-no-result : Aucun niveau d'observation trouvé

</i18n>