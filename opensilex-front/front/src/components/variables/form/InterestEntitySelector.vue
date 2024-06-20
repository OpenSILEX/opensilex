<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    :selected.sync="interestEntityURI"
    :multiple="multiple"
    :searchMethod="searchInterestEntities"
    :itemLoadingMethod="loadInterestEntities"
    :placeholder="placeholder"
    noResultsText="component.interestEntity.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
    @loadMoreItems="loadMoreItems"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {InterestEntityGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import FormSelector from "../../common/forms/FormSelector.vue";

@Component
export default class InterestEntitySelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  pageSize = 10;

  @PropSync("interestEntity")
  interestEntityURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  sharedResourceInstance;

  @Ref("formSelector") readonly formSelector!: FormSelector;

  @Watch("sharedResourceInstance")
  onSriChange() {
    this.formSelector.refresh();
  }

  get placeholder() {
    return this.multiple
      ? "component.interestEntity.form.selector.placeholder-multiple"
      : "component.interestEntity.form.selector.placeholder";
  }

  loadInterestEntities(interestEntities): Promise<Array<InterestEntityGetDTO>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
      .getInterestEntitiesByURIs(interestEntities, this.sharedResourceInstance)
      .then((http: HttpResponse<OpenSilexResponse<Array<InterestEntityGetDTO>>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  searchInterestEntities(name): Promise<HttpResponse<OpenSilexResponse<Array<InterestEntityGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchInterestEntity(name, ["name=asc"], 0, this.pageSize, this.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<InterestEntityGetDTO>>>) => {
        return http;
    });
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
    this.formSelector.refresh();
    this.$nextTick(() => {
      this.formSelector.openTreeselect();
    })
  }
}
</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component: 
    interestEntity: 
        form:
          selector:
            placeholder : Select one entity of interest
            placeholder-multiple : Select one or more entities of interest
            filter-search-no-result : No entities of interest found
    
            
fr:
  component: 
    interestEntity: 
        form: 
          selector:
            placeholder : Sélectionner une entité d'intérêt
            placeholder-multiple : Sélectionner une ou plusieurs entités d'intérêt
            filter-search-no-result : Aucune entité d'intérêt trouvée

</i18n>
