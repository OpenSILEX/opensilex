<template>
  <opensilex-SelectForm
      ref="soSelector"
      modalComponent="opensilex-ScientificObjectModalListByExp"
      :label="label"
      :placeholder="placeholder"
      :selected.sync="scientificObjectsURI"
      :filter.sync="soFilter"
      :multiple="true"
      :maximumSelectedItems="maximumSelectedRows"
      :clearable="true"
      :required="required"
      :isModalSearch="true"
      :limit="1"
      @clear="reset"
      @onValidate="refreshProvComponent"
      @onClose="refreshProvComponent"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import { NamedResourceDTO, ScientificObjectsService} from "opensilex-core/index";

/**
* Selector of Scientific Objects present in an experiment.
  Used for graphical data visualization of chosen experiment.
  SebastienP
*/

@Component
export default class UsedScientificObjectSelector extends Vue {
  $opensilex: any;
  soService: ScientificObjectsService;

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
  isModalSearch;
  @Prop()
  modalComponent;
  @Prop()
  soFilter;
  @Prop()
  maximumSelectedRows: number;

  @Ref("soSelector") readonly soSelector!: any;

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
}
</script>

<style scoped lang="scss">
</style>
