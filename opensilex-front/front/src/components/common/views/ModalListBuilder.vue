<template>
  <opensilex-FormField
      :required="required"
      :requiredBlue="requiredBlue"
      :label="fieldLabel"
      :helpMessage="helpMessage"
  >
    <template v-slot:field="field">

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
            :value="parseListForTreeselect"
            :placeholder="placeholder"
            :disable-branch-nodes="false"
            :search-nested="false"
            :show-count="true"
            @open="show"
            @input="clearIfNeeded"
            @deselect="removeLineAndSave"
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
            {{ modalTitle }}
          </template>

          <template v-slot:modal-footer>
            <button
                type="button"
                class="btn btn-secondary"
                v-on:click="hide(false)"
            >{{ $t('ModalListBuilder.cancel-button') }}</button>
      &nbsp;     <button
              type="button"
              class="btn greenThemeColor"
              v-on:click="hide(true)"
          >{{ $t('ModalListBuilder.validate-button') }}</button>
            <font-awesome-icon
                tabindex="0"
                icon="question-circle"
                class="validateHelp"
                v-b-tooltip.hover.top="$t('ModalListBuilder.validate-explanation')"
            />

          </template>

          <div class="card">
            <div v-if="modalExplanation">
              <p>{{modalExplanation}}</p>
            </div>

            <ValidationObserver ref="validatorRef">
              <div
                  v-for="(singleLine, index) in lineList"
                  v-bind:key="index"
                  class="linerow criteria-border"
              >
                <component
                    :is="lineComponent"
                    :lineData="singleLine"
                    :lineIndex="index"
                    :extraProps="extraProps"
                    @updateLine="updateLine"
                    class="row col"
                ></component>
                <div class="remove-criteria-button-container">
                  <opensilex-Button
                      @click="removeLine(singleLine.id)"
                      icon="fa#minus"
                      :small="true"
                      :disabled="false"
                      class="greenThemeColor add-criteria-button"
                  ></opensilex-Button>
                </div>
              </div>
            </ValidationObserver>

            <div class="add-criteria-button-container">
              <opensilex-Button
                  @click="addNewSingleLineToList(null)"
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
import { BModal } from 'bootstrap-vue';
import { ValidationObserver } from 'vee-validate';
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop, Ref } from 'vue-property-decorator';
import { SelectableItem } from '../forms/SelectForm.vue';

interface LineData{
  id: string
}

@Component
export default class ModalListBuilder<T extends LineData, LineOutputClass> extends Vue {

  @Ref("validatorRef") readonly validatorRef!: InstanceType<typeof ValidationObserver>;
  @Ref("modalRef") readonly modalRef!: BModal;

  /**
   * Text that shows in the question mark next to the field
   */
  @Prop()
  helpMessage: string;

  /**
   * Paragraph at the top of modal
   */
  @Prop()
  modalExplanation: string;

  @Prop()
  fieldLabel: string;

  @Prop()
  modalTitle: string;

  @Prop()
  placeholder: string;

  @Prop({ default: false })
  disabled: boolean;

  @Prop()
  required: boolean;

  @Prop()
  requiredBlue: boolean;

  /**
   * The component that is used to build each line of the list in the modal.
   */
  @Prop()
  lineComponent: string;

  @Prop()
  extraProps: any;

  @Prop()
  parseSingleLineForTreeselect: (singleLine : T) => SelectableItem;

  @Prop()
  generateEmptyLine: (lineId: number) => T;

  @Prop()
  filterIncompleteLines: (lines: Array<T>) => Array<T>;

  @Prop()
  convertLineToOutputObject: (singleLine: T) => LineOutputClass;

  /**
   * This is the final list that parent components use once the user has finished building his list.
   */
  listToOutput: Array<LineOutputClass> = [];

  lineList: Array<T> = [];

  lineListOnLastValidate: Array<T> = [];

  //start from 1 as 0 can be used up to define a first empty line in the created methods
  //of classes that use this class.
  idCount: number = 1;

  idCountOnLastValidate: number = 0;
  treeselectRefreshKey = 0;
  modalRefreshKey = 0;

  get parseListForTreeselect(): Array<SelectableItem> {
    //Only show lines that are complete
    let filteredIncompleteCriteria : Array<T> = this.filterIncompleteLines(this.lineListOnLastValidate);
    return filteredIncompleteCriteria.map((singleCriteria) => {return this.parseSingleLineForTreeselect(singleCriteria);});
  }

  updateLine(fieldsToChange: any, lineListIndex: number){
    this.lineList = this.lineList.map((line, index) => {
      if(index === lineListIndex){
        //let lineCopy = this.copySingleLineWithAttributesForFront(line);
        for(const [key, value] of Object.entries(fieldsToChange)){
          line[key] = value;
        }
      }
      return line;
    })
  }

