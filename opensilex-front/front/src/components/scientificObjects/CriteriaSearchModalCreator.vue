<template>
  <opensilex-FormField
      :required="required"
      :requiredBlue="requiredBlue"
      :label="$t('CriteriaSearchModalCreator.filter-label')"
      :helpMessage="$t('CriteriaSearchModalCreator.help-message')"
  >
    <template v-slot:field="field">
      <b-spinner small label="Small Spinning" v-if="loading"></b-spinner>

<!--      Actual field, with recapitulated request : -->
      <b-input-group class="select-button-container">
        <treeselect
            class="multiselect-popup modalSearchLabel"
            :key="treeselectRefreshKey"
            :multiple="true"
            :openOnClick="true"
            :searchable="false"
            :clearable="true"
            valueFormat="object"
            :value="parseCriteriaForTreeselect"
            :placeholder="$t('CriteriaSearchModalCreator.placeholder')"
            :disable-branch-nodes="false"
            :search-nested="false"
            :show-count="true"
            @open="show"
            @input="clearIfNeeded"
            @deselect="removeCriteriaAndSave"
            :limit="10"
        >
        </treeselect>

        <b-input-group-append>
          <b-button class="createButton greenThemeColor" @click="show">>></b-button>
        </b-input-group-append>

      </b-input-group>

<!--      Modal popup where the user makes his request : -->
      <template>
        <b-modal ref="modalRef" size="xl" :static="true" :key="modalRefreshKey">
          <template v-slot:modal-title>
            <i class="ik ik-search mr-1"></i>
            {{ $t('CriteriaSearchModalCreator.title') }}
          </template>

          <template v-slot:modal-footer>
            <button
                type="button"
                class="btn btn-secondary"
                v-on:click="hide(false)"
            >{{ $t('CriteriaSearchModalCreator.cancel-button') }}</button>
      &nbsp;     <button
                type="button"
                class="btn greenThemeColor"
                v-on:click="hide(true)"
            >{{ $t('CriteriaSearchModalCreator.validate-button') }}</button>
            <font-awesome-icon
                tabindex="0"
                icon="question-circle"
                class="validateHelp"
                v-b-tooltip.hover.top="$t('CriteriaSearchModalCreator.validate-explanation')"
            />

          </template>

          <div class="card">
            <div>
              <p>{{ $t('CriteriaSearchModalCreator.modal-explanation') }}</p>
            </div>
            <ValidationObserver ref="validatorRef">
            <div
                v-for="(singleCriteria, index) in criteriaList"
                v-bind:key="index"
                class="row criteria-border"
            >
                <opensilex-VariableSelector
                    label="CriteriaSearchModalCreator.variable"
                    :variables.sync="singleCriteria.variable_uri"
                    :multiple="false"
                    @select="loadVariableInformation($event, singleCriteria)"
                    class="col-md-5"
                ></opensilex-VariableSelector>
                <opensilex-CriteriaOperatorSelector
                    :operator.sync="singleCriteria.criteria_operator"
                    :rules="singleCriteria.criteria_rules"
                    @select="loadCriteriaInformation($event, singleCriteria)"
                    class="col-md-2"
                ></opensilex-CriteriaOperatorSelector>
                <div class="col-md-5">
                  <label for="value">{{ $t("CriteriaSearchModalCreator.value") }}</label>
                  <opensilex-InputForm
                      v-if="$opensilex.checkURIs(singleCriteria.datatype, Xsd.INTEGER)"
                      :value.sync="singleCriteria.value"
                      type="number"
                      rules="integer"
                      :disabled="false"
                      :required="true"
                      placeholder="XSDIntegerInput.placeholder"
                  ></opensilex-InputForm>
                  <opensilex-InputForm
                      v-else-if="$opensilex.checkURIs(singleCriteria.datatype, Xsd.DECIMAL)"
                      :value.sync="singleCriteria.value"
                      type="number"
                      rules="decimal"
                      :disabled="false"
                      :required="true"
                      placeholder="XSDDecimalInput.placeholder"
                  ></opensilex-InputForm>
                  <opensilex-DateForm
                      v-else-if="$opensilex.checkURIs(singleCriteria.datatype, Xsd.DATE)"
                      :value.sync="singleCriteria.value"
                      :required="true"
                      class="searchFilter"
                  ></opensilex-DateForm>
                  <opensilex-DateTimeForm
                      v-else-if="$opensilex.checkURIs(singleCriteria.datatype, Xsd.DATETIME)"
                      :value.sync="singleCriteria.value"
                      :disabled="false"
                      :required="true"
                  ></opensilex-DateTimeForm>
                  <opensilex-SelectForm
                      v-else-if="$opensilex.checkURIs(singleCriteria.datatype, Xsd.BOOLEAN)"
                      :selected.sync="singleCriteria.value"
                      :multiple="false"
                      placeholder="CriteriaSearchModalCreator.boolean-field-palceholder"
                      :showCount="false"
                      :required="false"
                      :options="trueFalseList"
                  ></opensilex-SelectForm>
                  <!--  Disabled string filter if no variable has been selected or if the variable's datatype isn't registered yet, non disabled string filter if variable's datatype is deciaml or int -->
                  <opensilex-StringFilter
                      v-else
                      id="value"
                      :disabled="true"
                      :filter.sync="singleCriteria.value"
                      placeholder="CriteriaSearchModalCreator.value-placeholder"
                  ></opensilex-StringFilter>
                </div>

            </div>
            </ValidationObserver>

            <div class="add-criteria-button-container">
              <opensilex-Button
                  @click="addNewSingleCriteriaToList"
                  icon="fa#plus"
                  :small="true"
                  :disabled="false"
                  class="greenThemeColor add-criteria-button"
              ></opensilex-Button>
            </div>
          </div>
        </b-modal>
      </template>

    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop, PropSync, Ref } from 'vue-property-decorator';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import AsyncComputedProp from "vue-async-computed-decorator";
