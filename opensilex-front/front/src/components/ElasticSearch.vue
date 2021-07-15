<template>

<div>
     
 <div class="container-fluid boxed-layout">
      <div class="d-flex justify-content-end">
        <div class="top-menu d-flex align-items-center">
          
          <opensilex-StringFilter
              :filter.sync="nameFilter"
                placeholder="Search"
              ></opensilex-StringFilter>
              
          <b-button v-b-modal.modal-scrollable>Show results </b-button>
          <b-modal id="modal-scrollable" size="xl" scrollable title="Search results">

            <opensilex-TableAsyncView 
              ref="tableRef"
              :searchMethod="loadData"
              :fields="fields">
            </opensilex-TableAsyncView>

        </b-modal>

      </div>
    </div>
  </div>
</div>

  


</template>

<script lang="ts">

import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";

// @ts-ignore
import { ElasticSearchService } from "../../../../opensilex-elastic/front/src/lib";

@Component
export default class ElasticSearch extends Vue {
  $opensilex: any;
  $store: any;


  service: ElasticSearchService;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

static async asyncInit($opensilex) {
await $opensilex.loadModule("opensilex-elastic");
}

    created() {
    this.service = this.$opensilex.getService("opensilex-elastic.ElasticSearchService");
    
  }

  @Prop({
    default: false
  })
  isSelectable;

  @Prop({
    default: false
  })
  noActions;

  @Prop()
  maximumSelectedRows;

   @Prop()
  searchMethod;

  @Prop({
    default: ""
  })
  nameFilter: string;
  

  get fields() {
    let tableFields: any = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true
      },
      {
      label: "Type",
      key: "type",
      sortable: true
      },
      {
      label: "Uri",
      key: "uri",
      sortable: true
      }
    ];
    
    return tableFields;
  }

  getSelected() {
    return this.tableRef.getSelected();
  }

  @Ref("tableRef") readonly tableRef!: any;
  refresh() {
    this.tableRef.selectAll = false;
    this.tableRef.onSelectAll();
    this.tableRef.refresh();
  }

  loadData(options) {
    return this.service.searchES(
      this.nameFilter,
      options.currentPage,
      options.pageSize      
    );
  }

  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
  }

}
</script>

<style scoped lang="scss">
</style>

