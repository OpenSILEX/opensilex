<template>
  <opensilex-FormSelector
    ref="experimentSelector"
    :required="required"
    :label="label"
    :selected.sync="experimentsURI"
    :multiple="multiple"
    :searchMethod="searchExperiments"
    :placeholder="placeholder"
    noResultsText="component.experiment.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
// @ts-ignore
import { ExperimentGetListDTO } from "opensilex-core/index";
import FormSelector from "../common/forms/FormSelector.vue";

@Component
export default class ExperimentSelector extends Vue {
  $opensilex: any;
  pageSize = 10;
  page = 0;

  @PropSync("experiments")
  experimentsURI;

  @Prop({
    default: "component.experiment.experiment",
  })
  label;

  @Prop()
  multiple;

  @Prop()
  required;



  @Ref("experimentSelector") readonly experimentSelector!: any;

  get placeholder() {
    return this.multiple
      ? "component.experiment.form.selector.placeholder-multiple"
      : "component.experiment.form.selector.placeholder";
  }

  experimentsByUriCache: Map<string, ExperimentGetListDTO>;

  created() {
    this.experimentsByUriCache = new Map();
  }

  searchExperiments(name, page, pageSize) {
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
        undefined,
        page,
        pageSize
      )
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => {

          if (http && http.response) {
            this.experimentsByUriCache.clear();
            http.response.result.forEach(dto => {
                this.experimentsByUriCache.set(dto.uri, dto);
            })
          }
          return http;
        }
          
      );

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

  loadMoreItems(){
    this.pageSize = 0;
    this.experimentSelector.refresh();
    this.$nextTick(() => {
      this.experimentSelector.openTreeselect();
    })
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
            placeholder-multiple : Select one or more Experiments
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
