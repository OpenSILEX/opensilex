<template>
  <treeselect
    ref="treeref"
    v-bind="$attrs"
    v-on="$listeners"
    :async="searchMethod != null"
    :default-options="searchMethod != null" 
    :load-options="loadOptions"
    :multiple="multiple"
    :show-count="showCount"
    @deselect="deselect"
    @select="select"
    @input="clearIfNeeded"
    @close="$emit('close')"
    @search-change="onSearchChange"
    @keyup.enter.native="onEnter"
    :key="treeselectRefreshKey"
  >
    <!-- v-bind="treeselectProps" -->
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
  $opensilex: any;
  treeselectRefreshKey: number = 0;

  @Ref("treeref") readonly  treeref!: any;

  // @Prop({ type: Object, required: true }) 
  // treeselectProps!: object;

  // @Prop() multiple;
  // @Prop() placeholder: string;
  // @Prop({ default: false }) showCount: boolean;
  // @Prop({ default: true }) clearable: boolean;
  // @Prop() disabled: boolean;
  // @Prop({ default:  1 }) limit: number;
  // @Prop({ default: "component.common.filter-search-no-result" }) noResultsText: string;
  // @Prop({ default: false }) disableBranchNodes: boolean;
  // @Prop({ default: false }) searchNested: boolean;
  // @Prop({ default: true }) flat: boolean;
  // @Prop() searchMethod;
  // @Prop() loadOptions: Function;
  // @Prop() optionsLoadingMethod;
  // @Prop() options;

currentValue;
internalOption = null;

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
  multiple;

  @Prop()
  itemLoadingMethod;

  @Prop()
  options;

  @Prop({
    default: 10,
  })
  resultLimit;

  @Prop()
  optionsLoadingMethod;

  @Prop()
  defaultSelectedValue;

  selectedTmp = [];

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

@Watch("selection")
onSelectionChange() {
  console.log("watcher selection : ", this.selection)
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
                    console.log("currentValue : ", this.currentValue)
                    console.log("nodeList ", nodeList)
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
              console.log("items ", items)
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
              console.log("loadOp this.selection ", this.selection)
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
  countCache = new Map<String, { total: number , result: number}>()


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

  debounceSearch;

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


  onSearchChange(searchQuery) {
    if (searchQuery == "") {
      searchQuery = ".*";
    }

    if(this.countCache.has(searchQuery)){
      this.resultCount = this.countCache.get(searchQuery).result;
      this.totalCount = this.countCache.get(searchQuery).total;
      this.$emit('counts', { totalCount: this.totalCount, resultCount: this.resultCount });
    }
    this.lastSearchQuery = searchQuery;
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


  select(value) {  
    if (this.multiple) {
      console.log("custom -select - value ", value)
      console.log("custom -select - this.selection Av ", this.selection)
      this.selection.push(value.id);
      console.log("custom -select - this.selection AP ", this.selection)
    } else {
      this.selection = value.id;
    }
    this.$emit("select", value);
  }

  deselect(item) { 
    if (this.multiple) {
      console.log("deselect av", this.selection)
        this.selection = this.selection.filter((id) => id !== item.id);
              console.log("deselect ap", this.selection)
    } else {
        this.selection = null;
      }
  
    this.$emit("deselect", item);
  }

  onEnter() {
      this.$emit("handlingEnterKey")
  }

  clearIfNeeded(values) {
    if (this.multiple) {
      if (values.length == 0) {
        console.log("clearIf 1er ifselection av", this.selection)
        this.selection.splice(0, this.selection.length);
                console.log("clearIf  1ier if selection ap", this.selection)
        this.$emit("clear");
        return;
      }
    } else if (!values) {
      console.log("clearIf no value ", this.selection)
      this.selection = undefined;
      this.$emit("clear");
      return;
    }

    if (this.multiple) {
      let newValues = [];
      for (let i in values) {
        newValues.push(values[i].id);
      }
        console.log("clearIf selection av", this.selection )
      this.selection = newValues;
      console.log("clearIf  selection ap ", this.selection)
    }
  }


}
</script>
