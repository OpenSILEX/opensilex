<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <input :id="field.id" type="hidden" :value="hiddenValue" />
      <b-input-group class="select-button-container">
        <treeselect
          v-if="isModalSearch"
          class="multiselect-popup"
          :multiple="true"
          :openOnClick="openOnClick"
          :searchable="false"
          :clearable="clearable"
          valueFormat="object"
          v-model="selection"
          :placeholder="$t(placeholder)"
          :disable-branch-nodes="disableBranchNodes"
          :search-nested="searchNested"
          :show-count="showCount"
          @deselect="searchModal.unSelect($event)"
          @open="showModal"
        >
          <template v-slot:option-label="{ node }">
            <slot name="option-label" v-bind:node="node">{{ node.label }}</slot>
          </template>
          <template v-slot:value-label="{ node }">
            <slot name="value-label" v-bind:node="node">{{ node.label }}</slot>
          </template>
        </treeselect>
        <treeselect
          v-else-if="optionsLoadingMethod"
          v-bind:class="{
            'multiselect-action': actionHandler,
            'multiselect-view': viewHandler,
          }"
          :multiple="multiple"
          :flat="multiple && flat"
          :value="selectedValues"
          valueFormat="node"
          :async="searchMethod != null"
          :default-options="searchMethod != null"
          :load-options="loadOptions"
          :options="internalOption"
          :placeholder="$t(placeholder)"
          :disabled="disabled"
          :clearable="clearable"
          :defaultExpandLevel="2"
          @deselect="deselect"
          @select="select"
          @input="clearIfNeeded"
          @close="close(field)"
          :noResultsText="$t(noResultsText)"
          :searchPromptText="$t('component.common.search-prompt-text')"
          :disable-branch-nodes="disableBranchNodes"
          :search-nested="searchNested"
          :show-count="showCount"
        >
          <template v-slot:option-label="{ node }">
            <slot name="option-label" v-bind:node="node">{{ node.label }}</slot>
          </template>
          <template v-slot:value-label="{ node }">
            <slot name="value-label" v-bind:node="node">{{ node.label }}</slot>
          </template>
          <template v-if="resultCount < totalCount" v-slot:after-list>
            <i class="more-results-info">{{
              $t("SelectorForm.refineSearchMessage", [resultCount, totalCount])
            }}</i>
          </template>
        </treeselect>
        <treeselect
          v-else
          v-bind:class="{
            'multiselect-action': actionHandler,
            'multiselect-view': viewHandler,
          }"
          :multiple="multiple"
          :flat="multiple && flat"
          :value="selectedValues"
          valueFormat="node"
          :async="searchMethod != null"
          :default-options="searchMethod != null"
          :load-options="loadOptions"
          :options="options || internalOption"
          :placeholder="$t(placeholder)"
          :disabled="disabled"
          :clearable="clearable"
          :defaultExpandLevel="2"
          @deselect="deselect"
          @select="select"
          @input="clearIfNeeded"
          @close="close(field)"
          :noResultsText="$t(noResultsText)"
          :searchPromptText="$t('component.common.search-prompt-text')"
          :disable-branch-nodes="disableBranchNodes"
          :search-nested="searchNested"
          :show-count="showCount"
        >
          <template v-slot:option-label="{ node }">
            <slot name="option-label" v-bind:node="node">{{ node.label }}</slot>
          </template>
          <template v-slot:value-label="{ node }">
            <slot name="value-label" v-bind:node="node">{{ node.label }}</slot>
          </template>
          <template v-if="resultCount < totalCount" v-slot:after-list>
            <i class="more-results-info">{{
              $t("SelectorForm.refineSearchMessage", [resultCount, totalCount])
            }}</i>
          </template>
        </treeselect>
        <b-input-group-append v-if="isModalSearch">
          <b-button variant="primary" @click="showModal">+</b-button>
        </b-input-group-append>
        <b-input-group-append v-else-if="!actionHandler && viewHandler">
           <opensilex-DetailButton
            v-if="viewHandler"
            @click="viewHandler"
            :detailVisible="viewHandlerDetailsVisible"
            :label="(viewHandlerDetailsVisible ? 'SelectorForm.hideDetails' : 'SelectorForm.showDetails')"
            :small="true"
          ></opensilex-DetailButton>
        </b-input-group-append>
        <b-input-group-append v-else-if="actionHandler">
          <b-button variant="primary" @click="actionHandler">+</b-button>
          <opensilex-DetailButton
            v-if="viewHandler"
            @click="viewHandler"
            :detailVisible="viewHandlerDetailsVisible"
            :label="(viewHandlerDetailsVisible ? 'SelectorForm.hideDetails' : 'SelectorForm.showDetails')"
            :small="true"
          ></opensilex-DetailButton>
        </b-input-group-append>
   
      </b-input-group>
      <component
        v-if="isModalSearch"
        :is="modalComponent"
        ref="searchModal"
        :maximumSelectedRows="maximumSelectedItems"
        @onValidate="updateValues"
      ></component>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Watch, Ref } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";
