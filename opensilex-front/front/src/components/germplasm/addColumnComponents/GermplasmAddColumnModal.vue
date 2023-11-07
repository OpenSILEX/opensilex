<template>
  <b-modal
      ref="colModal"
      hide-footer
      :title="$t('GermplasmTable.addColumn')"
      size="md"
  >
    <ValidationObserver ref="validatorRef">
        <!-- Existing property -->
        <opensilex-GermplasmControlledAttributesSelector
            ref="controlledAttributeSelector"
            :property.sync="chosenPropertyUri"
            :existingRdfAttributes="existingRdfAttributesObjects"
            @select="selectedExistingProperty()"
            @clear="clearedExistingPropertyField()"
        ></opensilex-GermplasmControlledAttributesSelector>
        <!-- Non existing property -->
        <opensilex-InputForm
            ref="uncontrolledAttributeInput"
            :key="inputFormKey"
            :value.sync="uncontrolledColName"
            :disabled="pickedExisting"
            :rules="existingRdfAttributesStringRule"
            label="GermplasmAddColumnModal.nonExistingAttributeFieldLabel"
            type="text"
            :required="false"
        ></opensilex-InputForm>
        <b-button class="mt-3 btn greenThemeColor" variant="primary" block @click="checkValidationAndAddColumn">{{
            $t("GermplasmAddColumnModal.addColumn")
          }}</b-button>

<!--      </form>-->
    </ValidationObserver>
  </b-modal>
</template>


<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop, Ref} from 'vue-property-decorator';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import { SelectableItem } from 'src/components/common/forms/SelectForm.vue';
import InputForm from "@/components/common/forms/InputForm.vue";
import GermplasmControlledAttributesSelector from './GermplasmControlledAttributesSelector.vue';

@Component({})
/**
 * Modal that pops up when the user hits the add column button.
 */
export default class GermplasmAddColumnModal extends Vue {

  @Ref("colModal") readonly colModal!: any;
  @Ref("uncontrolledAttributeInput") readonly uncontrolledAttributeInput!: InputForm;
  @Ref("controlledAttributeSelector") readonly controlledAttributeSelector!: GermplasmControlledAttributesSelector;
  @Ref("validatorRef") readonly validatorRef!: any;

  inputFormKey = 0;

  $opensilex: OpenSilexVuePlugin;

  uncontrolledColName: string = null;

  chosenPropertyUri: string = "";

  @Prop()
  existingRdfAttributesObjects: Array<SelectableItem>;

  @Prop()
  existingRdfAttributesStringRule:string;

  pickedExisting: boolean = false;


  /**
   * @Pre The uri choice and labels were previously got together so we can assume that the filtered list will always have size 1
   */
  getColumnNameForExistingPropertyUri(): string{
    let filteredByUri: Array<SelectableItem> = this.existingRdfAttributesObjects.filter(selectableItem => this.$opensilex.checkURIs(selectableItem.id, this.chosenPropertyUri));
    return filteredByUri[0].label;
  }

  async checkValidationAndAddColumn(){
    let isValid: boolean = await this.validatorRef.validate();
    if(!this.chosenPropertyUri){
      if(isValid){
        this.$emit('addingUncontrolledColumn', this.uncontrolledColName);
      }
    }else{
      this.$emit('addingExistingColumn', this.getColumnNameForExistingPropertyUri(), this.chosenPropertyUri);
    }
  }

  selectedExistingProperty(){
    this.pickedExisting = true;

  }
  clearedExistingPropertyField(){
    this.pickedExisting = false;
  }

  show(){
    this.colModal.show();
  }
  hide(){
    this.colModal.hide();
  }
}
</script>


<style scoped lang="scss">

</style>

<i18n>

en:
  GermplasmAddColumnModal:
    addColumn: Add column
    nonExistingAttributeFieldLabel: Non existing new column
    thereIsAnExistingErrorMessage: This property already exists.

fr:
  GermplasmAddColumnModal:
    addColumn: Ajouter colonne
    nonExistingAttributeFieldLabel: Nouvelle colonne non existante
    thereIsAnExistingErrorMessage: Cette propriété est déjà existante.
</i18n>