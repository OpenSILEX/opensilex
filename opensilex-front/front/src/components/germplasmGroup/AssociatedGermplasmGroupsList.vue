<template>
  <opensilex-Card label="AssociatedGermplasmGroupsList.relatedGroups" icon="fa#seedling">

    <template v-slot:body>
      <opensilex-StringFilter
          :filter.sync="filter"
          @update="updateFilters()"
          :debounce="300"
          :lazy="false"
          placeholder="AssociatedGermplasmGroupsList.nameFilter"
      ></opensilex-StringFilter>
      <opensilex-TableAsyncView
          ref="tableRef"
          :searchMethod="searchMethod"
          :fields="fields"
          defaultSortBy="name"
          :defaultPageSize="5"
      >
        <template v-slot:cell(name)="{data}">
          <opensilex-UriLink
              :uri="data.item.uri"
              :value="data.item.name"
              :to="{path: '/germplasm/group?selected='+ encodeURIComponent(data.item.uri)}"
          ></opensilex-UriLink>
        </template>
      </opensilex-TableAsyncView>
    </template>
  </opensilex-Card>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

@Component
export default class AssociatedGermplasmGroupsList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  @Ref("tableRef") readonly tableRef!: any;

  @Prop()
  searchMethod;

  @PropSync("nameFilter")
  filter

  fields = [
    {
      key: "name",
      label: "component.common.name"
    }
  ];

  updateFilters() {
    this.tableRef.refresh();
  }

}
</script>


<style scoped lang="scss">

</style>

<i18n>
en:
  AssociatedGermplasmGroupsList:
    nameFilter: Search by group name
    relatedGroups: Related groups

fr:
  AssociatedGermplasmGroupsList:
    nameFilter: Rechercher par nom de groupe
    relatedGroups: Groupes liés

</i18n>