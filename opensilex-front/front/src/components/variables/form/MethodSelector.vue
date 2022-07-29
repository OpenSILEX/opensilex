<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="methodURI"
    :multiple="multiple"
    :searchMethod="searchMethods"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.method.form.selector.filter-search-no-result"
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
import {MethodGetDTO} from "opensilex-core/index";

@Component
export default class MethodSelector extends Vue {
  $opensilex: any;

  @PropSync("method")
  methodURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  get placeholder() {
    return this.multiple
      ? "component.method.form.selector.placeholder-multiple"
      : "component.method.form.selector.placeholder";
  }

  searchMethods(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchMethods(name, ["name=asc"], 0, 10)    
    .then((http: HttpResponse<OpenSilexResponse<Array<MethodGetDTO>>>) => {
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
    method: 
        form:
          selector:
            placeholder : Select one method
            placeholder-multiple : Select one or more methods
            filter-search-no-result : No methods found
    
            
fr:
  component: 
    method: 
        form: 
          selector:
            placeholder : Sélectionner une méthode
            placeholder-multiple : Sélectionner une ou plusieurs méthodes
            filter-search-no-result : Aucune méthode trouvée

</i18n>