import {VariablesService} from "opensilex-core/api/variables.service";
import Xsd from "../../ontologies/Xsd";
import XSDIntegerInput from "../ontology/XSDIntegerInput.vue";
import { SelectableItem } from '../common/forms/SelectForm.vue';
import HttpResponse, { OpenSilexResponse } from 'opensilex-security/HttpResponse';
import { VariableDetailsDTO } from 'opensilex-core/index';

interface SingleCriteriaDTO {
  variable_uri: string,
  criteria_operator: string,
  value: any,
}

interface SingleCriteriaAttributesUsedInFront {
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

interface TreeselectItem{
  id: string,
  label: string
}

@Component({
  components: {XSDIntegerInput},
  computed: {
    CriteriaSearchModalCreator() {
      return CriteriaSearchModalCreator
    },
    Xsd() {
      return Xsd
    },
    parseCriteriaForTreeselect(): Array<TreeselectItem> {
      //Only show criteria that are complete
      let filteredIncompleteCriteria : Array<SingleCriteriaAttributesUsedInFront> = this.filterIncompleteCriteria();
      return filteredIncompleteCriteria.map((singleCriteria) => {return this.parseSingleCriteriaForTreeselect(singleCriteria);});
    }
  }
})
export default class CriteriaSearchModalCreator extends Vue {

  @Ref("validatorRef") readonly validatorRef!: any;

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
  variableService: VariablesService;
  //Remember the datatypes of picked variables in case the same variables come up
  savedVariablesDatatypes : Map<string, string> = new Map();
  criteriaList: Array<SingleCriteriaAttributesUsedInFront> = [];
  criteriaListOnLastValidate: Array<SingleCriteriaAttributesUsedInFront> = [];
  criteriaIdCount : number = 0;
  idCountOnLastValidate : number = 0;
  trueFalseList: Array<SelectableItem> = [{id: "true", label: "true"}, {id: "false", label: "false"}];

  treeselectRefreshKey = 0;
  modalRefreshKey = 0;

