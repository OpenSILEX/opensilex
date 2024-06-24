<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :requiredBlue="requiredBlue"
    :label="label"
    :helpMessage="helpMessage"
  >

    <template v-slot:field="field">
      <!-- <b-spinner small label="Small Spinning" v-if="loading"></b-spinner> -->
      <input :id="field.id" type="hidden" />
      <b-input-group class="select-button-container">

        <opensilex-CustomTreeselect
        ref="customTreeselect"
          v-bind="$attrs"
          v-on="$listeners"
          :searchMethod="searchMethod"
          :async="async"
          :resultLimit="resultLimit"
          @close="close(field)"
          @totalCount="updateTotalCount"
          @resultCount="updateResultCount"
          @select="select"
          @deselect="deselect"
          :multiple="multiple"
          :selected.sync="selection"
          :placeholder="placeholder"
          :optionsLoadingMethod="optionsLoadingMethod"
          :options="options"
          :viewHandler="viewHandler"
          :itemLoadingMethod="itemLoadingMethod"
          :conversionMethod="conversionMethod"
          :defaultSelectedValue="defaultSelectedValue"
          :showCount="showCount"
          :noResultsText="$t(noResultsText)"
        >
          
          <template v-slot:after-list v-if="resultCount < totalCount && !showAllResults">
            <opensilex-CustomTreeselectRefineSearchMessage
              @loadMoreItems="loadMoreItems"
              :totalCount="totalCount"
              :resultCount="resultCount"
            />
          </template>

        </opensilex-CustomTreeselect>
        
        <!-- detail view-->
        <b-input-group-append v-if="!actionHandler && viewHandler">
           <opensilex-DetailButton
            v-if="viewHandler"
            @click="viewHandler"
            :label="(viewHandlerDetailsVisible ? 'FormSelector.hideDetails' : 'FormSelector.showDetails')"
            :small="true"
            class="greenThemeColor"
          ></opensilex-DetailButton>
        </b-input-group-append>

        <!-- create entity view-->
        <b-input-group-append v-else-if="actionHandler">
          <b-button class="greenThemeColor" @click="actionHandler">+</b-button>
          <opensilex-DetailButton
            v-if="viewHandler"
            @click="viewHandler"
            :label="(viewHandlerDetailsVisible ? 'FormSelector.hideDetails' : 'FormSelector.showDetails')"
            :small="true"
          ></opensilex-DetailButton>
        </b-input-group-append>

      </b-input-group>

    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref, Provide } from "vue-property-decorator";
import Vue from "vue";
import AsyncComputedProp from "vue-async-computed-decorator";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import CustomTreeselect from "./CustomTreeselect.vue";


export interface SelectableItem {
  id: string,
  label: string,
  isDisabled?: boolean
}

@Component
export default class FormSelector extends Vue {

  //#region Plugins and Datas
  $opensilex: any;
  $t: any;
  showAllResults : boolean = false;
  totalCount = 0;
  resultCount = 0;
  resultLimit = 10;
  flat: boolean = true;
  //#endregion

  //#region Refs
  @Ref("customTreeselect") readonly customTreeselect!: CustomTreeselect;
  //#endregion

  //#region Props
  @PropSync("selected")
  selection;

  @Prop()
  searchMethod;

  @Prop()
  async;

  @Prop()
  multiple;

  @Prop()
  itemLoadingMethod;

  @Prop()
  optionsLoadingMethod;

  @Prop()
  options;

  @Prop({
    default: false,
  })
  showCount;

  @Prop({
    type: Function,
    default: function (e) {
      if (e && e.name) {
        return {
            id: e.uri,
            label: e.name,
            isDisabled: e.isDisabled ?? false
          };
      } else {
        return e;
      }
    }
  })
  conversionMethod: (dto: NamedResourceDTO) => SelectableItem;
  
  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @Prop({
    default: "component.common.filter-search-no-result",
  })
  noResultsText: string;

  @Prop()
  required: boolean;

  //a blue star on text fields to indicate that at least one of them is required.
  @Prop()
  requiredBlue: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop({
    default: null,
  })
  actionHandler;

  @Prop()
  viewHandler: Function;

  @Prop({
    default: false,
  })
  viewHandlerDetailsVisible

  @Prop()
  defaultSelectedValue;
  //#endregion

   //#region Methods
    refresh(){
      this.customTreeselect.refresh()
    }
  
    openTreeselect(){
      this.customTreeselect.openTreeselect()
    }
  
    loadMoreItems(){
      this.resultLimit = 0;
      this.showAllResults = true;
      this.customTreeselect.refresh();
      this.$nextTick(() => {
        this.customTreeselect.openTreeselect();
      })
    }
   //#endregion

   //#region Events Handlers
   close(field) {
    if (field.validator) {
      this.$nextTick(() => field.validator.validate());
    }
  }

  select(value){
    this.$emit("select", value);
  }

  deselect(value){
    this.$emit("deselect", value)
  }

  /**
  *  Use Vue.set to ensure reactivity (target, key, value ) 
  *  target is the object of array to update
  *  key is the key or the index to modify
  *  value is the new value to give to this key or index
  */
  updateTotalCount(totalCountUpdate){
    this.$set(this, 'totalCount', totalCountUpdate);
  }

  updateResultCount(resultCountUpdate){
    this.$set(this, 'resultCount', resultCountUpdate);
  }
  //#endregion
}
</script>

<style scoped lang="scss">
::v-deep .multiselect-action.vue-treeselect,
::v-deep .multiselect-popup.vue-treeselect {
  width: calc(100% - 38px);
}

.multiselect-popup ~ .input-group-append > button {
  height: 100%;
}

::v-deep .multiselect-action .vue-treeselect__control {
  border-bottom-right-radius: 0;
  border-top-right-radius: 0;
  border-bottom-left-radius: 5px !important;
  width: 100%;
  height: 35px;
}

::v-deep .multiselect-view.vue-treeselect {
  width: calc(100% - 85px);
}

::v-deep .multiselect-view .vue-treeselect__control {
  border-bottom-right-radius: 0;
  border-top-right-radius: 0;
  border-bottom-left-radius: 5px !important;
  width: 100%;
  height: 35px;
}

.select-button-container {
  margin-bottom: 0;
}

i.more-results-info {
  margin-left: 10px;
  margin-right: 10px;
}
.greenThemeColor {
  color: #fff
}

.label {
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}


.refineSearchMessage {
  font-weight: bold;
  background-color: #00A28C;
  color: #FFFFFF ;
  cursor:pointer;
}
</style>

<i18n>
en:
  FormSelector:
    refineSearchMessage: "{0} / {1} results displayed, please refine your search or click HERE to display all results"
    showDetails : "Show details"
    hideDetails : "Hide details"
  
fr:
  FormSelector:
    refineSearchMessage: "{0} / {1} résultats affichés, merci de préciser votre recherche ou de cliquer ICI pour afficher tous les résultats"
    showDetails : "Afficher les détails"
    hideDetails : "Masquer les détails"

</i18n>
