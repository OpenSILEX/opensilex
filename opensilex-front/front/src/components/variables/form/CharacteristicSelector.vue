<template>
  <opensilex-SelectForm
      ref="selectForm"
      :label="label"
    :selected.sync="characteristicURI"
    :multiple="multiple"
    :searchMethod="searchCharacteristics"
    :itemLoadingMethod="loadCharacteristics"
    :conversionMethod="characteristicToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.characteristic.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
// @ts-ignore
import {CharacteristicGetDTO} from "opensilex-core/index";

@Component
export default class CharacteristicSelector extends Vue {
  $opensilex: any;

  @PropSync("characteristic")
  characteristicURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  @Prop()
  resource;

  @Ref("selectForm") readonly selectForm!: any;

  @Watch("resource")
  onResourceChange() {
    this.selectForm.refresh();
  }

  get placeholder() {
    return this.multiple
      ? "component.characteristic.form.selector.placeholder-multiple"
      : "component.characteristic.form.selector.placeholder";
  }

  loadCharacteristics(characteristics) {
    return this.$opensilex.getService("opensilex.VariablesService")
      .getCharacteristicsByURIs(characteristics,(this.resource === "http://localhost") ? undefined : this.resource)
      .then((http: HttpResponse<OpenSilexResponse<CharacteristicGetDTO>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler); 
  }

  searchCharacteristics(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchCharacteristics(name, ["name=asc"], (this.resource === "http://localhost") ? undefined : this.resource, 0, 10)
    .then((http: HttpResponse<OpenSilexResponse<Array<CharacteristicGetDTO>>>) => {
        return http;
    });
  }

  characteristicToSelectNode(dto) {
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
