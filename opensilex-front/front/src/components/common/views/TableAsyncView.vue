<template>
  <opensilex-Overlay :show="isSearching && !isGlobalLoaderVisible">
    <div class="card">
      <div v-if="isSelectable && tableRef" class="card-header row clearfix">
        <div class="col col-sm-12">
            <h3 class="d-inline mr-1">
              <opensilex-Icon :icon="iconNumberOfSelectedRow" class="title-icon" />
              {{$t(labelNumberOfSelectedRow)}}
            </h3>
            <span v-if="!maximumSelectedRows" class="badge badge-pill badge-info">{{numberOfSelectedRows}}</span>
            <span v-else class="badge badge-pill badge-warning">{{numberOfSelectedRows}}/{{maximumSelectedRows}}</span>
            <slot name="selectableTableButtons" v-bind:numberOfSelectedRows="numberOfSelectedRows">></slot>
        </div>
      </div>

      <b-input-group size="sm">
        <slot name="export" v-if="this.totalRow >0" >          
        </slot>
      </b-input-group>

      <div v-if="showCount">
          <div v-if="totalRow > 0">
          <strong>
            <span class="ml-1"> {{$t('component.common.list.pagination.nbEntries', { limit : getCurrentItemLimit() ,offset : getCurrentItemOffset(), totalRow : this.totalRow})}}
              </span>
          </strong>
        </div>
        <div v-else>
          <strong>
            <span class="ml-1"> {{$t('component.common.list.pagination.noEntries')}}
              </span>
          </strong>
        </div>
      </div>
       <b-table
        ref="tableRef"
        striped
        hover
        small
        responsive
        :selectable="isSelectable"
        :select-mode="selectMode"
        primary-key="uri"
        :busy="isSearching"
        :sort-by.sync="sortBy"
        :sort-desc.sync="sortDesc"
        :items="loadData"
        :fields="fields"
        sort-icon-left
        @row-selected="onRowSelected"
        @row-clicked="onRowClicked"
        @refreshed="onRefreshed"
      >
        <template v-for="(field, index) in fields" v-slot:[getHeadTemplateName(field.key)]="data">
          <span v-if="!field.isSelect" :key="index">{{$t(data.label)}}</span>

          <label v-else :key="index">
            <input
              v-if="!maximumSelectedRows"
              type="checkbox"
              :value = true
              v-model="selectAll"
              @change="onSelectAll()"
            />
            <span v-if="!maximumSelectedRows">&nbsp;</span>
          </label>
        </template>

        <template v-for="(field, index) in fields" v-slot:[getCellTemplateName(field.key)]="data">
          <span v-if="!field.isSelect" :key="index">
            <slot :name="getCellTemplateName(field.key)" v-bind:data="data">{{data.item[field.key]}}</slot>
          </span>

          <span v-else :key="index" class="checkbox"></span>
        </template>

        <template v-slot:row-details="data">
          <slot name="row-details" v-bind:data="data"></slot>
        </template>
      </b-table>
      <b-pagination
        v-model="currentPage"
        :total-rows="totalRow"
        :per-page="pageSize"
        @change="pagination"
      ></b-pagination>
    </div>
  </opensilex-Overlay>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

