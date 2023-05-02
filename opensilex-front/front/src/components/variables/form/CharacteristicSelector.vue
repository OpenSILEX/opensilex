<template>
  <opensilex-SelectForm
    ref="selectForm"
    :label="label"
    :selected.sync="characteristicURI"
    :multiple="multiple"
    :searchMethod="searchCharacteristics"
    :itemLoadingMethod="loadCharacteristics"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.characteristic.form.selector.filter-search-no-result"
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
import {CharacteristicGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import SelectForm from "../../common/forms/SelectForm.vue";

@Component
export default class CharacteristicSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @PropSync("characteristic")
  characteristicURI;

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
      ? "component.characteristic.form.selector.placeholder-multiple"
      : "component.characteristic.form.selector.placeholder";
  }

  loadCharacteristics(characteristics): Promise<Array<CharacteristicGetDTO>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
      .getCharacteristicsByURIs(characteristics, this.sharedResourceInstance)
      .then((http: HttpResponse<OpenSilexResponse<Array<CharacteristicGetDTO>>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler); 
  }

  searchCharacteristics(name): Promise<HttpResponse<OpenSilexResponse<Array<CharacteristicGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchCharacteristics(name, ["name=asc"], 0, 10, this.sharedResourceInstance)
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
