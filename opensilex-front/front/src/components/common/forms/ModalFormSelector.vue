<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-spinner small label="Small Spinning" v-if="loading"></b-spinner>
      <input :id="field.id" type="hidden" :value="hiddenValue" />

      <b-input-group class="select-button-container">

        <treeselect
          class="multiselect-popup modalSearchLabel"
          :multiple="true"
          :openOnClick="openOnClick"
          :searchable="false"
          :clearable="clearable"
          valueFormat="object"
          :value="selectedValues"
          :placeholder="$t(placeholder)"
          :show-count="showCount"
          @input="clearIfNeeded"
          @deselect="onTreeselectDeselect"
          @open="showModal"
          :limit="limit"
          @keyup.enter.native="onEnter"
        >
          <template v-slot:option-label="{ node }">
            <slot name="option-label" v-bind:node="node">{{ node.label }}</slot>
          </template>

          <template v-slot:value-label="{ node }">
            <slot name="value-label" v-bind:node="node"> <div class="modalSearchLabel" :title="node.label">{{ node.label }}</div></slot>
          </template>

        </treeselect>

        <!-- MODAL CONDITION  -->
        <b-input-group-append>
          <b-button class="createButton greenThemeColor" @click="showModal">>></b-button>
        </b-input-group-append>
      </b-input-group>

      <!-- MODAL CONDITION  -->
      <component
        ref="searchModal"
        v-bind="modalComponentProps"
        :is="modalComponent"
        :maximumSelectedRows="maximumSelectedItems"
        :searchFilter.sync="searchModalFilter"
        :withAssociatedData="withAssociatedData"

        :experiment="experiment"
        :objects="objects"
        :devices="devices"

        @onClose="$emit('onClose')"
        @close='$emit("close")'
        @onValidate="onValidate"
        @shown="onModalSearchShown"
        @clear='$emit("clear")'
        @select="onSelect(conversionMethod($event))"
        @unselect="onModalComponentUnselect(conversionMethod($event))"
        @selectall="onSelectAll"
        @hide='$emit("hide")'
        class="isModalSearchComponent"
      ></component>

    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Watch, Ref } from "vue-property-decorator";
import Vue from "vue";
import AsyncComputedProp from "vue-async-computed-decorator";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

export interface SelectableItem {
  id: string,
  label: string,
  isDisabled?: boolean
}

@Component
export default class ModalFormSelector extends Vue {

  //#region Plugins and Datas
    $opensilex: OpenSilexVuePlugin;
    currentValue;
    loading = false;
    detailVisible: boolean = false;

    // confirmed modal selection
    selectedCopie = [];
    // temporary modal selection
    selectedTmp = [];

    firstTimeOpening = false;

     /**
     * Refresh key for the Treeselect component. Used by the {@link refresh} method.
     */
    treeselectRefreshKey: number = 0;



    get hiddenValue() {
      if (this.multiple && Array.isArray(this.selection)) {
        if (this.selection.length > 0) {
          return this.selection.join(",");
        } else {
          return "";
        }
      } else {
        if (this.selection) {
          return this.selection;
        }
      }
      return "";
    }
  //#endregion

  //#region Refs
    @Ref("searchModal") readonly searchModal!: any;
    @Ref("treeref") readonly  treeref!: any;
  //#endregion

  //#region Props
    @PropSync("selected")
    selection;

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

    @Prop()
    searchMethod;


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

    @Prop()
    modalComponentProps: {[key: string]: any};

    @PropSync("filter")
    searchModalFilter;

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
  //#endregion


  //#region Computed
    @Watch("selection")
    onSelectionChange() {
      this.currentValue = null;
    }

