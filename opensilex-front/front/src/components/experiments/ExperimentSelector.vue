<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="experimentsURI"
    :multiple="multiple"
    :searchMethod="searchExperiments"
    :itemLoadingMethod="loadExperiments"
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
import Vue from "vue";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
// @ts-ignore
import { ExperimentGetListDTO } from "opensilex-core/index";

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

  experimentsByUriCache: Map<string, ExperimentGetListDTO>;

  created() {
    this.experimentsByUriCache = new Map();
  }

  loadExperiments(experiments) {

    if (!experiments){
        return undefined;
    }

    if(! Array.isArray(experiments)){
        experiments = [experiments];
    }else if(experiments.length == 0){
        return undefined;
    }

    let dtosToReturn = [];

    if (this.experimentsByUriCache.size == 0) {
      experiments.forEach(exp => {
        // if the experiment is an object (and not an uri) with an already filled name and uri, then no need to call service
        if (exp.name && exp.name.length > 0 && exp.uri && exp.uri.length > 0) {
            dtosToReturn.push(exp);
        }
      })

      // if all element to load are objects then just return them
      if (dtosToReturn.length == experiments.length) {
          return dtosToReturn;
      }

    } else {

      // if object have already been loaded, then it's not needed to call the GET{uri} service just for retrieve the object name
      // since the name is returned by the SEARCH service and the result is cached into dtoByUriCache

      experiments.forEach(exp => {
          let loadedDto = this.experimentsByUriCache.get(exp);
          dtosToReturn.push(loadedDto);
      });

      return dtosToReturn;
    }

    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getExperimentsByURIs(experiments)
      .then((http: HttpResponse<OpenSilexResponse<ExperimentGetListDTO>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);   

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
