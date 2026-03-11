<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    :selected.sync="methodURI"
    :multiple="multiple"
    :searchMethod="searchMethods"
    :itemLoadingMethod="loadMethods"
    :conversionMethod="conversionMethod"
    :placeholder="placeholder"
    :actionHandler="actionHandler"
    :required="required"
    :helpMessage="helpMessage"
    noResultsText="component.method.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {MethodGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import FormSelector from "../../common/forms/FormSelector.vue";

@Component
export default class MethodSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  pageSize = 10;
  page = 0;

  @PropSync("selected")
  methodURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage;

  @Prop()
  required;

  @Prop()
  actionHandler: Function;

  @Prop()
  conversionMethod;


  @Prop()
  sharedResourceInstance;

  @Ref("formSelector") readonly formSelector!: FormSelector;

  @Watch("sharedResourceInstance")
  onSriChange() {
    this.formSelector.refresh();
  }

  private tutorialLabels: Record<string, string> = {};

  setSelectedNode(node: { id: string; label: string }) {
    this.tutorialLabels[node.id] = node.label;
    this.formSelector.select(node);
  }

  clearSelection() {
    if ((this.formSelector as any).clear) (this.formSelector as any).clear();
    else this.methodURI = this.multiple ? [] : undefined;
  }


  get placeholder() {
    return this.multiple
      ? "component.method.form.selector.placeholder-multiple"
      : "component.method.form.selector.placeholder";
  }

  loadMethods(methods): Promise<Array<MethodGetDTO>> {
    if (Array.isArray(methods) && methods.length === 1 && String(methods[0]).startsWith("__tutorial__:")) {
    const uri = String(methods[0]);
    return Promise.resolve([{ uri, name: this.tutorialLabels[uri] || "" } as any]);
  }

  return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .getMethodsByURIs(methods, this.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<MethodGetDTO>>>) => http.response.result)
    .catch(this.$opensilex.errorHandler);
  }

  searchMethods(name, page, pageSize): Promise<HttpResponse<OpenSilexResponse<Array<MethodGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchMethods(name, ["name=asc"], page, pageSize, this.sharedResourceInstance)
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
