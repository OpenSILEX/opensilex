<template>
  <div>
    <opensilex-GeneticResourceSelector
        label="GeneticResourceParentsModalFormFieldLine.parent-fieldLabel"
        :multiple="false"
        :geneticResource.sync="lineData.geneticResource_uri"
        :required="false"
        @select="loadGeneticResourceInformation($event)"
        class="col-md-7"
    ></opensilex-GeneticResourceSelector>
    <opensilex-GeneticResourceControlledAttributesSelector
        :property.sync="lineData.parent_type_uri"
        :existingRdfAttributes="extraProps.existingRdfParentProperties"
        :required="false"
        @select="updateParentType"
        class="col-md-4"
    ></opensilex-GeneticResourceControlledAttributesSelector>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import { GeneticResourceParentsAttributesUsedInFront } from './GeneticResourceParentsModalFormField.vue';
import {SelectableItem} from "../common/forms/FormSelector.vue"

@Component({})
export default class GeneticResourceParentsModalFormFieldLine extends Vue {

  @Prop()
  lineData: GeneticResourceParentsAttributesUsedInFront;

  @Prop()
  lineIndex: number;

  @Prop()
  extraProps: any;

  loadGeneticResourceInformation(idAndLabelJson : SelectableItem){
    let newLine = JSON.parse(JSON.stringify(this.lineData));
    newLine.geneticResource_label = idAndLabelJson.label;
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
  GeneticResourceParentsModalFormFieldLine:
    parent-fieldLabel: Parent

fr:
  GeneticResourceParentsModalFormFieldLine:
    parent-fieldLabel: Parent

</i18n>