@Component
export default class TableAsyncView extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  @Ref("tableRef") readonly tableRef!: any;

  @Prop()
  fields;

  @Prop()
  searchMethod;

  @Prop({
    default: true
  })
  useQueryParams;

  @Prop({
    default: "name"
  })
  defaultSortBy;

  @Prop({
    default: false
  })
  defaultSortDesc;

  @Prop({
    default: 20
  })
  defaultPageSize;

  @Prop({
    default: false
  })
  isSelectable;

  @Prop({
    default: "multi"
  })
  selectMode;

  @Prop()
  labelNumberOfSelectedRow;

  @Prop()
  iconNumberOfSelectedRow;

  @Prop({
    default: true
  })
  showCount: boolean;

  numberOfSelectedRows = 0;
  selectedRowIndex;

  @Prop()
  maximumSelectedRows;

  selectedItems = [];
  selectedItem;

  currentPage: number = 1;
  pageSize: number;
  totalRow = 0;
  sortBy;
  sortDesc = false;
  isSearching = false;
  selectAll = false;

  @Prop({
    default: 1000
  })
  selectAllLimit; 

  created() {
    if (this.isSelectable) {
      this.fields.unshift({
        key: "select",
        isSelect: true
      });
    }
    if (this.useQueryParams) {
      let query: any = this.$route.query;

      if (query.pageSize) {
        this.pageSize = parseInt(query.pageSize) || this.defaultPageSize;
      } else {
        this.pageSize = this.defaultPageSize;
      }

      if (query.sortBy) {
        this.sortBy = decodeURIComponent(query.sortBy) || this.defaultSortBy;
      } else {
        this.sortBy = this.defaultSortBy;
      }

      if (query.sortDesc) {
        this.sortDesc = query.sortDesc == "true";
      } else {
        this.sortDesc = this.defaultSortDesc;
      }
    }
  }

  get isGlobalLoaderVisible() {
    return this.$store.state.loaderVisible;
  }

  getHeadTemplateName(key) {
    return "head(" + key + ")";
  }

  getCellTemplateName(key) {
    return "cell(" + key + ")";
  }

  //first step
  // item = clicked item : We cannot unselect the item here, cause it's not selected at this time..
  onRowClicked(item) {

    const idx = this.selectedItems.findIndex(it => item.uri === it.uri);
    if (idx >= 0) {
      this.selectedItems.splice(idx, 1);
    } else {
      this.selectedItems.push(item);
    }
    this.numberOfSelectedRows = this.selectedItems.length;

    if (
      this.maximumSelectedRows &&
      this.maximumSelectedRows > 1 &&
      this.numberOfSelectedRows > this.maximumSelectedRows
    ) {
      this.selectedRowIndex = this.tableRef.sortedItems.findIndex(
        it => item == it
      );
      this.selectedItem = item;
    }
  }
  //second step
  // items = all selected items : if limit condition then unselect the item
  onRowSelected(items) {
    if (
      this.maximumSelectedRows &&
      this.maximumSelectedRows > 1 &&
      this.numberOfSelectedRows > this.maximumSelectedRows
    ) {
      this.tableRef.unselectRow(this.selectedRowIndex);
      const idx = this.selectedItems.findIndex(it => this.selectedItem == it);
      this.selectedItems.splice(idx, 1);
    }
    this.numberOfSelectedRows = this.selectedItems.length;
    this.$emit("row-selected", this.numberOfSelectedRows);
  }

  pagination() {
    this.tableRef.refresh();
  }

   refresh() {
    this.currentPage = 1;
    this.tableRef.refresh();
  }

  update() {
    this.tableRef.refresh();
  }


  onRefreshed() {
    let that = this;
    this.$emit('refreshed')
    setTimeout(function() {
      that.afterRefreshedItemsSelection();
    }, 1); //do it after real table refreshed
  }

  afterRefreshedItemsSelection() {
    this.selectedItems.forEach(element => {
      let index = this.tableRef.sortedItems.findIndex(
        it => element.uri == it.uri
      );
      if (index >= 0) {
        this.tableRef.selectRow(index);
      }
    });
  }

  //from outside the component
  onItemUnselected(item) {
    const idx = this.tableRef.sortedItems.findIndex(it => item.id == it.uri);

    if (idx >= 0) {
      this.tableRef.unselectRow(idx);
    } 
    const index = this.selectedItems.findIndex(it => item.id == it.uri);
    this.selectedItems.splice(index, 1);
    this.numberOfSelectedRows = this.selectedItems.length;
  }

  getSelected() {
    return this.selectedItems;
  }

  loadData() {
    let orderBy = [];
    if (this.sortBy) {
      let orderByText = this.sortBy + "=";
      if (this.sortDesc) {
        orderBy.push(orderByText + "desc");
      } else {
        orderBy.push(orderByText + "asc");
      }
    }

    if (this.useQueryParams) {
      this.$opensilex.updateURLParameter(
        "sortBy",
        this.sortBy,
        this.defaultSortBy
      );
      this.$opensilex.updateURLParameter(
        "sortDesc",
        this.sortDesc,
        this.defaultSortDesc
      );
      this.$opensilex.updateURLParameter(
        "pageSize",
        this.pageSize,
        this.pageSize
      );
    }

    this.$opensilex.disableLoader();
    this.isSearching = true;
    return this.searchMethod({
      orderBy: orderBy,
      currentPage: this.currentPage - 1,
      pageSize: this.pageSize
    })
      .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        this.isSearching = false;
        this.$opensilex.enableLoader();
        return http.response.result;
      })
      .catch(error => {
        this.isSearching = false;
        this.$opensilex.errorHandler(error);
      });
  }

  getCurrentItemLimit() : number {
    return (this.pageSize * (this.currentPage -1) < 0 ? 0  :  this.pageSize * (this.currentPage -1) )
  }

  getCurrentItemOffset() : number {
    return (this.pageSize * (this.currentPage ) < this.totalRow ? this.pageSize * (this.currentPage )  :  this.totalRow )
  }

  onSelectAll() {  
    if (this.selectAll) {

      if(this.totalRow > this.selectAllLimit) {
        alert(this.$t('TableAsyncView.alertSelectAllLimitSize') + this.selectAllLimit);
        this.selectAll=false;
      }
      else {
        this.selectedItems = [];

        let orderBy = [];
        if (this.sortBy) {
          let orderByText = this.sortBy + "=";
          if (this.sortDesc) {
            orderBy.push(orderByText + "desc");
          } else {
            orderBy.push(orderByText + "asc");
          }
        }

        this.searchMethod({
          orderBy: orderBy,
          currentPage: 0,
          pageSize: this.selectAllLimit
        })
        .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.selectedItems = http.response.result;
        this.numberOfSelectedRows = this.selectedItems.length;
        this.tableRef.selectAllRows();
        return this.selectedItems;
        }) 
      }
    }
    else {
      this.selectedItems = [];
      this.numberOfSelectedRows = this.selectedItems.length;
      this.tableRef.clearSelected();
    }
  }
}
</script>

