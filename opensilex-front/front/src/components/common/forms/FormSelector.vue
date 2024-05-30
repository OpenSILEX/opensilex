<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :requiredBlue="requiredBlue"
    :label="label"
    :helpMessage="helpMessage"
  >

    <template v-slot:field="field">
      <b-spinner small label="Small Spinning" v-if="loading"></b-spinner>
      <input :id="field.id" type="hidden" :value="hiddenValue" />
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
          :options="options || internalOption"
          :viewHandler="viewHandler"
          :itemLoadingMethod="itemLoadingMethod"
          :conversionMethod="conversionMethod"
        >

          <!-- choice in lists -->
          <template v-slot:option-label ="{ node }" >
            <opensilex-CustomTreeselectOptionLabel :node="node" />
          </template>
          
          <!-- selected elements (for multiple choice filters)-->
          <template v-slot:value-label="{ node }">
            <opensilex-CustomTreeselectValueLabel :node="node" />
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
  @Ref("customTreeselect") readonly customTreeselect!: CustomTreeselect;

  @PropSync("selected")
  selection;

  /**
   * selection but as a list of jsons, containing at least name and uri of each selected item. Required to show labels of pre-existing elements
   */
  @Prop({default: null})
  selectedInJsonFormat;

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

  internalOption = null;

  @Prop()
  options;

  @Prop({
    default: true,
  })
  clearable;

  @Prop({
    default: false,
  })
  showCount;

  totalCount = 0;
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

  @Prop()
  viewHandler: Function;

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

  refresh(){
    this.customTreeselect.refresh()
  }

  openTreeselect(){
    this.customTreeselect.openTreeselect()
  }

    loadMoreItems(){
    this.resultLimit = 0;
    this.customTreeselect.refresh();
    this.$nextTick(() => {
      this.customTreeselect.openTreeselect();
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
    this.$emit("select", value);
  }

  deselect(value){
    this.$emit("deselect", value)
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
