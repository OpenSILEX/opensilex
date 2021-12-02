<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="variablesURI"
    :multiple="multiple"
    :searchMethod="searchVariables"
    :itemLoadingMethod="load"
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
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {NamedResourceDTO, VariableDetailsDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse"

@Component
export default class VariableSelector extends Vue {
  $opensilex: any;

  @PropSync("variables")
  variablesURI: string;

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
      ? "VariableSelector.placeholder-multiple"
      : "VariableSelector.placeholder";
  }

  searchVariables(query, page, pageSize) {
    this.filterLabel = query;

    if (this.filterLabel === ".*") {
      this.filterLabel = undefined;
    }

    return this.$opensilex
        .getService("opensilex.VariablesService")
        .searchVariables(
            this.filterLabel,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            ["name=asc"],
            page,
            pageSize
        ).then(http => {
          return http;
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

<i18n>

en:
  VariableSelector:    
    placeholder : Select a variable
    placeholder-multiple : Select one or more variables
    filter-search-no-result : No variable found    
            
fr:
  VariableSelector:
    placeholder : Sélectionner une variable
    placeholder-multiple : Sélectionner une ou plusieurs variables
    filter-search-no-result : Aucune variable trouvée

</i18n>