import AsyncComputedProp from "vue-async-computed-decorator";

@Component
export default class SelectForm extends Vue {
  $opensilex: any;
  currentValue;

  @Ref("searchModal") readonly searchModal!: any;

  @PropSync("selected")
  selection;

  @Prop()
  multiple;

  @Prop()
  itemLoadingMethod;

  @Prop()
  optionsLoadingMethod;

  internalOption = null;

  @Prop()
  options;

  @Prop()
  searchMethod;

  @Prop({
    default: false,
  })
  isModalSearch;

  @Prop({
    default: true,
  })
  clearable;

  @Prop({
    default: true,
  })
  openOnClick;

  @Prop({
    default: false,
  })
  showCount;

  @Prop()
  modalComponent;

  @Prop({
    type: Function,
    default: function (e) {
      return e;
    },
  })
  conversionMethod: Function;

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
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop({
    default: true,
  })
  flat: boolean;

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

  @Prop({
    default: 10,
  })
  resultLimit;


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
  detailVisible: boolean = false;

  @AsyncComputedProp()
  selectedValues(): Promise<any> {
    return new Promise((resolve, reject) => {
      if (this.itemLoadingMethod) {
        if (!this.selection || this.selection.length == 0) {
          if (this.multiple) {
            resolve([]);
          } else {
            resolve();
          }
        } else if (this.currentValue) {
          resolve(this.currentValue);
        } else {
          this.$opensilex.disableLoader();
          let uris = this.selection;
          if (!this.multiple) {
            uris = [this.selection];
          }
          let loadingPromise = this.itemLoadingMethod(uris);
          if (!(loadingPromise instanceof Promise)) {
            loadingPromise = Promise.resolve(loadingPromise);
          }
          loadingPromise
            .then((list) => {
              let nodeList = [];
              list.forEach((item) => {
                nodeList.push(this.conversionMethod(item));
              });
              if (this.multiple) {
                this.currentValue = nodeList;
              } else {
                this.currentValue = nodeList[0];
              }

              resolve(this.currentValue);
            })
            .catch((error) => {
              this.$opensilex.errorHandler(error);
              reject(error);
            });
        }
      } else if (this.searchMethod) {
        resolve(this.selectedNodes);
      } else {
        let currentOptions = this.options || this.internalOption;
        if (this.multiple) {
          if (this.selection && this.selection.length > 0) {
            let items = this.findListInTree(currentOptions, this.selection);
            resolve(items);
          } else {
            resolve([]);
          }
        } else {
          if (this.selection) {
            let item = this.findInTree(currentOptions, this.selection);
            resolve(item);
          } else {
            resolve();
          }
        }
      }
    });
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
      if (this.selection.length > 0) {
        return this.selection.join(",");
      }
    } else {
      if (this.selection) {
        return this.selection;
      }
    }