<style scoped lang="scss">
table.b-table-selectable tbody tr td span {
  line-height: 24px;
  text-align: center;
  position: relative;
  margin-bottom: 0;
  vertical-align: top;
}

table.b-table-selectable tbody tr td span.checkbox,
.custom-control.custom-checkbox {
  top: -4px;
  line-height: unset;
}

.modal .custom-control-label:after,
.modal .custom-control-label:before {
  left: 0rem;
}

.custom-checkbox {
  padding-left: 12px;
}

.modal table.b-table-selectable tbody tr td span.checkbox:after,
.modal table.b-table-selectable tbody tr td span.checkbox:before {
  left: 0.75rem;
  width: 1rem;
  height: 1rem;
  content: "";
}

table.b-table-selectable tbody tr td span.checkbox:after,
table.b-table-selectable tbody tr td span.checkbox:before {
  position: absolute;
  top: 0.25rem;
  left: 0;
  display: block;
  width: 1rem;
  height: 1rem;
  content: "";
}

table.b-table-selectable tbody tr td span.checkbox:before {
  border-radius: 4px;
  pointer-events: none;
  background-color: #fff;
  border: 1px solid #adb5bd;
}

table.b-table-selectable tbody tr.b-table-row-selected td span.checkbox:before {
  color: #fff;
  border-color: #007bff;
  background-color: #007bff;
}

table.b-table-selectable tbody tr.b-table-row-selected td span.checkbox:after {
  background-image: none;
  content: "\e83f";
  line-height: 16px;
  font-family: "iconkit";
  color: #fff;
}

.title-icon {
  position:relative;
  top: -5px;
}
</style>
<i18n>
en:
  TableAsyncView:
    alertSelectAllLimitSize: The selection has too many lines for this feature, refine your search, maximum= 

fr:
  TableAsyncView:
    alertSelectAllLimitSize: La selection comporte trop de lignes pour cette fonctionnalité, affinez votre recherche, maximum= 
</i18n>
