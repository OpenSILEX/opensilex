<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="variablesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadExperimentVariables"
    :conversionMethod="experimentToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.experiment.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";
import { ExperimentsService, NamedResourceDTO } from "opensilex-core/index";

@Component
export default class ExperimentVariableSelector extends Vue {
  $opensilex: any;

  @PropSync("experiment")
  experimentURI: string;

  @PropSync("variables")
  variablesURI: string;

  @Prop({
    default: "component.experiment.experiment",
  })
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  filterLabel = "";

  get placeholder() {
    return this.multiple
      ? "component.experiment.form.selector.variables.placeholder-multiple"
      : "component.experiment.form.selector.variables.placeholder";
  }

  loadExperimentVariables(options) {
    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getUsedVariables(this.experimentURI)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) =>
          http.response.result
      );
  }

  experimentToSelectNode(dto: NamedResourceDTO) {
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
              placeholder-multiple : Select one or several variables
              filter-search-no-result : No variable found
    
            
fr:
  component: 
    experiment: 
        form: 
          selector:
            placeholder : Selectionner une variable
            placeholder-multiple : Selectionner une ou plusieurs variables
            filter-search-no-result : Aucune variable trouvée

</i18n>