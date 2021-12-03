<template>
  <opensilex-SelectForm
    modalComponent="opensilex-VariableModalList"
    noResultsText="VariableSelector.filter-search-no-result"
    :label="label"
    :placeholder="placeholder"
    :selected.sync="variablesURI"
    :conversionMethod="variableToSelectNode"
    :itemLoadingMethod="load"
    :isModalSearch="true"
    :required="required"
    :multiple="multiple"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { NamedResourceDTO, VariableDetailsDTO } from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse"

@Component
export default class VariableSelectorWithFilter extends Vue {
  $opensilex: any;

  @PropSync("variables")
  variablesURI: string;

  @Prop()
  label;

  @Prop()
  multiple; 

  @Prop()
  required;

  get placeholder() {
    return this.multiple
      ? "VariableSelector.placeholder-multiple"
      : "VariableSelector.variables.placeholder";
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

  load(variables) {
    return this.$opensilex
    .getService("opensilex.VariablesService")
    .getVariablesByURIs(variables)
    .then((http: HttpResponse<OpenSilexResponse<Array<VariableDetailsDTO>>>) => {
        return (http && http.response) ? http.response.result : undefined
    }).catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>