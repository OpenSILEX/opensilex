<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :requiredBlue="requiredBlue"
    :label="label"
    :helpMessage="helpMessage"
  >
  <!-------------------------------------------------------------------->
    <template v-slot:field="field">
      <b-spinner small label="Small Spinning" v-if="loading"></b-spinner>
      <input :id="field.id" type="hidden" :value="hiddenValue" />
      <b-input-group class="select-button-container">

        <opensilex-CustomTreeselect
          v-bind="$attrs"
          v-on="$listeners"
          :searchMethod="searchMethod"
          :async="async"
          :resultLimit="resultLimit"
          @close="close(field)"
          @totalCount="updateTotalCount"
          @resultCount="updateResultCount"
          @select="select"
          :multiple="multiple"
          :selected.sync="selection"

          ref="treeref"
        >
          <!-- @deselect="deselect"
          @select="select"
          @input="clearIfNeeded"
          @close="close(field)"
          @search-change="onSearchChange"
          @keyup.enter.native="onEnter"
           -->

          <!-- choix dans les listes déroulantes -->

          
          <!-- elements selectionnés (pour les filtres à choix multiples) -->
          <template v-slot:value-label="{ node }">
            <opensilex-CustomValueLabel :node="node" />
          </template>
          
          <template v-slot:after-list v-if="resultCount < totalCount">
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
            :detailVisible="viewHandlerDetailsVisible"
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
            :detailVisible="viewHandlerDetailsVisible"
            :label="(viewHandlerDetailsVisible ? 'FormSelector.hideDetails' : 'FormSelector.showDetails')"
            :small="true"
          ></opensilex-DetailButton>
        </b-input-group-append>

      </b-input-group>


    </template>
    <!-------------------------------------------------------------------->
  </opensilex-FormField>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Watch, Ref, Provide } from "vue-property-decorator";
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
  $opensilex: any;
  $t: any;
  currentValue;
  loading = false;

  @Ref("treeref") readonly  treeref!: any;

  @PropSync("selected")
  selection;


  isMultiple: boolean = false;
  hadShowCount: boolean = false;
  isClearable: boolean = true;
  isDisabled: boolean = false;
  hadDisableBranchNodes: boolean = false;
  hadSearchNested: boolean = false;


  private hadPlaceholder: string = "";
  private hadSearchPromptText: string = 'component.common.search-prompt-text';
  private hadNoResultsText: string = 'component.common.filter-search-no-result';
  hadDefaultExpandLevel: number = 2;
  hadLimit: number = 1;
  private selectedValuesData: any = null;
  
  @Prop()
  searchMethod;

  @Prop()
  async;
  

  setSearchMethod(value: any){
    this.searchMethod = value;
  }


  // @Provide() treeselectProps = {
  //   multiple: this.isMultiple,
  //   showCount: this.hadShowCount,
  //   disabled: this.isDisabled,
  //   disableBranchNodes: this.hadDisableBranchNodes,
  //   searchNested: this.hadSearchNested,
  //   clearable: this.isClearable,
  //   placeholder: Ref(this.hadPlaceholder),
  //   searchPromptText: Ref(this.hadSearchPromptText),
  //   noResultsText: Ref(this.hadNoResultsText),
  //   defaultExpandLevel: this.hadDefaultExpandLevel,
  //   limit: this.hadLimit,
  //   key: this.treeselectRefreshKey,
  //   value: this.selectedValuesData,
  //   async: this.searchMethod !== null,
  //   defaultOptions: this.searchMethod !== null,
  // };


  /**
   * selection but as a list of jsons, containing at least name and uri of each selected item. Required to show labels of pre-existing elements
   */
  @Prop({default: null})
  selectedInJsonFormat;

 @Prop()
  multiple;

  @Prop()
  itemLoadingMethod;

  @Prop()
  optionsLoadingMethod;

  internalOption = null;

  @Prop()
  options;

  // @Prop()
  // searchMethod;

  @Prop({
    default: true,
  })
  clearable;

  @Prop({
    default: false,
  })
  showCount;

  // @Prop({
  //   default: 0
  // })
  totalCount = 0;

  // @Prop({
  //   default: 0
  // })
  resultCount = 0;




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

  @Prop()
  requiredBlue: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;


  flat: boolean = true;

  @Prop({
    default: null,
  })
  actionHandler;

  @Prop({
    default: null,
  })
  viewHandler;

  @Prop({
    default: false,
  })
  viewHandlerDetailsVisible


  resultLimit = 10;


  @Prop()
  defaultSelectedValue;

  @Watch("selection")
  onSelectionChange() {
    this.currentValue = null;
  }

  @Prop({
    default: false,
  })
  disableBranchNodes: boolean;

  @Prop({
    default: false,
  })
  searchNested: boolean;

  @Prop()
  maximumSelectedItems;

  @Prop({default: 1})
  limit: number; // limit number of items in the input box

  // props for variableSelectorWithFilter
  @Prop()
  withAssociatedData;

  @Prop()
  experiment;

  @Prop()
  objects;

  @Prop()
  devices;

  countsData;

  detailVisible: boolean = false;
    // confirmed modal selection
  selectedCopie = [];
  // temporary modal selection
  selectedTmp = [];

  firstTimeOpening = false;

  /**
   * Refresh key for the Treeselect component. Used by the {@link refresh} method.
   */

  // treeselectRefreshKey: number = 0;

