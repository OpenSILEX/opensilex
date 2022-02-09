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
import { Component, Prop, PropSync } from "vue-property-decorator";
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
  objects;
  @Prop()
  device;

  get placeholder() {
    return this.multiple
        ? "VariableSelector.placeholder-multiple"
        : "VariableSelector.variables.placeholder";
  }
  loadVariables() {
    let experiments = null;
    if (this.experiment != null) {
      experiments = [this.experiment];
    }
    let objects = null;
    if (this.objects != null) {
      objects = this.objects;
    }
    if (this.device != null) {
      return this.$opensilex.getService("opensilex.DevicesService").getDevice(this.device).then(http => {
        let variables = [];
        if(http.response.result && http.response.result.relations){
          http.response.result.relations.forEach(relation => {
            const measures_prop = this.$opensilex.Oeso.MEASURES_PROP_URI;
            if(relation.property == measures_prop || relation.property == this.$opensilex.Oeso.getShortURI(measures_prop)){ // in case uri is long..
                variables.push(relation.value);
            }
          });
          if(variables.length > 0){
            return this.$opensilex.getService("opensilex.VariablesService").getVariablesByURIs(variables).then(http => {
              return http.response.result;
            });
          } else {
            return variables;
          }
        }
      });
    } else {
      return this.$opensilex
          .getService("opensilex.DataService")
          .getUsedVariables(experiments, objects, null)
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
