<template>
  <treeselect
    ref="treeref"
    v-bind:class="{
    'multiselect-action': actionHandler,
    'multiselect-view': viewHandler, // class if viewHandler = function
    }"
    v-on="$listeners"
    :async="searchMethod != null"
    :value="selectedValues"
    valueFormat="node"
    :placeholder="$t(placeholder)"
    :flat="true"
    :default-options="searchMethod != null" 
    :load-options="loadOptions"
    :options="options || internalOption"
    :multiple="multiple"
    :disabled="disabled"
    :show-count="showCount"
    :limit="limit"
    :clearable="true"
    :disableBranchNodes="disableBranchNodes"
    @deselect="deselect"
    @select="select"
    @input="clearIfNeeded"
    @close="$emit('close')"
    @search-change="onSearchChange"
    @keyup.enter.native="onEnter"
    :key="treeselectRefreshKey"
  >
    <template v-slot:after-list>
      <slot name="after-list"></slot>
    </template>
  </treeselect>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import AsyncComputedProp from "vue-async-computed-decorator";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";

export interface SelectableItem {
  id: string,
  label: string,
  isDisabled?: boolean
}

@Component
export default class CustomTreeselect extends Vue {

  //#region Plugins and Datas
  $opensilex: any;
  treeselectRefreshKey: number = 0;
  totalCount = -1;
  resultCount = 0;
  countCache = new Map<String, { total: number , result: number}>()
  currentValue;
  internalOption = null;
  debounceSearch;
  lastSearchQuery = ".*";
  //#endregion

  //#region Props and PropSync
  @PropSync("selected")
  selection;

  @Prop()
  searchMethod;

  @Prop()
  multiple;

  @Prop()
  itemLoadingMethod;

  @Prop()
  options;

  @Prop({
    default: 10,
  })
  resultLimit;

  @Prop({default: 1})
  limit: number; // limit number of items in the input box
  
  @Prop()
  optionsLoadingMethod;

  @Prop()
  defaultSelectedValue;

  @Prop()
  placeholder: string;

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
    default: false,
  })
  showCount;

  @Prop()
  disabled: boolean;

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

  @Prop({
    default: false
  })
  disableBranchNodes: boolean;
  //#endregion

  //#region Refs
  @Ref("treeref") readonly  treeref!: any;
  //#endregion

  //#region Computed
  @Watch("selection")
  onSelectionChange() {
    this.currentValue = null;
  }

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
            // If there is a search method but no item loading method, then initial values
            // cannot be retrieved.
            console.warn("A search method was specified but no item loading method.")
            resolve(undefined);
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
  //#endregion

  //#region Events Handlers 
  select(value) {  
    if (this.multiple) {
      this.selection.push(value.id);
    } else {
      this.selection = value.id;
    }
    this.$emit("select", value);
  }

  deselect(item) { 
    if (this.multiple) {
      this.selection = this.selection.filter((id) => id !== item.id);
    } else {
        this.selection = null;
      }
    this.$emit("deselect", item);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }

  onSearchChange(searchQuery) {
    if (searchQuery == "") {
      searchQuery = ".*";
    }

    if(this.countCache.has(searchQuery)){
      this.resultCount = this.countCache.get(searchQuery).result;
      this.totalCount = this.countCache.get(searchQuery).total;
    } 
    
    this.$emit('totalCount', this.totalCount);
    this.$emit('resultCount', this.resultCount);
    this.lastSearchQuery = searchQuery;
  }
  //#endregion

  //#region Hooks
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

          // at start if the research is empty field
          // after, onSearchChange() replace lastSearchQuery by the value entered by user
          if(self.lastSearchQuery === query){
            self.totalCount = http.response.metadata.pagination.totalCount;
            if (http.response.size && http.response.size != list.length) {
              self.resultCount = http.response.size;
            } else {
              self.resultCount = list.length;
            }
          }
          this.$emit('totalCount', self.totalCount);
          this.$emit('resultCount', self.resultCount)
          self.countCache.set(query, {total : http.response.metadata.pagination.totalCount, result : list.length})
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
  //endregion

  //#region Methods
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

  public findListInTree(tree, ids, map?: Map<string, any>) {
    map = map || new Map();

    for (let item of tree) {
      if (ids.indexOf(item.id) >= 0) {
        map.set(item.id, item);
        if (map.size == ids.length) {
          break;
        }
      }

      if (item.children) {
        this.findListInTree(item.children, ids, map);
      }
      if (map.size == ids.length) {
        break;
      }
    }
    return Array.from(map.values());
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
              var URISelected = []
              list.forEach((element, index) => {
                URISelected.push(element.uri);
              });
              this.selection=URISelected;
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


  openTreeselect() {
    this.treeref.focusInput();
    this.treeref.openMenu();
    this.treeref.getInput().value = this.lastSearchQuery;
  }

  /**
   * Refreshes the treeselect component & clears its cache.
   */
  refresh(){
    this.treeselectRefreshKey += 1;
  }

  clearIfNeeded(values) {
    if (this.multiple) {
      if (values.length == 0) {
        this.selection.splice(0, this.selection.length);
        this.$emit("clear");
        return;
      }
    }
    
    else if (!values) {
      this.selection = undefined;
      this.$emit("clear");
      return;
    }

    if (this.multiple) {
      let newValues = [];
      for (let i in values) {
        newValues.push(values[i].id); // id because valueFormat="node" if we dont have a valueFormat we dont need the id
      }
      this.selection = newValues;
    }
  }
  //#endregion

}
</script>