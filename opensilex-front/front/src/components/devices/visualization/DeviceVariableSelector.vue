<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="variablesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadOptions"
    :conversionMethod="variableToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    :required="required"
    :defaultSelectedValue="defaultSelectedValue"
    noResultsText="component.experiment.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { NamedResourceDTO } from "opensilex-core/index";

@Component
export default class DeviceVariableSelector extends Vue {
  $opensilex: any;

  @PropSync("variables")
  variablesURI: any;

  @Prop()
  device;

  @Prop()
  label;

  @Prop()
  defaultSelectedValue;

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


  loadOptions() {
    if (this.device) {
      return this.$opensilex
        .getService("opensilex.DevicesService")
        .getDeviceVariables(this.device)
        .then(http => {
          return http.response.result;
        });
    }
  }

  variableToSelectNode(dto: NamedResourceDTO) {
    return {
      id: dto.uri,
      label: dto.name
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
              placeholder : Sélectionner une variable
              placeholder-multiple : Sélectionner une ou plusieurs variables
              filter-search-no-result : Aucune variable trouvée

</i18n>