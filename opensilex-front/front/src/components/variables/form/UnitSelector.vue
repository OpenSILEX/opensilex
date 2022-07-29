<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="unitURI"
    :multiple="multiple"
    :searchMethod="searchUnits"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.unit.form.selector.filter-search-no-result"
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
import {UnitGetDTO} from "opensilex-core/index";

@Component
export default class UnitSelector extends Vue {
  $opensilex: any;

  @PropSync("unit")
  unitURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  get placeholder() {
    return this.multiple
      ? "component.unit.form.selector.placeholder-multiple"
      : "component.unit.form.selector.placeholder";
  }

  searchUnits(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchUnits(name, ["name=asc"], 0, 10)    
    .then((http: HttpResponse<OpenSilexResponse<Array<UnitGetDTO>>>) => {
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
    unit: 
        form:
          selector:
            placeholder : Select one unit
            placeholder-multiple : Select one or more units
            filter-search-no-result : No units found
    
            
fr:
  component: 
    unit: 
        form: 
          selector:
            placeholder : Sélectionner une unité
            placeholder-multiple : Sélectionner une ou plusieurs unités
            filter-search-no-result : Aucune unité trouvée

</i18n>
