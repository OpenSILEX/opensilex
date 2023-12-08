<template>
  <div>
    <opensilex-GermplasmSelector
        label="GermplasmParentsModalFormFieldLine.parent-fieldLabel"
        :multiple="false"
        :germplasm.sync="lineData.germplasm_uri"
        :required="true"
        @select="loadGermplasmInformation($event)"
        class="col-md-7"
    ></opensilex-GermplasmSelector>
    <opensilex-GermplasmControlledAttributesSelector
        :property.sync="lineData.parent_type_uri"
        :existingRdfAttributes="extraProps.existingRdfParentProperties"
        :required="true"
        @select="updateParentType"
        class="col-md-4"
    ></opensilex-GermplasmControlledAttributesSelector>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import { GermplasmParentsAttributesUsedInFront } from './GermplasmParentsModalFormField.vue';
import {SelectableItem} from "../common/forms/SelectForm.vue"

@Component({})
export default class GermplasmParentsModalFormFieldLine extends Vue {

  @Prop()
  lineData: GermplasmParentsAttributesUsedInFront;

  @Prop()
  lineIndex: number;

  @Prop()
  extraProps: any;

  loadGermplasmInformation(idAndLabelJson : SelectableItem){
    let newLine = JSON.parse(JSON.stringify(this.lineData));
    newLine.germplasm_label = idAndLabelJson.label;
    this.$emit("updateLine", newLine, this.lineIndex);
  }

  updateParentType(){
    let newLine = JSON.parse(JSON.stringify(this.lineData));
    this.$emit("updateLine", newLine, this.lineIndex);
  }

}
</script>

<style scoped lang="scss">

</style>

<i18n>

en:
  GermplasmParentsModalFormFieldLine:
    parent-fieldLabel: Parent

fr:
  GermplasmParentsModalFormFieldLine:
    parent-fieldLabel: Parent

</i18n>