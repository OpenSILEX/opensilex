<template>
  <div>
    <opensilex-VariableSelector
        label="CriteriaSearchModalLine.variable"
        :variables.sync="lineData.variable_uri"
        :multiple="false"
        :required="true"
        @select="loadVariableInformation($event)"
        class="col-md-5"
    ></opensilex-VariableSelector>
    <opensilex-CriteriaOperatorSelector
        :operator.sync="lineData.criteria_operator"
        :rules="lineData.criteria_rules"
        :required="true"
        @select="loadCriteriaInformation($event)"
        class="col-md-2"
    ></opensilex-CriteriaOperatorSelector>
    <div class="col-md-5">
      <opensilex-InputForm
          v-if="$opensilex.checkURIs(lineData.datatype, Xsd.INTEGER)"
          label="CriteriaSearchModalLine.value"
          :value.sync="lineData.value"
          type="number"
          rules="integer"
          :disabled="criteriaOperatorIsIsNotMeasured()"
          :required="!criteriaOperatorIsIsNotMeasured()"
          placeholder="XSDIntegerInput.placeholder"
      ></opensilex-InputForm>
      <opensilex-InputForm
          v-else-if="$opensilex.checkURIs(lineData.datatype, Xsd.DECIMAL)"
          label="CriteriaSearchModalLine.value"
          :value.sync="lineData.value"
          type="number"
          rules="decimal"
          :disabled="criteriaOperatorIsIsNotMeasured()"
          :required="!criteriaOperatorIsIsNotMeasured()"
          placeholder="XSDDecimalInput.placeholder"
      ></opensilex-InputForm>
      <opensilex-DateForm
          v-else-if="$opensilex.checkURIs(lineData.datatype, Xsd.DATE)"
          label="CriteriaSearchModalLine.value"
          :value.sync="lineData.value"
          :required="!criteriaOperatorIsIsNotMeasured()"
          :disabled="criteriaOperatorIsIsNotMeasured()"
          class="searchFilter"
      ></opensilex-DateForm>
      <opensilex-DateTimeForm
          v-else-if="$opensilex.checkURIs(lineData.datatype, Xsd.DATETIME)"
          label="CriteriaSearchModalLine.value"
          :value.sync="lineData.value"
          :disabled="criteriaOperatorIsIsNotMeasured()"
          :required="!criteriaOperatorIsIsNotMeasured()"
      ></opensilex-DateTimeForm>
      <opensilex-SelectForm
          v-else-if="$opensilex.checkURIs(lineData.datatype, Xsd.BOOLEAN)"
          label="CriteriaSearchModalLine.value"
          :selected.sync="lineData.value"
          :multiple="false"
          placeholder="CriteriaSearchModalLine.boolean-field-placeholder"
          :showCount="false"
          :required="!criteriaOperatorIsIsNotMeasured()"
          :disabled="criteriaOperatorIsIsNotMeasured()"
          :options="trueFalseList"
      ></opensilex-SelectForm>
      <!--  Disabledfilter if no variable has been selected or if the variable's datatype isn't registered yet, non disabled string filter if variable's datatype is deciaml or int -->
      <opensilex-InputForm
          v-else
          label="CriteriaSearchModalLine.value"
          type="string"
          :disabled="true"
          :required="false"
          placeholder="CriteriaSearchModalLine.value-placeholder"
      ></opensilex-InputForm>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import Xsd from "../../ontologies/Xsd";
import { SelectableItem } from '../common/forms/SelectForm.vue';
import { SingleCriteriaAttributesUsedInFront } from './CriteriaSearchModalCreator.vue';
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {VariablesService} from "opensilex-core/api/variables.service";
import Oeso from "../../ontologies/Oeso";

@Component
export default class CriteriaSearchModalLine extends Vue {

  @Prop()
  lineData: SingleCriteriaAttributesUsedInFront;

  @Prop()
  lineIndex: number;

  $opensilex: OpenSilexVuePlugin;
  variableService: VariablesService;
  //Remember the datatypes of picked variables in case the same variables come up
  savedVariablesDatatypes : Map<string, string> = new Map();
  trueFalseList: Array<SelectableItem> = [{id: "true", label: "true"}, {id: "false", label: "false"}];

  get Oeso() {
    return Oeso
  }

  get Xsd() {
    return Xsd
  }

  created(){
    this.variableService = this.$opensilex.getService("opensilex.VariablesService");
  }

  criteriaOperatorIsIsNotMeasured() : boolean{
    return (this.lineData.criteria_operator ? this.lineData.criteria_operator === "NotMeasured" : false);
  }

  loadCriteriaInformation(criteriaIdAndLabelJson : SelectableItem){
    let lineFieldsToChange = {};
    lineFieldsToChange['criteria_symbol'] = criteriaIdAndLabelJson.label;
    this.$emit("updateLine", lineFieldsToChange, this.lineIndex);
  }

  async loadVariableInformation(variableIdAndLabelJson : SelectableItem){
    let lineFieldsToChange = {};
    let variableUri : string = variableIdAndLabelJson.id;
    lineFieldsToChange['variable_name'] = variableIdAndLabelJson.label;
    let allreadyFetchedType : string = this.savedVariablesDatatypes.get(variableUri);
    if(!allreadyFetchedType){
      let response: HttpResponse<OpenSilexResponse<VariableDetailsDTO>> = await this.variableService.getVariable(variableUri);
      lineFieldsToChange['datatype'] = response.response.result.datatype;
      this.savedVariablesDatatypes.set(variableUri, lineFieldsToChange['datatype']);
    }else{
      lineFieldsToChange['datatype'] = allreadyFetchedType;
    }
    lineFieldsToChange['criteria_rules']="";
    if(this.$opensilex.checkURIs(lineFieldsToChange['datatype'], Xsd.BOOLEAN)){
      lineFieldsToChange['criteria_rules'] ="refuseOperators:vocabulary:LessThan,vocabulary:MoreThan,vocabulary:MoreOrEqualThan,vocabulary:LessOrEqualThan"
    }
    this.$emit("updateLine", lineFieldsToChange, this.lineIndex);
  }
}
</script>

<style scoped lang="scss">
  .label-notinline {
    display: block;
  }
</style>

<i18n>

en:
  CriteriaSearchModalLine:
    variable: Variable
    value: Value
    value-placeholder: Enter value
    boolean-field-placeholder: Enter true or false

fr:
  CriteriaSearchModalLine:
    variable : Variable
    value: Valeur
    value-placeholder: Entrez valeur
    boolean-field-placeholder: Entrez vrai ou faux

</i18n>
