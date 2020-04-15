<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterPattern"
          debounce="300"
          :placeholder="$t('component.user.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterPattern" variant="primary" @click="filterPattern = ''">
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>
      </b-input-group>
    </b-input-group>
    <div class="tables">
      <div class="table-left">
        <div>
          <div class="table-title">{{$t('component.group.form-all-users-title')}}</div>
          <b-table
            ref="tableRef"
            striped
            hover
            small
            :items="loadData"
            :fields="fields"
            :sort-by.sync="sortBy"
            :sort-desc.sync="sortDesc"
            no-provider-paging
          >
            <template v-slot:head(label)="data">{{ $t(data.label) }}</template>

            <template v-slot:cell(selected)="data">
              <b-form-checkbox
                v-model="selectedInfrastructures[data.item.uri]"
                @change="toggleSelection(data.item)"
              ></b-form-checkbox>
            </template>

            <template v-slot:cell(label)="data"> {{data.item.name}}  </template>
          </b-table>
        </div>
        <b-pagination
          class="bottom-pagination"
          v-model="currentPage"
          :total-rows="totalRow"
          :per-page="pageSize"
          @change="refresh()"
        ></b-pagination>
      </div>
      <div class="table-right">
        <div>
          <div
            class="table-title"
          >{{$t('component.group.form-selected-users-title')}} ({{selectedTableData.length}})</div>
          <b-table
            id="user-selection-table"
            striped
            hover
            small
            :items="selectedTableData"
            :fields="selectedFields"
            :per-page="pageSize"
            :current-page="currentSelectedPage"
          >
            <template v-slot:head(label)="data">{{ $t(data.name) }}</template>

            <template v-slot:cell(selected)="data">
              <b-btn
                variant="outline-danger"
                class="btn-no-border"
                size="xs"
                @click="unselect(data.item)"
              >
                <font-awesome-icon icon="trash-alt" size="xs" />
              </b-btn>
            </template>

            <template v-slot:cell(label)="data">{{data.item}}</template>

          </b-table>
        </div>
        <b-pagination
          class="bottom-pagination"
          v-model="currentSelectedPage"
          :total-rows="selectedTableData.length"
          :per-page="pageSize"
          aria-controls="user-selection-table"
        ></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  InfrastructuresService,
  ResourceTreeDTO,
} from "opensilex-core/index";

import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class InfrastructuresListSelector extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  selectedInfrastructures = {};

  @Prop()
  selectedTableData;

  service: InfrastructuresService;

  mounted() {
    this.clearForm();
  }

  clearForm() {
    this.currentSelectedPage = 1;
    this.currentPage = 1;
    this.pageSize = 5;
    this.totalRow = 0;
    this.sortBy = "label";
    this.sortDesc = false;
    this.filterPatternValue = "";
    this.selectedInfrastructures = {};
  }
 
  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  currentPage: number = 1;
  currentSelectedPage: number = 1;
  pageSize = 5;
  totalRow = 0;
  sortBy = "label";
  sortDesc = false;

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    this.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  async created() {
    this.service = this.$opensilex.getService("opensilex.InfrastructuresService");
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

    return this.service
      .searchInfrastructuresTree(
        this.filterPattern
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  fields = [
    {
      key: "selected",
      label: ""
    },
    {
      key: "label",
      label: "component.common.name",
      sortable: true
    }
  ];

  selectedFields = [
    {
      key: "selected",
      label: ""
    },
    {
      key: "label",
      label: "component.common.name",
      sortable: true
    }
  ];

  toggleSelection(infra) {
  
    if (!this.selectedInfrastructures[infra.uri]) {
      this.selectedInfrastructures[infra.uri] = true;
      this.selectedTableData.push(infra.uri);
    } else {
      this.unselect(infra.uri);
    }
  }

  unselect(uri) {
    const index = this.selectedTableData.findIndex(
      up => up == uri
    );
    if (index > -1) {
      this.selectedTableData.splice(index, 1);
    }
    this.selectedInfrastructures[uri] = false;
  }
}
</script>

<style scoped lang="scss">
.tables {
  display: flex;
}

.table-left,
.table-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.table-left > div:first-child,
.table-right > div:first-child {
  flex: 1;
  flex-direction: column;
  flex-grow: 1;
}

.table-left {
  margin-right: 5px;
}

.table-right {
  margin-left: 5px;
}

.profile-selector {
  height: 20px;
  line-height: 15px;
  padding-top: 0;
  padding-bottom: 0;
}

.table-title {
  font-weight: bold;
  text-align: center;
}

.btn-xs {
  width: 25px;
  height: 25px;
  padding: 0;
}
</style>