  async loadVariableInformation(variableIdAndLabelJson : any, singleCriteria: SingleCriteriaAttributesUsedInFront){
    let variableUri : string = variableIdAndLabelJson.id;
    singleCriteria.variable_name = variableIdAndLabelJson.label;
    let allreadyFetchedType : string = this.savedVariablesDatatypes.get(variableUri);
    if(!allreadyFetchedType){
      let response: HttpResponse<OpenSilexResponse<VariableDetailsDTO>> = await this.variableService.getVariable(variableUri);
      singleCriteria.datatype = response.response.result.datatype;
      this.savedVariablesDatatypes.set(variableUri, singleCriteria.datatype);
    }else{
      singleCriteria.datatype = allreadyFetchedType;
    }
    singleCriteria.criteria_rules="";
    if(this.$opensilex.checkURIs(singleCriteria.datatype, Xsd.BOOLEAN)){
      singleCriteria.criteria_rules="refuseOperators:vocabulary:LessThan,vocabulary:MoreThan,vocabulary:MoreOrEqualThan,vocabulary:LessOrEqualThan"
    }
  }

  /**
   * Returns a list of single criteria, excluding any that have empty or missing fields.
   * Uses CriteriaOnLastValidate
   */
  filterIncompleteCriteria() : Array<SingleCriteriaAttributesUsedInFront>{
    return this.criteriaListOnLastValidate.filter((singleCriteria) => {
      return (singleCriteria.criteria_operator) &&
          (singleCriteria.criteria_operator.trim().length > 0) &&
          (singleCriteria.value) &&
          (singleCriteria.value.trim().length > 0) &&
          (singleCriteria.variable_uri) &&
          (singleCriteria.variable_uri.trim().length > 0);
    });
  }

  loadCriteriaInformation(criteriaIdAndLabelJson : any, singleCriteria: SingleCriteriaAttributesUsedInFront){
    singleCriteria.criteria_symbol = criteriaIdAndLabelJson.label;
  }

  parseSingleCriteriaForTreeselect(singleCriteria : SingleCriteriaAttributesUsedInFront) : TreeselectItem {
    let stringifyedCriteria = singleCriteria.variable_name + " " + singleCriteria.criteria_symbol + " " + singleCriteria.value;
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

  generateEmptySingleCriteria (): SingleCriteriaAttributesUsedInFront {
    this.criteriaIdCount = this.criteriaIdCount + 1;
    return {
      variable_uri: "",
      criteria_operator: "",
      value: "",
      datatype: "",
      variable_name: "",
      criteria_symbol: "",
      id: this.criteriaIdCount.toString(),
      criteria_rules:""
    }
  }

  addNewSingleCriteriaToList(){
    let newOne:SingleCriteriaAttributesUsedInFront = this.generateEmptySingleCriteria();
    this.criteriaList.push(newOne);
  }

  removeCriteria(singleCriteriaIdAndLabel){
    this.criteriaList = this.criteriaList.filter(singleCriteria => singleCriteria.id !== singleCriteriaIdAndLabel.id);
    //Keep an empty one if we removed last one
    if(this.criteriaList.length===0){
      this.resetCriteriaList();
    }
  }

  removeCriteriaAndSave(singleCriteriaIdAndLabel) {
    this.removeCriteria(singleCriteriaIdAndLabel);
    this.setOnLastValidateParameters();
    this.setCriteriaDto();
  }

  created() {
    this.id = this.$opensilex.generateID();
    this.variableService = this.$opensilex.getService("opensilex.VariablesService");
    this.criteriaList = [this.generateEmptySingleCriteria()];
    this.setOnLastValidateParameters();
  }
  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  copySingleCriteriaWithAttributesForFront(singleCriteria : SingleCriteriaAttributesUsedInFront) : SingleCriteriaAttributesUsedInFront{
    return JSON.parse(JSON.stringify(singleCriteria));
  }

  setCriteriaDto() : void{
    this.criteriaDto = {criteria_list: this.filterIncompleteCriteria().map(e=>this.convertFrontSingleCriteriaToSingleCriteriaDto(e))};
  }

  /**
   * Saves state of list and id count information. This is the state we return to when the user hits cancel.
   */
  setOnLastValidateParameters(){
    this.criteriaListOnLastValidate = this.criteriaList.map(e=>this.copySingleCriteriaWithAttributesForFront(e));
    this.idCountOnLastValidate = this.criteriaIdCount;
  }

  async hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    if (validate) {
      if(await this.validatorRef.validate()){
        this.setOnLastValidateParameters();
        this.setCriteriaDto();
        modalRef.hide();
        this.treeselectRefreshKey += 1;
      }
    }else{
      this.criteriaIdCount = this.idCountOnLastValidate;
      this.criteriaList = this.criteriaListOnLastValidate.map(e=>e);
      modalRef.hide();
      this.treeselectRefreshKey += 1;
    }
  }

