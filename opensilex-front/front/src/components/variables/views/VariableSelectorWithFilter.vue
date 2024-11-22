<template>
  <opensilex-ModalFormSelector
    ref="variableSelector"
    modalComponent="opensilex-VariableModalList"
    :label="label"
    :placeholder="placeholder"
    :selected.sync="variablesURI"
    :selectedInJsonFormat="this.editMode ? variablesAsSelectableItems : null"
    :experiment="experiment"
    :objects="objects"
    :devices="devices"
    :required="required"
    :multiple="true"
    :maximumSelectedItems="maximumSelectedRows"
    :withAssociatedData="withAssociatedData"
    :limit="4"
    @clear="refreshVariableSelector"
    @select="select"
    @deselect="deselect"
    @onValidate="onValidate"
    @hide='$emit("hideSelector")'
    @shown='$emit("shownSelector")'
  ></opensilex-ModalFormSelector>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { NamedResourceDTO, VariableDetailsDTO } from "opensilex-core/index";
import {SelectableItem} from '../../common/forms/ModalFormSelector.vue';
import ModalFormSelector from "../../common/forms/ModalFormSelector.vue";

@Component
export default class VariableSelectorWithFilter extends Vue {
  $opensilex: any;

  @Ref("variableSelector") readonly variableSelector!: ModalFormSelector;

  @PropSync("variables")
  variablesURI;

  //Needed if elements were already present to correctly show their labels
  @PropSync("variablesWithLabels")
  variablesAsSelectableItems: Array<SelectableItem>;

  //To say if some elements can already be present when we open this selector
  @Prop()
  editMode: boolean;

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

  //Function to load in the already present variables if this is the first time we are opening this selector
  setVariableSelectorToFirstTimeOpen(){
    this.variableSelector.setSelectorToFirstTimeOpen();
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