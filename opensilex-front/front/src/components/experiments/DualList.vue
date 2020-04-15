<template>
  <div>
    <!-- List filtering -->
    <!-- <b-input-group class="mt-3 mb-3" size="sm">
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
    </b-input-group> -->
    <div class="tables">
      <div class="table-left">
        <div>
          <div class="table-title">{{leftListTitle}}</div>
          <b-table
            ref="tableRef"
            striped
            hover
            small
            :items="leftTableData"
            :fields="fields"
            :sort-by.sync="sortBy"
            :sort-desc.sync="sortDesc"
            no-provider-paging
          >
            <template v-slot:head(firstName)="data">{{ $t(data.label) }}</template>

            <template v-slot:cell(selected)="data">
              <b-form-checkbox
                v-model="leftTableDataSelected[data.item.value]"
                @change="toggleRightTableSelection(data.item)"
              ></b-form-checkbox>
            </template>

            <template v-slot:cell(firstName)="data">
              {{data.item.firstName}} {{data.item.text}}           
            </template>
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
          >{{rightListTitle}} ({{rightTableData.length}})</div>
          <b-table
            id="user-selection-table"
            striped
            hover
            small
            :items="rightTableData"
            :fields="selectedFields"
            :per-page="pageSize"
            :current-page="currentSelectedPage"
          >
            <template v-slot:head(firstName)="data">{{ $t(data.label) }}</template>

            <template v-slot:cell(selected)="data">
              <b-btn
                variant="outline-danger"
                class="btn-no-border"
                size="xs"
                @click="unselectOnLeftTable(data.item)"
              >
                <font-awesome-icon icon="trash-alt" size="xs" />
              </b-btn>
            </template>

            <template v-slot:cell(firstName)="data">{{data.item.text}}</template>

            <template v-slot:cell(uri)="data">
              <a class="uri-info">{{ data.item.value }}</a>
            </template>

          </b-table>
        </div>
        <b-pagination
          class="bottom-pagination"
          v-model="currentSelectedPage"
          :total-rows="rightTableData.length"
          :per-page="pageSize"
          aria-controls="user-selection-table"
        ></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Emit, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  UserCreationDTO,
  GroupUpdateDTO,
  SecurityService,
  UserGetDTO,
  ProfileGetDTO,
  GroupUserProfileModificationDTO,
  GroupUserProfileDTO
} from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DualList extends Vue {

  @Prop()
  leftListTitle;

  @Prop()
  rightListTitle;

  $opensilex: any;
  $store: any;
  $router: VueRouter;

  leftTableDataSelected = {};

  @Prop()
  leftTableData;

  @Prop()
  rightTableData;

  service: SecurityService;

  get user() {
    return this.$store.state.user;
  }

  mounted() {
    this.clearForm();
  }

  clearForm() {
    this.currentSelectedPage = 1;
    this.currentPage = 1;
    this.pageSize = 5;
    this.totalRow = 0;
    this.sortBy = "firstName";
    this.sortDesc = false;
    this.filterPatternValue = "";
  }

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  currentPage: number = 1;
  currentSelectedPage: number = 1;
  pageSize = 5;
  totalRow = 0;
  sortBy = "firstName";
  sortDesc = false;

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    this.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  created() {
    this.initLeftTableSelection();
  }

  initLeftTableSelection(){
    // this.leftTableDataSelected = {};
    // this.leftTableData.forEach(element => {
    //     this.leftTableDataSelected[element.value] = false;
    // });
    // this.rightTableData.forEach(element => {
    //     // element of the left table which are present into the right table are selected by default
    //     this.leftTableDataSelected[element.value] = true;
    // })
  }


  fields = [
    {
      key: "selected",
      label: ""
    },
    {
      key: "firstName",
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
      key: "firstName",
      label: "component.common.name",
      sortable: true
    }
  ];

  toggleRightTableSelection(item) {
   
    console.log("toggleUserSelection :"+item.value + "," + item.text);

    if(! this.leftTableDataSelected[item.value]){
        this.leftTableDataSelected[item.value] = true;
        this.rightTableData.push(item);
    }else{
        this.unselectOnLeftTable(item);
    }
  }

  unselectOnLeftTable(item) {
    const index = this.rightTableData.findIndex(
      up => up.value == item.value
    );
    if (index > -1) {
      this.rightTableData.splice(index, 1);
    }
    this.leftTableDataSelected[item.value] = false;
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

