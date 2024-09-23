<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    :selected.sync="characteristicURI"
    :multiple="multiple"
    :searchMethod="searchCharacteristics"
    :itemLoadingMethod="loadCharacteristics"
    :placeholder="placeholder"
    :actionHandler="actionHandler"
    :required="required"
    :helpMessage="helpMessage"
    :conversionMethod="objectToSelectNode"
    noResultsText="component.characteristic.form.selector.filter-search-no-result"
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
import {CharacteristicGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import FormSelector from "../../common/forms/FormSelector.vue";

@Component
export default class CharacteristicSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  currentPage: number = 0;
  pageSize = 10;
  page = 0;

  @PropSync("selected")
  characteristicURI;

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

  @Prop()
  conversionMethod;

  @Ref("formSelector") readonly formSelector!: FormSelector;

  @Watch("sharedResourceInstance")
  onSriChange() {
    this.formSelector.refresh();
  }

  get placeholder() {
    return this.multiple
      ? "component.characteristic.form.selector.placeholder-multiple"
      : "component.characteristic.form.selector.placeholder";
  }

  loadCharacteristics(characteristics): Promise<Array<CharacteristicGetDTO>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
      .getCharacteristicsByURIs(characteristics, this.sharedResourceInstance)
      .then((http: HttpResponse<OpenSilexResponse<Array<CharacteristicGetDTO>>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler)
  }

  searchCharacteristics(name, page, pageSize): Promise<HttpResponse<OpenSilexResponse<Array<CharacteristicGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchCharacteristics(name, ["name=asc"], page, pageSize, this.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<CharacteristicGetDTO>>>) => {
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
    characteristic: 
        form:
          selector:
            placeholder : Select one characteristic
            placeholder-multiple : Select one or more characteristics
            filter-search-no-result : No characteristics found
    
            
fr:
  component: 
    characteristic: 
        form: 
          selector:
            placeholder : Sélectionner une caractéristique
            placeholder-multiple : Sélectionner une ou plusieurs caractéristiques
            filter-search-no-result : Aucune caractéristique trouvée

</i18n>