    return "";
  }

  select(value) {
    if (this.multiple) {
      this.selection.push(value.id);
    } else {
      this.selection = value.id;
    }
    this.$emit("select", value);
  }

  deselect(value) {
    if (this.multiple) {
      this.selection = this.selection.filter((id) => id !== value.id);
    } else {
      this.selection = null;
    }
    this.$emit("deselect", value);
  }

  close(field) {
    if (field.validator) {
      this.$nextTick(() => field.validator.validate());
    }
  }

  clearIfNeeded(values) {
    if (this.multiple) {
      if (values.length == 0) {
        this.selection.splice(0, this.selection.length);
        this.$emit("clear");
        return;
      }
    } else if (!values) {
      this.selection = undefined;
      this.$emit("clear");
      return;
    }

    if (this.multiple) {
      let newValues = [];
      for (let i in values) {
        newValues.push(values[i].id);
      }
      this.selection = newValues;
    }
  }

  loadOptions({ action, searchQuery, callback }) {
    if (action === "ASYNC_SEARCH") {
      this.debounceSearch(searchQuery, callback);
    } else if (action === "LOAD_ROOT_OPTIONS") {
      if (this.optionsLoadingMethod) {
        this.$opensilex.disableLoader();
        this.optionsLoadingMethod()
          .then((list) => {
            let nodeList = [];
            list.forEach((item) => {
              nodeList.push(this.conversionMethod(item));
            });
            this.internalOption = nodeList;
            if(list.length>0 && this.defaultSelectedValue){
               this.selection=list[0].uri;
               this.$emit("select", this.selection);
            }
            
            callback(null, this.internalOption);
            this.$opensilex.enableLoader();
          })
          .catch(this.$opensilex.errorHandler);
      } else if (this.options) {
        this.internalOption = this.options;
        callback(null, this.internalOption);
      } else {
        this.internalOption = [];
        callback(null, this.internalOption);
      }
    }
  }

  totalCount = -1;
  resultCount = 0;

  created() {
    let self = this;
    this.debounceSearch = this.debounce(function (query, callback) {
      self.$opensilex.disableLoader();
      if (query == "") {
        query = ".*";
      }
      self
        .searchMethod(query, 0, self.resultLimit)
        .then((http) => {
          let list = http.response.result;
          self.totalCount = http.response.metadata.pagination.totalCount;
          if (http.response.size && http.response.size != list.length) {
            self.resultCount = http.response.size;
          } else {
            self.resultCount = list.length;
          }
          
          let nodeList = [];
          list.forEach((item) => {
            nodeList.push(self.conversionMethod(item));
          });
          callback(null, nodeList);
          self.$opensilex.enableLoader();
        })
        .catch(self.$opensilex.errorHandler);
    }, 300);
  }

  debounceSearch;

  refresh(){ 
      this.$opensilex.disableLoader();
         let query = ".*";
       this
        .searchMethod(query, 0, this.resultLimit)
        .then((http) => {
          let list = http.response.result;
          this.totalCount = http.response.metadata.pagination.totalCount;
          this.resultCount = list.length;
          let nodeList = [];
          list.forEach((item) => {
            nodeList.push(this.conversionMethod(item));
          });
           this.$opensilex.enableLoader();
        })
        .catch(this.$opensilex.errorHandler);
 
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

  selectedNodes;

  setCurrentSelectedNodes(values) {
    this.selectedNodes = values;
  }

  showModal() {
    let searchModal: any = this.$refs.searchModal;
    searchModal.show();
  }

  updateValues(selectedValues) {
    if (this.conversionMethod && selectedValues) {
      this.selection = selectedValues.map((item) =>
        this.conversionMethod(item)
      );
    } else {
      this.selection = selectedValues;
    }
  }
  showDetails() {
    this.detailVisible != this.detailVisible;
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
</style>

<i18n>
en:
  SelectorForm:
    refineSearchMessage: "{0}/{1} results displayed, please refine your search..."
    showDetails : "Show details"
    hideDetails : "Hide details"
  
fr:
  SelectorForm:
    refineSearchMessage: "{0}/{1} résultats affichés, merci de préciser votre recherche..."
    showDetails : "Afficher les détails"
    hideDetails : "Masquer les détails"

</i18n>
