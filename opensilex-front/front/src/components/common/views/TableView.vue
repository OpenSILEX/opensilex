<template>
  <div>
     <b-input-group size="sm">
        <slot name="export" v-if="this.computedTotalRows > 0" >
        </slot>
      </b-input-group>
    <div class="card">
      <div v-if="globalFilterField">
        <div>
          <opensilex-StringFilter
            :filter.sync="filter"
            :placeholder="$t(filterPlaceholder)"
            :debounce="300"
            :lazy="false"
          ></opensilex-StringFilter>
        </div>
      </div>
      <div v-if="showCount">
        <div v-if="totalRows > 0">
          <strong>
            <span class="ml-1"> {{
                $t('component.common.list.pagination.nbEntries', {
                  limit: getCurrentItemLimit(),
                  offset: getCurrentItemOffset(),
                  totalRow: this.totalRows
                })
              }}
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
          :current-page="currentPage"
          :fields="fields"
          :filter="filter"
          :items="items"
          :per-page="pageSize"
          :sort-by="sortBy"
          :sort-desc="sortDesc"
          :selectable="selectable"
          select-mode="single"
          hover
          primary-key="uri"
          responsive
          small
          sort-icon-left
          striped
          @filtered="onFiltered"
          @row-selected="emitRowSelected"
          ref="tableRef"
      >
        <template
          v-for="(field, index) in fields"
          v-slot:[getHeadTemplateName(field.key)]="data"
        >
          <span v-if="!field.isSelect" :key="index">{{ $t(data.label) }}</span>
        </template>

        <template
          v-for="(field, index) in fields"
          v-slot:[getCellTemplateName(field.key)]="data"
        >
          <span v-if="!field.isSelect" :key="index">
            <slot :name="getCellTemplateName(field.key)" v-bind:data="data">{{
              data.item[field.key]
            }}</slot>
          </span>

          <span v-else :key="index" class="checkbox"></span>
        </template>

        <template v-slot:row-details="data">
          <slot name="row-details" v-bind:data="data"></slot>
        </template>
      </b-table>
      <b-pagination
        v-if="withPagination"
        v-model="currentPage"
        :per-page="pageSize"
        :total-rows="computedTotalRows"
      ></b-pagination>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import {BTable} from "bootstrap-vue";

@Component
export default class TableView extends Vue {

  //#region Props
  @PropSync("items", {default: []})
  private data: any[];

  @Prop()
  private readonly fields: any[];

  @Prop({default: "name"})
  private readonly defaultSortBy;

  @Prop({default: "TableView.filter.placeholder"})
  private readonly filterPlaceholder: string;

  @Prop({default: 10})
  private readonly pageSize: number;

  @Prop({default: false})
  private readonly globalFilterField: boolean;

  @Prop({default: true})
  private readonly showCount: boolean;

  @Prop({default: true})
  private readonly withPagination: boolean;

  @Prop()
  private readonly sortBy: string;

  @Prop({default: false})
  private readonly sortDesc: boolean;

  @Prop({default: false})
  private readonly selectable: boolean;
  //#endregion

  //#region Refs
  @Ref("tableRef") private readonly tableRef!: BTable;
  //#endregion

  //#region data
  private currentPage: number = 1;

  private filter: string = null;

  private totalRows: number = 1;
  //#endregion

  //#region Computed
  private get computedTotalRows() {
    return this.filter == null ? (this.totalRows = this.data.length) : this.totalRows
  }

  //#endregion

  //#region Events
  private emitRowSelected(selected: Array<any>) {
    this.$emit('row-selected', selected[0]);
  }

  //#endregion

  //#region Event handlers
  private onFiltered(filteredItems) {
    // Trigger pagination to update the number of buttons/pages due to filtering
    this.totalRows = filteredItems.length;
    this.currentPage = 1;
  }

  //#endregion

  //#region Public methods
  public selectRow(rowNumber: number) {
    this.tableRef.selectRow(rowNumber);
  }

  //#endregion

  private mounted() {
    // Set the initial number of items
    this.totalRows = this.data.length;
  }

  //#endregion

  //#region Private methods
  private getHeadTemplateName(key) {
    return "head(" + key + ")";
  }

  private getCellTemplateName(key) {
    return "cell(" + key + ")";
  }

  private getCurrentItemLimit(): number {
    return (this.pageSize * (this.currentPage - 1) < 0 ? 0 : this.pageSize * (this.currentPage - 1))
  }

  private getCurrentItemOffset(): number {
    return (this.pageSize * (this.currentPage) < this.totalRows ? this.pageSize * (this.currentPage) : this.totalRows)
  }

  //#endregion
}
</script>

<style lang="scss" scoped>
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
</style>

<i18n>
en:
  TableView:
    filter:
      placeholder: Search in this table

fr:
  TableView:
    filter:
      placeholder: Rechercher dans ce tableau
</i18n>