// juste fonction et l'@AsyncComputedProp() dans l'enfant ?
  // @AsyncComputedProp()
  // selectedValues(): Promise<any> {
  //   return new Promise((resolve, reject) => {
  //     console.log("Async FormSelect")
  //       if (this.itemLoadingMethod) {
  //         if (!this.selection || this.selection.length == 0) {
  //           if (this.multiple) {
  //             resolve([]);
  //           } else {
  //             resolve();
  //           }
  //         } else if (this.currentValue) {
  //           resolve(this.currentValue);
  //         } else {
  //           this.$opensilex.disableLoader();
  //           let uris = this.selection;
  //           if (!this.multiple) {
  //             uris = [this.selection];
  //           }
  //           let loadingPromise = this.itemLoadingMethod(uris);
  //           if (!(loadingPromise instanceof Promise)) {
  //             loadingPromise = Promise.resolve(loadingPromise);
  //           }
  //           loadingPromise
  //               .then((list) => {
  //                 let nodeList = [];
  //                 list.forEach((item) => {
  //                   nodeList.push(this.conversionMethod(item));
  //                 });
  //                 if (this.multiple) {
  //                   this.currentValue = nodeList;
  //                 } else {
  //                   this.currentValue = nodeList[0];
  //                 }
  //                 // this.selectedValuesData = this.currentValue;
  //                 resolve(this.currentValue);
  //               })
  //               .catch((error) => {

  //                 this.$opensilex.errorHandler(error);
  //                 reject(error);
  //               });
  //         }
  //       } else if (this.searchMethod) {
  //         // If there is a search method but no item loading method, then initial values
  //         // cannot be retrieved.
  //         console.warn("A search method was specified but no item loading method.")
  //         resolve(undefined);
  //       } else {
  //         let currentOptions = this.options || this.internalOption;
  //         if (this.multiple) {
  //           if (this.selection && this.selection.length > 0) {
  //             let items = this.findListInTree(currentOptions, this.selection);
  //             // this.selectedValuesData = items;
  //             resolve(items);
  //           } else {
  //             resolve([]);
  //           }
  //         } else {
  //           if (this.selection) {
  //             let item = this.findInTree(currentOptions, this.selection);
  //             // this.selectedValuesData = item;
  //             resolve(item);
  //           } else {
  //             resolve();
  //           }
  //         }
  //     }
  //   });
  // }

  setSelectorToFirstTimeOpen(){
    this.firstTimeOpening = true;
  }


  public findInTree(tree, id) {
    for (let i in tree) {
      let item = tree[i];

      if (item.id == id) {
        return item;
      }

      let childItem = this.findInTree(item.children, id);
      if (childItem != null) {
        return childItem;
      }
    }
  }

    loadMoreItems(){
    this.resultLimit = 0;
    this.treeref.refresh();
    this.$nextTick(() => {
      this.treeref.openTreeselect();
    // selectForm.refresh();
    // this.$nextTick(() => {
    //   selectForm.openTreeselect();
    })
  }

  public findListInTree(tree, ids, list?) {
    list = list || [];
    for (let i in tree) {
      let item = tree[i];

      if (ids.indexOf(item.id) >= 0) {
        list.push(item);
        if (list.length == ids.length) {
          return list;
        }
      }

      let childItems = this.findListInTree(item.children, ids, list);
      if (list.length == ids.length) {
        return list;
      }
    }

    return list;
  }

  get hiddenValue() {
    if (this.multiple) {
      if(Array.isArray(this.selection)) {
        if (this.selection.length > 0) {
          return this.selection.join(",");
        }else{
          return "";
        }
      }
    } else {
      if (this.selection) {
        return this.selection;
      }
    }

    return "";
  }

  debounce(func, wait, immediate?): Function {
    var timeout;
    var context: any = this;
    return function () {
      var args = arguments;
      var later = function () {
        timeout = null;
        if (!immediate) func.apply(context, args);
      };
      var callNow = immediate && !timeout;
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
      if (callNow) func.apply(context, args);
    };
  }

  lastSearchQuery = ".*";

  close(field) {
    if (field.validator) {
      this.$nextTick(() => field.validator.validate());
    }
  }

  select(value){
    console.log("FormSelector value", value)
    this.$emit("select", value);
  }

  updateTotalCount(totalCountUpdate){
    this.totalCount = totalCountUpdate;
  }

  updateResultCount(resultCountUpdate){
    this.resultCount = resultCountUpdate;
  }
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
  width: calc(100% - 85px);
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