  setOutputList() {
    this.listToOutput = this.filterIncompleteLines(this.lineListOnLastValidate).map(e=>this.convertLineToOutputObject(e));
    this.$emit("validateList", this.listToOutput);
  }

  removeLineAndSave(lineIdAndLabel: SelectableItem) {
    this.removeLineUsingTreeselectItem(lineIdAndLabel);
    this.setOnLastValidateParameters();
    this.setOutputList();
  }

  removeLineUsingTreeselectItem(lineIdAndLabel: SelectableItem){
    this.removeLine(lineIdAndLabel.id);
  }

  removeLine(id: string){
    this.lineList = this.lineList.filter(singleCriteria => singleCriteria.id !== id);
    //Keep an empty one if we removed last one
    if(this.lineList.length===0){
      this.resetLineList();
    }
  }

  resetLineListWithInitialLabels(resetLineList: Array<T>){
    this.lineList = resetLineList;
    this.setOnLastValidateParameters();
  }

  /**
   *
   * @param optionalProvidedLine type any, but not really, will be the same params as SingleLineAttributesUsedInFront but without an id, the id gets made here
   */
  addNewSingleLineToList(optionalProvidedLine : any){
    this.idCount++;
    if(optionalProvidedLine===null){
      let newOne:T = this.generateEmptyLine(this.idCount);
      this.lineList.push(newOne);
    }else{
      optionalProvidedLine.id = this.idCount.toString();
      this.lineList.push(optionalProvidedLine);
    }
  }

  show() {
    this.modalRef.show();
  }

  async hide(validate: boolean) {
    if (validate) {
      if(await this.validatorRef.validate()){
        this.setOnLastValidateParameters();
        this.setOutputList();
        this.modalRef.hide();
        this.treeselectRefreshKey += 1;
      }
    }else{
      this.idCount = this.idCountOnLastValidate;
      this.lineList = this.lineListOnLastValidate.map(e=>e);
      this.modalRef.hide();
      this.treeselectRefreshKey += 1;
    }
  }

  clearIfNeeded(values : Array<SelectableItem>){
    if(values.length === 0){
      this.resetCriteriaListAndSave();
    }
  }

  /**
   * Resets the list to one empty line and validate.
   */
  resetCriteriaListAndSave() {
    this.resetLineList();
    this.setOnLastValidateParameters();
    this.setOutputList();
  }

  /**
   * Resets the list of lines to initial state.
   * That is one empty line so that the user doesn't just land on a blank page.
   */
  resetLineList(){
    this.idCount = 0;
    let aNewElement = this.generateEmptyLine(this.idCount);
    this.lineList.splice(0, this.lineList.length, aNewElement);
  }

  /**
   * Utility function to copy data of a line
   */
  copySingleLineWithAttributesForFront(singleLine : T) : T{
    return JSON.parse(JSON.stringify(singleLine));
  }

  /**
   * Saves state of list and id count information. This is the state we return to when the user hits cancel.
   */
  setOnLastValidateParameters(){
    this.lineListOnLastValidate = this.lineList.map(e=>this.copySingleLineWithAttributesForFront(e));
    this.idCountOnLastValidate = this.idCount;
  }
}
</script>

<style scoped lang="scss">

.criteria-border {
  border: solid 2px rgba(192, 194, 193, 0.59);
  border-radius: 5px;
  margin: 5px;
}

.add-criteria-button-container {
  display: flex;
  justify-content: right;
  vertical-align: center;
}

.linerow {
  display: flex;
  align-items: center;
}

.remove-criteria-button-container {
  width: 50px;
  margin-right: 15px;
  vertical-align: middle;
  display: inline;
}

.add-criteria-button{
  width: 50px;
  margin: 1% 1% 0 0;
  align-content: center;
  display: flex;
  justify-content: center;
}

.modalSearchLabel {
  white-space: normal;
  text-overflow: ellipsis;
  overflow: hidden;
  width: 170px;
}

.input-group{
  flex-wrap: nowrap;
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
  ModalListBuilder:
    validate-button: Validate list
    cancel-button: Revert changes
    validate-explanation: The search will only take the current list into account if you press validate. Press cancel to see current used list.

fr:
  ModalListBuilder:
    validate-button: Validez liste
    cancel-button: Annulez modifications
    validate-explanation: La recherche ne tiendrait compte la liste actuelle que si vous appuyez sur Valider. Appuyez sur Annuler pour voir la liste actuelle qui sera prise en compte.

</i18n>