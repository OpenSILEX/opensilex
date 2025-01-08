<template>
  <opensilex-ModalListBuilder
      ref="listBuilderRef"
      :fieldLabel="$t('CriteriaSearchModalCreator.filter-label')"
      :helpMessage="$t('CriteriaSearchModalCreator.help-message')"
      :modalTitle="$t('CriteriaSearchModalCreator.title')"
      :placeholder="$t('CriteriaSearchModalCreator.placeholder')"
      :modalExplanation="$t('CriteriaSearchModalCreator.modal-explanation')"
      :required="required"
      :requiredBlue="requiredBlue"
      :parseSingleLineForTreeselect="parseSingleCriteriaForTreeselect"
      :generateEmptyLine="generateEmptySingleCriteria"
      :filterIncompleteLines="filterIncompleteCriteria"
      :convertLineToOutputObject="convertFrontSingleCriteriaToSingleCriteriaDto"
      @validateList="setOutputList"
      lineComponent="opensilex-CriteriaSearchModalLine"
  >
  </opensilex-ModalListBuilder>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop, PropSync, Ref } from 'vue-property-decorator';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { SelectableItem } from '../common/forms/FormSelector.vue';
import ModalListBuilder from "../common/views/ModalListBuilder.vue";

interface SingleCriteriaDTO {
  variable_uri: string,
  criteria_operator: string,
  value: any
}

export interface SingleCriteriaAttributesUsedInFront {
  variable_uri: string,
  criteria_operator: string,
  value: any,
  datatype: string,
  variable_name: string,
  criteria_symbol: string,
  id: string,
  criteria_rules:string
}

interface CriteriaDTO {
  criteria_list: Array<SingleCriteriaDTO>
}

@Component
export default class CriteriaSearchModalCreator extends Vue {

  @Ref("listBuilderRef") readonly listBuilderRef!: ModalListBuilder<SingleCriteriaAttributesUsedInFront, SingleCriteriaDTO>;

  @Prop({ default: false })
  disabled: boolean;

  @Prop()
  required: boolean;

  @Prop()
  requiredBlue: boolean;

  @PropSync("criteria_dto")
  criteriaDto: CriteriaDTO;

  $opensilex: OpenSilexVuePlugin;
  loading : boolean = false;
  id: string;

  /**
   * Returns a list of single criteria, excluding any that have empty or missing fields.
   * Uses CriteriaOnLastValidate
   */
  filterIncompleteCriteria(criteriaListOnLastValidate: Array<SingleCriteriaAttributesUsedInFront>) : Array<SingleCriteriaAttributesUsedInFront>{
    return criteriaListOnLastValidate.filter((singleCriteria) => {
      return (singleCriteria.criteria_operator) &&
          (singleCriteria.criteria_operator.trim().length > 0) &&
          ((singleCriteria.value) &&
          (singleCriteria.value.trim().length > 0) ||
              singleCriteria.criteria_operator === "NotMeasured"
          ) &&
          (singleCriteria.variable_uri) &&
          (singleCriteria.variable_uri.trim().length > 0);
    });
  }


  parseSingleCriteriaForTreeselect(singleCriteria : SingleCriteriaAttributesUsedInFront) : SelectableItem {
    let stringifyedCriteria = singleCriteria.variable_name + " " + singleCriteria.criteria_symbol + " " + (singleCriteria.value ? singleCriteria.value  : "");
    return {
      id: singleCriteria.id,
      label: stringifyedCriteria
    };
  }

  convertFrontSingleCriteriaToSingleCriteriaDto(frontSingleCriteria : SingleCriteriaAttributesUsedInFront) : SingleCriteriaDTO{
    return {variable_uri: frontSingleCriteria.variable_uri,
      criteria_operator: frontSingleCriteria.criteria_operator,
      value: frontSingleCriteria.value}
  }

  generateEmptySingleCriteria (lineId: number): SingleCriteriaAttributesUsedInFront {
    console.debug("generating empty line...");
    return {
      variable_uri: "",
      criteria_operator: "",
      value: "",
      datatype: "",
      variable_name: "",
      criteria_symbol: "",
      id: lineId.toString(),
      criteria_rules:""
    }
  }

  setOutputList(outputList: Array<SingleCriteriaDTO>){
    //TODO see if this doesnt need to be map
    this.criteriaDto.criteria_list = outputList.map(e=>e);
  }

  created() {
    this.id = this.$opensilex.generateID();

  }

  mounted(){
    this.listBuilderRef.resetLineListWithInitialLabels([this.generateEmptySingleCriteria(0)]);
  }


  /**
   * Used from outside the class by reset all filters buttons
   */
  resetCriteriaListAndSave(){
    this.listBuilderRef.resetCriteriaListAndSave();
  }

}

</script>

<style scoped lang="scss">

</style>

<i18n>

en:
  CriteriaSearchModalCreator:
    title: Criteria Creation
    placeholder: Click to create criteria list
    filter-label: Criteria on data
    help-message: Add an undefined amount of data criteria that the Scientific objects must validate to be counted in the result.
    modal-explanation: Validate all of the following conditions.
fr:
  CriteriaSearchModalCreator:
    title: Création de critère
    placeholder: Cliquez pour créer une liste de critères
    filter-label: Critères par données
    help-message: Ajouter un nombre indéfini de critères sur les données que les Objets scientifiques doivent valider pour être prise en compte dans le résultat.
    modal-explanation : Valider toutes les conditions suivantes.

</i18n>