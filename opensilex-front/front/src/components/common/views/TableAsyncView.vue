<template>
  <opensilex-Overlay :show="isSearching">

    <div class="card">

      <div v-if="isSelectable && tableRef" class="card-header row clearfix">
        <div class="col col-sm-6">
          <slot name="titleSelectableTable">
            <h3 class="d-inline mr-1"><opensilex-Icon :icon="iconNumberOfSelectedRow" class="title-icon" /> {{$t(labelNumberOfSelectedRow)}}</h3>                                    
            <span class="badge badge-pill badge-info">{{numberOfSelectedRows}}</span>
          </slot>
        </div>
        <div class="col col-sm-3">
          <slot name="firstActionsSelectableTable"></slot>
        </div>
        <div class="col col-sm-3">
          <slot name="secondActionsSelectableTable"></slot>
        </div>                               
      </div>

      <b-table
        ref="tableRef"
        striped
        hover
        small
        responsive
        :selectable="isSelectable"
        primary-key="uri"
        :busy="isSearching"
        :sort-by.sync="sortBy"
        :sort-desc.sync="sortDesc"
        :items="loadData"
        :fields="fields"
        sort-icon-left
        @row-selected="onRowSelected"
      >
        <template v-for="(field, index) in fields" v-slot:[getHeadTemplateName(field.key)]="data">
          <span v-if="!field.isSelect" :key="index">{{$t(data.label)}}</span>

          <label v-else :key="index" class="custom-control custom-checkbox m-0">
              <input type="checkbox" 
                  :value="true" 
                  class="custom-control-input select_all_child"
                  v-model="selectAll"
                  @change="onSelectAll()">
              <span class="custom-control-label">&nbsp;</span>
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
        @change="tableRef.refresh()"
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

  @Prop()
  labelNumberOfSelectedRow;

  @Prop()
  iconNumberOfSelectedRow;

  numberOfSelectedRows = 0;

  currentPage: number = 1;
  pageSize: number;
  totalRow = 0;
  sortBy;
  sortDesc = false;
  isSearching = false;
  
  selectAll = false;

  created() {
    if(this.isSelectable) {
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
        this.sortBy = decodeURI(query.sortBy) || this.defaultSortBy;
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

  getHeadTemplateName(key) {
    return "head(" + key + ")";
  }

  getCellTemplateName(key) {
    return "cell(" + key + ")";
  }

  onSelectAll() {
    if(this.selectAll) {
      this.tableRef.selectAllRows();
    } else {
      this.tableRef.clearSelected();
    }
  }

  onRowSelected() {
    this.numberOfSelectedRows = this.tableRef.selectedRows.filter(value => value).length;

    if(this.numberOfSelectedRows == this.pageSize) {
      this.selectAll = true;
    } else {
      this.selectAll = false;
    }
  }

  refresh() {
    this.currentPage = 1;
    this.tableRef.refresh();
  }

  getSelected() {
    let results = new Array();
    for(let i=0; i<this.tableRef.selectedRows.length; i++) {
      if(this.tableRef.selectedRows[i]) {
        results.push(this.tableRef.sortedItems[i]);
      }
    }
    return results;
  }

  loadData() {
    this.selectAll = false;

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

.modal .custom-control-label:after, 
.modal .custom-control-label:before {
  left: 0rem;
}

.modal table.b-table-selectable tbody tr td span.checkbox:after,
.modal table.b-table-selectable tbody tr td span.checkbox:before {
  position: absolute;
  top: .75rem;
  left: .75rem;
  display: block;
  width: 1rem;
  height: 1rem;
  content: "";
}

.modal table.b-table-selectable tbody tr td span.checkbox:after,
.modal table.b-table-selectable tbody tr td span.checkbox:before {
  position: absolute;
  top: .75rem;
  left: .75rem;
  display: block;
  width: 1rem;
  height: 1rem;
  content: "";
}

table.b-table-selectable tbody tr td span.checkbox:after,
table.b-table-selectable tbody tr td span.checkbox:before {
  position: absolute;
  top: .25rem;
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
  font-family: 'iconkit';
  color: #fff;
}

</style>
