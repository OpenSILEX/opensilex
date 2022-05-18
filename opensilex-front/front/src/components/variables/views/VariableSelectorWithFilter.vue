<template>
  <opensilex-SelectForm
    ref="variableSelector"
    modalComponent="opensilex-VariableModalList"
    :label="label"
    :placeholder="placeholder"
    :selected.sync="variablesURI"
    :experiment="experiment"
    :objects="objects"
    :devices="devices"
    :isModalSearch="true"
    :required="required"
    :multiple="true"
    :maximumSelectedItems="maximumSelectedRows"
    :withAssociatedData="withAssociatedData"
    :limit="1"
    @clear="refreshVariableSelector"
    @select="select"
    @deselect="deselect"
    @onValidate="onValidate"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { NamedResourceDTO, VariableDetailsDTO } from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse"

@Component
export default class VariableSelectorWithFilter extends Vue {
  $opensilex: any;

  @Ref("variableSelector") readonly variableSelector!: any;

  @PropSync("variables")
  variablesURI;

  @Prop()
  placeholder;

  @Prop({default: "DataView.filter.variables"})
  label;

  @Prop()
  required;

  @Prop()
  withAssociatedData: boolean;

  @Prop()
  experiment;

  @Prop()
  objects;

  @Prop()
  devices;

  @Prop()
  maximumSelectedRows;

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onValidate(value) {
    this.$emit("validate", value);
  }

  refreshVariableSelector() {
    this.variableSelector.refreshModalSearch();
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  VariableSelectorWithFilter:
    placeholder : Select a variable
    placeholder-multiple : Select one or more variables
    filter-search-no-result : No variable found

fr:
  VariableSelectorWithFilter:
    placeholder : Sélectionner une variable
    placeholder-multiple : Sélectionner une ou plusieurs variables
    filter-search-no-result : Aucune variable trouvée

</i18n>