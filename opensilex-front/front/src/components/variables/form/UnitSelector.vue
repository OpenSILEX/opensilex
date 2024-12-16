<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    :selected.sync="unitURI"
    :multiple="multiple"
    :searchMethod="searchUnits"
    :itemLoadingMethod="loadUnits"
    :placeholder="placeholder"
    :helpMessage="helpMessage"
    :actionHandler="actionHandler"
    :required="required"
    noResultsText="component.unit.form.selector.filter-search-no-result"
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
import {UnitGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import FormSelector from "../../common/forms/FormSelector.vue";

@Component
export default class UnitSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  pageSize = 10;
  page = 0;

  @PropSync("selected")
  unitURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage;

  @Prop()
  actionHandler;

  @Prop()
  required;

  @Prop()
  sharedResourceInstance;

  @Ref("formSelector") readonly formSelector!: FormSelector;

  @Watch("sharedResourceInstance")
  onSriChange() {
    this.formSelector.refresh();
  }

  get placeholder() {
    return this.multiple
      ? "component.unit.form.selector.placeholder-multiple"
      : "component.unit.form.selector.placeholder";
  }

  loadUnits(units): Promise<Array<UnitGetDTO>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
      .getUnitsByURIs(units, this.sharedResourceInstance)
      .then((http: HttpResponse<OpenSilexResponse<Array<UnitGetDTO>>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler); 
  }

  searchUnits(name, page, pageSize): Promise<HttpResponse<OpenSilexResponse<Array<UnitGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchUnits(name, ["name=asc"], page, pageSize, this.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<UnitGetDTO>>>) => {
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
    unit: 
        form:
          selector:
            placeholder : Select one unit
            placeholder-multiple : Select one or more units
            filter-search-no-result : No units found
    
            
fr:
  component: 
    unit: 
        form: 
          selector:
            placeholder : Sélectionner une unité
            placeholder-multiple : Sélectionner une ou plusieurs unités
            filter-search-no-result : Aucune unité trouvée

</i18n>
