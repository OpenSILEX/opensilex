<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="variablesURI"
    :multiple="multiple"
    :searchMethod="loadOptions"
    :conversionMethod="variableToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    :required="required"
    noResultsText="component.experiment.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import { NamedResourceDTO } from "opensilex-core/index";

@Component
export default class VariableSelector extends Vue {
  $opensilex: any;

  @PropSync("variables")
  variablesURI: string;

  @Prop({
    default: "component.experiment.experiment",
  })
  label;

  @Prop()
  multiple;

  @Prop({ default: false })
  required;

  @Prop()
  clearable;

  filterLabel = "";

  get placeholder() {
    return this.multiple
      ? "component.experiment.form.selector.variables.placeholder-multiple"
      : "component.experiment.form.selector.variables.placeholder";
  }

  loadOptions(query, page, pageSize) {
    console.debug(query);
    this.filterLabel = query;
    return this.$opensilex
      .getService("opensilex.VariablesService")
      .searchVariables(this.filterLabel, null, page, pageSize)
      .then((http) => {
        console.log(http);
        return http;
      });
  }

  variableToSelectNode(dto: NamedResourceDTO) {
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
    experiment: 
        form:
          selector:
            variables:
              placeholder : Select one variable
              placeholder-multiple : Select one ou several variables
              filter-search-no-result : No variable found
    
            
fr:
  component: 
    experiment: 
        form: 
          selector:
            variables:
              placeholder : Selectionner une variable
              placeholder-multiple : Selectionner une ou plusieurs variables
              filter-search-no-result : Aucune variable trouvée

</i18n>