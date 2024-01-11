<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="vgURI"
    :multiple="multiple"
    :searchMethod="searchVariablesGroups"
    :itemLoadingMethod="loadVariablesGroups"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.groupVariable.form.selector.filter-search-no-result"
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
import {VariablesGroupGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import SelectForm from "../common/forms/SelectForm.vue";


@Component
export default class GroupVariablesSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  pageSize = 10;

  @PropSync("variableGroup", {default: () => []})
  vgURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  @Prop()
  sharedResourceInstance;

  @Ref("selectForm") readonly selectForm!: SelectForm;

  @Watch("sharedResourceInstance")
  onSriChange() {
    this.selectForm.refresh();
  }

  get placeholder() {
    return this.multiple
      ? "component.groupVariable.form.selector.placeholder-multiple"
      : "component.groupVariable.form.selector.placeholder";
  }

  loadVariablesGroups(vgUris): Promise<Array<VariablesGroupGetDTO>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
      .getVariablesGroupByURIs(vgUris, this.sharedResourceInstance)
      .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  searchVariablesGroups(searchQuery, page, pageSize): Promise<HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchVariablesGroups(searchQuery, undefined, ["name=asc"], page, this.pageSize, this.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => {
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
    groupVariable: 
        form:
          selector:
            placeholder : Select one group of variables
            placeholder-multiple : Select one or more groups of variables
            filter-search-no-result : No groups of variables found
            
fr:
  component: 
    groupVariable: 
        form: 
          selector:
            placeholder : Sélectionner un groupe de variables
            placeholder-multiple : Sélectionner un ou plusieurs groupes de variables
            filter-search-no-result : Aucun groupe de variables trouvé

</i18n>
