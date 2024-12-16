<template>
  <opensilex-ModalFormSelector
      ref="soSelector"
      modalComponent="opensilex-ScientificObjectModalListByExp"
      :label="label"
      :placeholder="placeholder"
      :selected.sync="scientificObjectsURI"
      :selectedInJsonFormat= "this.mapMode ? scObj : null"
      :filter.sync="soFilter"
      :multiple="true"
      :devices="devices"
      :modalComponentProps="modalComponentProps"
      :maximumSelectedItems="maximumSelectedRows"
      :clearable="true"
      :required="required"
      :limit="1"
      @clear="reset"
      @onValidate="refreshProvComponent"
      @onClose="refreshProvComponent"
  ></opensilex-ModalFormSelector>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import {ScientificObjectsService} from "opensilex-core/index";
import ModalFormSelector, { SelectableItem } from "../../common/forms/ModalFormSelector.vue";

/**
* Selector of Scientific Objects present in an experiment.
  Used for graphical data visualization of chosen experiment.
  SebastienP
*/

@Component
export default class UsedScientificObjectSelector extends Vue {
  $opensilex: any;
  soService: ScientificObjectsService;

  @Prop()
  mapMode: boolean;

  @PropSync("scObjects")
  scObj: Array<SelectableItem>;

  @PropSync("scientificObjects")
  scientificObjectsURI: any;
  @Prop()
  label;
  @Prop()
  multiple;
  @Prop({ default: false })
  required;
  @Prop()
  clearable;
  @Prop()
  placeholder;
  @Prop()
  experiment;
  @Prop()
  objects;
  @Prop()
  modalComponent;
  @Prop()
  soFilter;
  @Prop()
  maximumSelectedRows: number;
  @Prop()
  variables;
  @Prop()
  devices;

  get modalComponentProps(){
    return {variables: this.variables, devices: this.devices}
  }

  @Ref("soSelector") readonly soSelector!: ModalFormSelector;

  refreshKey = 0;

  refreshTypeSelectorComponent(){
    this.refreshKey += 1
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
  
  reset() {
    this.soFilter = {
      name: "",
      experiment: this.experiment,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined
    }
    this.soSelector.refreshModalSearch();
    this.refreshProvComponent([]);
  }

  refreshProvComponent(selection) {
    this.refreshKey += 1;
    this.$emit("onValidate", selection);
  }
  setSoSelectorToFirstTimeOpen(){
    this.soSelector.setSelectorToFirstTimeOpen();
  }
}
</script>

<style scoped lang="scss">
</style>
