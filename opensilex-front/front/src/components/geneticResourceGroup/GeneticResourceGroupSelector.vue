<template>
  <opensilex-FormSelector
      ref="formSelector"
      :label="label"
      :selected.sync="groupURI"
      :multiple="multiple"
      :searchMethod="searchGeneticResourceGroups"
      :itemLoadingMethod="loadGeneticResourceGroups"
      :placeholder="placeholder"
      noResultsText="component.groupGeneticResource.form.selector.filter-search-no-result"
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
import {GeneticResourceGroupGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {GeneticResourceService} from "opensilex-core/api/geneticResource.service";
import FormSelector from "../common/forms/FormSelector.vue";

@Component
export default class GeneticResourceGroupSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  pageSize = 10;

  @PropSync("geneticResourceGroup")
  groupURI: string;

  @Prop()
  label: string;

  @Prop()
  multiple: string;

  @Ref("formSelector") readonly formSelector!: FormSelector;

  get placeholder() {
    return this.multiple
        ? "component.groupGeneticResource.form.selector.placeholder-multiple"
        : "component.groupGeneticResource.form.selector.placeholder";
  }

  loadGeneticResourceGroups(group): Promise<Array<GeneticResourceGroupGetDTO>> {
    return this.$opensilex.getService<GeneticResourceService>("opensilex.GeneticResourceService")
        .getGeneticResourceGroupByURIs([group])
        .then((http: HttpResponse<OpenSilexResponse<Array<GeneticResourceGroupGetDTO>>>) => {
          return http.response.result;
        })
        .catch(this.$opensilex.errorHandler);
  }

  searchGeneticResourceGroups(name): Promise<HttpResponse<OpenSilexResponse<Array<GeneticResourceGroupGetDTO>>>> {
    return this.$opensilex.getService<GeneticResourceService>("opensilex.GeneticResourceService")
        .searchGeneticResourceGroups(name, undefined, ["name=asc"], 0, this.pageSize)
        .then((http: HttpResponse<OpenSilexResponse<Array<GeneticResourceGroupGetDTO>>>) => {
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
    groupGeneticResource:
      form:
        selector:
          placeholder : Select one genetic resource group
          placeholder-multiple : Select one or more genetic resource group
          filter-search-no-result : No genetic resource group found


fr:
  component:
    groupGeneticResource:
      form:
        selector:
          placeholder : Sélectionner un groupe de ressources génétiques
          placeholder-multiple : Sélectionner un ou plusieurs groupes de ressources génétiques
          filter-search-no-result : Aucun groupe de ressources génétiques trouvé

</i18n>
