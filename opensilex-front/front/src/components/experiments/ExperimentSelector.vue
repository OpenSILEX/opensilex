<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="experimentsURI"
    :multiple="multiple"
    :searchMethod="searchExperiments"
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
import { ExperimentsService, ExperimentGetListDTO } from "opensilex-core/index";

@Component
export default class ExperimentSelector extends Vue {
  $opensilex: any;

  @PropSync("experiments")
  experimentsURI;

  @Prop({
    default: "component.experiment.experiment",
  })
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  get placeholder() {
    return this.multiple
      ? "component.experiment.form.selector.placeholder-multiple"
      : "component.experiment.form.selector.placeholder";
  }

  searchExperiments(name) {
    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .searchExperiments(
        name,
        undefined,        
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        10
      )
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => 
          http
      );

  }

  experimentToSelectNode(dto: ExperimentGetListDTO) {
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
            placeholder : Select one experiment
            placeholder-multiple : Select one or several experiments
            filter-search-no-result : No experiment found
    
            
fr:
  component: 
    experiment: 
        form: 
          selector:
            placeholder : Sélectionner une expérimentation
            placeholder-multiple : Sélectionner une ou plusieurs expérimentations
            filter-search-no-result : Aucune expérimentation trouvée

</i18n>