<template>
    <div>
        <opensilex-StringFilter
            :filter.sync="nameFilter"
            @update="updateFilters()"
            placeholder="GroupVariableList.label-filter-placeholder"
        ></opensilex-StringFilter>

        <opensilex-PageContent>
            <template>
               <opensilex-TableAsyncView
                  ref="tableRef"
                  :searchMethod="searchVariablesGroups"
                  :fields="fields"
                  defaultSortBy="name"
                  :isSelectable="isSelectable"
                  :maximumSelectedRows="maximumSelectedRows"
                  labelNumberOfSelectedRow="GroupVariableList.selected"
                  :iconNumberOfSelectedRow="iconNumberOfSelectedRow">

                <template v-slot:cell(name)="{data}">{{ data.item.name }}</template>
                <template v-slot:cell(description)="{data}">{{ data.item.description }}</template>

              </opensilex-TableAsyncView>
            </template>
        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { VariablesService } from "opensilex-core/index";

@Component
export default class GroupVariablesList extends Vue {
  $opensilex: any;
  $service: VariablesService;
  $store: any;
  $route: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  @Prop({
    default: true
  })
  isSelectable;

  @Prop({
    default: true
  })
  noActions;

  @Prop()
  maximumSelectedRows;

  @Prop()
  iconNumberOfSelectedRow;

  private nameFilter: any = "";
  private variables: any;

  @Ref("tableRef") readonly tableRef!: any;

  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
  }
  onItemSelected(row) {
    this.tableRef.onItemSelected(row);
  }

  searchVariablesGroups(options) {
    return this.$service.searchVariablesGroups(
      this.nameFilter,
      this.variables,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  updateFilters() {
    this.$opensilex.updateURLParameter("name", this.nameFilter, "");
    this.refresh();
  }

  resetSearch() {
    this.nameFilter = "";
    this.$opensilex.updateURLParameter("name", undefined, undefined);
    this.refresh();
  }

  refresh() {
    this.tableRef.refresh();
  }

  created() {
    let query: any = this.$route.query;
    if (query.name) {
      this.nameFilter = decodeURIComponent(query.name);
    }
    this.$opensilex.disableLoader();
    this.$service = this.$opensilex.getService("opensilex.VariablesService");
  }

  getSelected() {
    return this.tableRef.getSelected();
  }

  get fields() {
    let tableFields = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true
      },
      {
        key: "description",
        label: "component.common.description",
        sortable: false
      }
    ];
    return tableFields;
  }
}
</script>


<style scoped lang="scss">
</style>

<i18n>
en:
    GroupVariableList:
        label-filter: Search variables group
        label-filter-placeholder: "Search variables groups."
        selected: Selected variables groups
fr:
    GroupVariableList:
        label-filter: Chercher un groupe de variables
        label-filter-placeholder: "Rechercher des groupes de variables."
        selected: Groupes de variables sélectionnés
</i18n>