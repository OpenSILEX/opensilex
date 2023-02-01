<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="vgURI"
    :multiple="multiple"
    :searchMethod="searchVariablesGroups"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.groupVariable.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
// @ts-ignore
import {VariablesGroupGetDTO} from "opensilex-core/index";

@Component
export default class GroupVariablesSelector extends Vue {
  $opensilex: any;

  @PropSync("variableGroup")
  vgURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  get placeholder() {
    return this.multiple
      ? "component.groupVariable.form.selector.placeholder-multiple"
      : "component.groupVariable.form.selector.placeholder";
  }

  searchVariablesGroups(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchVariablesGroups(name, undefined, ["name=asc"], 0, 10)    
    .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => {
        return http;
    });
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }
}
</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component: 
    groupVariable: 
        form:
          selector:
            placeholder : Select one group of variables
            placeholder-multiple : Select one or more groups of variables
            filter-search-no-result : No groups of variables found
    
            
fr:
  component: 
    groupVariable: 
        form: 
          selector:
            placeholder : Sélectionner un groupe de variables
            placeholder-multiple : Sélectionner un ou plusieurs groupes de variables
            filter-search-no-result : Aucun groupe de variables trouvé

</i18n>
