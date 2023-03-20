<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="methodURI"
    :multiple="multiple"
    :searchMethod="searchMethods"
    :itemLoadingMethod="loadMethods"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.method.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {MethodGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";

@Component
export default class MethodSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @PropSync("method")
  methodURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  @Prop()
  sharedResourceInstance;

  @Ref("selectForm") readonly selectForm!: any;

  @Watch("sharedResourceInstance")
  onSriChange() {
    this.selectForm.refresh();
  }

  get placeholder() {
    return this.multiple
      ? "component.method.form.selector.placeholder-multiple"
      : "component.method.form.selector.placeholder";
  }

  loadMethods(methods): Promise<Array<MethodGetDTO>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
      .getMethodsByURIs(methods, this.sharedResourceInstance)
      .then((http: HttpResponse<OpenSilexResponse<Array<MethodGetDTO>>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler); 
  }

  searchMethods(name): Promise<HttpResponse<OpenSilexResponse<Array<MethodGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchMethods(name, ["name=asc"], 0, 10, this.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<MethodGetDTO>>>) => {
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
}
</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component: 
    method: 
        form:
          selector:
            placeholder : Select one method
            placeholder-multiple : Select one or more methods
            filter-search-no-result : No methods found
    
            
fr:
  component: 
    method: 
        form: 
          selector:
            placeholder : Sélectionner une méthode
            placeholder-multiple : Sélectionner une ou plusieurs méthodes
            filter-search-no-result : Aucune méthode trouvée

</i18n>
