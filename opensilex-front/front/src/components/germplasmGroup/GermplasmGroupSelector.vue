<template>
  <opensilex-SelectForm
      ref="selectForm"
      :label="label"
      :selected.sync="groupURI"
      :multiple="multiple"
      :searchMethod="searchGermplasmGroups"
      :itemLoadingMethod="loadGermplasmGroups"
      :clearable="clearable"
      :placeholder="placeholder"
      noResultsText="component.groupGermplasm.form.selector.filter-search-no-result"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
      @keyup.enter.native="onEnter"
      @loadMoreItems="loadMoreItems"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {GermplasmGroupGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import SelectForm from "../common/forms/SelectForm.vue";

@Component
export default class GermplasmGroupSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  pageSize = 10;

  @PropSync("germplasmGroup")
  groupURI: string;

  @Prop()
  label: string;

  @Prop()
  multiple: string;

  @Prop()
  clearable: string;

  @Ref("selectForm") readonly selectForm!: SelectForm;

  get placeholder() {
    return this.multiple
        ? "component.groupGermplasm.form.selector.placeholder-multiple"
        : "component.groupGermplasm.form.selector.placeholder";
  }

  loadGermplasmGroups(group): Promise<Array<GermplasmGroupGetDTO>> {
    return this.$opensilex.getService<GermplasmService>("opensilex.GermplasmService")
        .getGermplasmGroupByURIs([group])
        .then((http: HttpResponse<OpenSilexResponse<Array<GermplasmGroupGetDTO>>>) => {
          return http.response.result;
        })
        .catch(this.$opensilex.errorHandler);
  }

  searchGermplasmGroups(name): Promise<HttpResponse<OpenSilexResponse<Array<GermplasmGroupGetDTO>>>> {
    return this.$opensilex.getService<GermplasmService>("opensilex.GermplasmService")
        .searchGermplasmGroups(name, undefined, ["name=asc"], 0, this.pageSize)
        .then((http: HttpResponse<OpenSilexResponse<Array<GermplasmGroupGetDTO>>>) => {
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
    this.selectForm.refresh();
    this.$nextTick(() => {
      this.selectForm.openTreeselect();
    })
  }
}
</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component:
    groupGermplasm:
      form:
        selector:
          placeholder : Select one germplasm group
          placeholder-multiple : Select one or more germplasm group
          filter-search-no-result : No germplasm group found


fr:
  component:
    groupGermplasm:
      form:
        selector:
          placeholder : Sélectionner un groupe de ressources génétiques
          placeholder-multiple : Sélectionner un ou plusieurs groupes de ressources génétiques
          filter-search-no-result : Aucun groupe de ressources génétiques trouvé

</i18n>