    @AsyncComputedProp()
    selectedValues(): Promise<any> {
      return new Promise((resolve, reject) => {
          if (!this.selection || this.selection.length == 0) {
              this.firstTimeOpening = false;
              resolve([]);
          } else if (this.currentValue) {
            resolve(this.currentValue);
          } else {
            //Set table async view's checked items
            this.$nextTick(()=> {
              //Set selectedTmp and selectedCopie and table async view's initially selected items
              if( this.firstTimeOpening ){
                if(this.searchModal.setInitiallySelectedItems){
                  this.searchModal.setInitiallySelectedItems(this.selectedInJsonFormat);
                }
                this.firstTimeOpening = false;
                if( this.selectedInJsonFormat ){
                  this.selectedTmp = this.selectedInJsonFormat.map(e => this.conversionMethod(e));
                  this.selectedCopie = this.selectedInJsonFormat.map(e => this.conversionMethod(e));
                }
              }
              let nodeList = [];
              this.selectedTmp.forEach((item) => {
                nodeList.push(this.conversionMethod(item));
              });
              this.currentValue = nodeList;
              if (this.loading) {
                this.loading = false;
              }
              //if there are items initially selected, send the event "onValidate", without the need to open and validate in the modal, to send data to the form
              if(this.selectedInJsonFormat && this.currentValue !== []){
                this.$emit('onValidate', this.selectedCopie);
              }
              resolve(this.currentValue);
            });
          }
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
          } 
      });
    }
  //#endregion

  //#region Methods
    setSelectorToFirstTimeOpen(){
      this.firstTimeOpening = true;
    }

    clearSelectedModal() {
      this.selectedTmp.forEach((item) => {
        this.searchModal.unSelect(item);
      });
      this.selectedCopie = []
      this.selectedTmp = [];
    }

    clearIfNeeded(values) {
      if (this.multiple) {
        if (values.length == 0) {
          this.selection.splice(0, this.selection.length);
          this.clearSelectedModal();
          this.$emit("clear");
          return;
        }
      } else if (!values) {
        this.selection = undefined;
        this.clearSelectedModal();
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
      this.refreshModalSearch();
    }

    created() {
      let self = this;
      this.debounceSearch = this.debounce(function (query, callback) {
        self.$opensilex.disableLoader();
        self
          .searchMethod(query, 0, self.resultLimit)
          .then((http) => {
            let list = http.response.result;     
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

    /**
    * Refreshes the treeselect component & clears its cache.
    */
    refresh(){
      this.treeselectRefreshKey += 1;
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

    updateModal() {
      // unselect temporary items that are not in confirmed selection
      //This line creates a new array containing elements from selectedTmp that aren't in selectedCopie
      let difference = this.selectedTmp.filter(x => !this.selectedCopie.some(el => el.uri === x.uri));
      difference.forEach((item) => {
        this.searchModal.unSelect(item);
      });
      // reselect previously confirmed items that are not in temporary selection
      difference = this.selectedCopie.filter(x => !this.selectedTmp.some(el => el.uri === x.uri));
      difference.forEach((item) => {
        this.searchModal.selectItem(item);
      });
      // reset temporary selection
      this.selectedTmp = this.selectedCopie.slice();
    }

    showModal() {
      let searchModal: any = this.$refs.searchModal;
      this.updateModal();
      searchModal.show();
    }

    refreshModalSearch() {
      if (this.searchModal) {
        this.searchModal.refresh();
      }
    }

    deselect(item) {
      // copy selected items in local variable to wait validate action and then, change the selection
      this.selectedTmp = this.selectedTmp.filter((value) => value.id !== item.id);
      if (this.multiple) {
        this.selection = this.selection.filter((id) => id !== item.id);
      } else {
        this.selection = null;
      }
      this.$emit("deselect", item);
    }
  //#endregion

  //#region Events Handlers
    onSelect(value) {
      // copy selected items in local variable to wait validate action and then, change the selection
      this.selectedTmp.push(value);
      this.$emit("select", value, this.selectedTmp);
      if (this.multiple) {
        this.selection.push(value.id);
      } else {
        this.selection = value.id;
      }
      this.$emit("select", value, this.selectedTmp);
    }

    onModalComponentUnselect(item) {
      this.deselect(item)
    }

    onEnter() {
      this.$emit("handlingEnterKey")
    }

    onValidate() {
      if(this.selectedTmp == null || this.selectedTmp.length == 0) {
        this.loading = false;
      } else {
        this.loading = true;
      }
      this.selectedCopie = this.selectedTmp.slice();
      setTimeout(() => { // fix :  time to close the modal .
        this.selection = this.selectedCopie.map(value => value.id);
        this.$emit('onValidate', this.selectedCopie);
      }, 400);
    }
    
    onTreeselectDeselect(item) {
      this.deselect(item);
      this.selectedCopie = this.selectedTmp.slice();
      this.searchModal.unSelect(item);
    }

    onSelectAll(selectedValues) {
      if(selectedValues){
        // copy selected items in local variable to wait validate action and then, change the selection
        // Don't push to selected temp if the conversion method failed
        for(let next of selectedValues){
          let convertedNext = this.conversionMethod(next);
          if(convertedNext.label){
            this.selectedTmp.push(convertedNext);
          }
        }
      }
      else {
        this.selectedTmp = null;
      }
    }

    close(field) {
      if (field.validator) {
        this.$nextTick(() => field.validator.validate());
      }
    }

    onModalSearchShown() {
      this.$emit("shown");
      this.searchModal.refreshWithKeepingSelection();
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

.modalSearchLabel {
 white-space: normal;
  text-overflow: ellipsis;
  overflow: hidden;
  width: 170px;
}
</style>

<i18n>
</i18n>
