<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="variablesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadVariables"
    :conversionMethod="variableToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    :required="required"
    :defaultSelectedValue="defaultSelectedValue"
    noResultsText="VariableSelector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import { NamedResourceDTO } from "opensilex-core/index";

@Component
export default class UsedVariableSelector extends Vue {
  $opensilex: any;

  @PropSync("variables")
  variablesURI: any;

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

  @Prop()
  experiment;

  @Prop()
  object;

  @Prop()
  device;

  get placeholder() {
    return this.multiple
      ? "VariableSelector.placeholder-multiple"
      : "VariableSelector.placeholder";
  }

  loadVariables() {
    let experiments = null;    
    if (this.experiment != null) {
      experiments = [this.experiment]
    }

    let objects = null;
    if (this.object != null) {
      objects = [this.object]
    }

    let devices = null;
    if (this.device != null) {
      devices = [this.device]
    }

    return this.$opensilex
      .getService("opensilex.DataService")
      .getUsedVariables(experiments, objects, null, devices)
      .then(http => {
        return http.response.result;
      });
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
