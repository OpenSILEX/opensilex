<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="characteristicURI"
    :multiple="multiple"
    :searchMethod="searchCharacteristics"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.characteristic.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
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

  get placeholder() {
    return this.multiple
      ? "component.characteristic.form.selector.placeholder-multiple"
      : "component.characteristic.form.selector.placeholder";
  }

  searchCharacteristics(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchCharacteristics(name, ["name=asc"], 0, 10)    
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
