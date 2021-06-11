<template>
  <div>
    <opensilex-TableAsyncView 
      ref="tableRef"
      :searchMethod="loadData"
      :fields="fields"
      :isSelectable="true"
    >
      <!--<template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
        <b-dropdown
          dropright
          class="mb-2 mr-2"
          :small="true"
          :disabled="numberOfSelectedRows == 0"
          text="actions"
        >
        </b-dropdown>
      </template>-->
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">

import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { GlobalSearchService } from "opensilex-core/index";

@Component
export default class Elasticsearch extends Vue {
  $opensilex: any;
  $store: any;
  //searchQuery: string = 'sunagri'


  service: GlobalSearchService;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
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

  filter = {
    year: undefined,
    name: "",
    keyword: "",
    financial: "",
  };

  reset() {
    this.filter = {
      year: undefined,
      name: "",
      keyword: "",
      financial: "",
    };
    this.refresh();
  }
  

  get fields() {
    let tableFields: any = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true
      },
      {
      label: "component.common.description",
      key: "description",
      sortable: true
      }
    ];
    if (!this.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions"
      });
    }
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
    return this.service.searcsearchES(
    "sunagri",
      options.currentPage,
      options.pageSize      
    );
  }

  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
  }

  initForm() {
    let targetURI = [];
    for (let select of this.tableRef.getSelected()) {
      targetURI.push(select.uri);
    }

    return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: targetURI,
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined
      },
      file: undefined
    };
  }
}
</script>

<style scoped lang="scss">
</style>