  clearIfNeeded(values : Array<SingleCriteriaAttributesUsedInFront>){
    if(values.length === 0){
      this.resetCriteriaListAndSave();
    }
  }

  resetCriteriaList(){
    this.criteriaIdCount = 0;
    let aNewElement : SingleCriteriaAttributesUsedInFront = this.generateEmptySingleCriteria();
    this.criteriaList = [aNewElement];
  }

  resetCriteriaListAndSave() {
    this.resetCriteriaList();
    this.setOnLastValidateParameters();
    this.treeselectRefreshKey += 1;
    this.setCriteriaDto();
  }

}

</script>

<style scoped lang="scss">

.criteria-border {
  border: solid 2px rgba(192, 194, 193, 0.59);
  border-radius: 5px;
  margin: 5px;
}

.criteria-filter {
  width: 100%
}

.balarow {
  display: inline;
}

.add-criteria-button-container {
  display: flex;
  justify-content: right;
}

.remove-criteria-button-container {
  display: flex;
  justify-content: right;
  width: 40px;
  height: 20px
}

.add-criteria-button{
  width: 50px;
  margin: 1% 1% 0 0;
  align-content: center;
  display: flex;
  justify-content: center;
}

//Style stuff taken from select form :

.modalSearchLabel {
  white-space: normal;
  text-overflow: ellipsis;
  overflow: hidden;
  width: 170px;
}

.multiselect-popup ~ .input-group-append > button {
  height: 100%;
}

::v-deep .multiselect-action .vue-treeselect__control,
::v-deep .multiselect-popup .vue-treeselect__control {
  border-bottom-right-radius: 0;
  border-top-right-radius: 0;
  border-bottom-left-radius: 5px !important;
  width: 100%;
  height: 35px;
}

::v-deep .multiselect-view.vue-treeselect,
::v-deep .multiselect-popup.vue-treeselect {
  width: calc(110% - 85px);
}

::v-deep .multiselect-view .vue-treeselect__control,
::v-deep .multiselect-popup .vue-treeselect__control {
  border-bottom-right-radius: 0;
  border-top-right-radius: 0;
  border-bottom-left-radius: 5px !important;
  width: 100%;
  height: 35px;
}

.select-button-container {
  margin-bottom: 0;
}

::v-deep .multiselect-popup .vue-treeselect__control-arrow-container {
  display: none;
}

::v-deep .multiselect-popup .vue-treeselect__menu-container {
  display: none;
}

.validateHelp{
  font-size: 1.3em;
  border-radius: 50%;
}
</style>

<i18n>

en:
  CriteriaSearchModalCreator:
    title: Criteria Creation
    placeholder: Click to create criteria list
    filter-label: Criteria on data
    help-message: Add an undefined amount of data criteria that the Scientific objects must validate to be counted in the result.
    validate-explanation: The search will only take the current list of criteria into account if you press validate. Press cancel to see current used criteria.
    modal-explanation: The '+' button will add a criteria to the list, then fill in with a variable, an operator and a value. Each criteria is combined with an "And" logical operator.
    variable: Variable
    value: Value
    value-placeholder: Enter value
    validate-button: Validate criteria list
    cancel-button: Revert changes
    boolean-field-palceholder: Enter true or false

fr:
  CriteriaSearchModalCreator:
    title: Création de critère
    placeholder: Cliquez pour créer une liste de critères
    filter-label: Critères par données
    help-message: Ajouter un nombre indéfini de critères sur les données que les Objets scientifiques doivent valider pour être prise en compte dans le résultat.
    validate-explanation: La recherche ne tiendrait compte la liste actuelle de critères que si vous appuyez sur Valider. Appuyez sur Annuler pour voir la liste actuelle qui sera prise en compte.
    modal-explanation : Le bouton '+' va ajouter un critère à la liste, ensuite remplir avec une variable, un opérateur et une valeur. Chaque critère est combiné avec un opérateur logique "Et".
    variable : Variable
    value: Valeur
    value-placeholder: Entrez valeur
    validate-button: Validez liste de critères
    cancel-button: Annulez modifications
    boolean-field-palceholder: Entrez vrai ou faux

</i18n>