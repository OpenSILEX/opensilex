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
                <template v-slot:cell(actions)="{ data }">
                    <b-button-group size="sm">
                        <opensilex-DetailButton
                        @click="loadVariablesGroupDetails(data)"
                        label="component.common.details-label"
                        :detailVisible="data.detailsShowing"
                        :small="true"
                        ></opensilex-DetailButton>
                    </b-button-group>
                </template>
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
export default class GroupVariablesListWithVariables extends Vue {
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


  loadVariablesGroupDetails(data) {
    if(!data.detailsShowing){
        this.$opensilex.disableLoader();
        this.$service.searchVariablesGroups()
        .then((http: any) => {
          let listVariables = [];
          for (let variablesGroup of http.response.result) {
            if (variablesGroup.variables != null) {
              listVariables.push({
                uri: variablesGroup.variable.uri,
                name: variablesGroup.variable.name,
              });
            }
          }
          this.variables[data.item.uri] = listVariables;
          data.toggleDetails();
          this.$opensilex.enableLoader();
        })
        .catch(this.$opensilex.errorHandler);
    } else {
      data.toggleDetails();
    }
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