<template>
  <div>
    <opensilex-SearchFilterField
      @search="updateFilter()"
      @clear="resetFilters()"
      withButton="false"
    >
      <template v-slot:filters>
        
      </template>
    </opensilex-SearchFilterField>
<opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDevices"
      :fields="fields"
      defaultSortBy="label"
    >
      <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink :uri="data.item.uri"
        :value="data.item.name"
        ></opensilex-UriLink>
      </template>

      <template v-slot:row-details>
      </template>

      <template v-slot:cell(actions)="{data}">

      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { DevicesService, DeviceDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DeviceList extends Vue {
  $opensilex: any;
  service: DevicesService;
  $store: any;
  $route: any;

  @Ref("tableRef") readonly tableRef!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  filter = {
  };

  resetFilters() {
    this.filter = {
    };
    this.refresh();
  }

  created() {
    let query: any = this.$route.query;
    this.service = this.$opensilex.getService("opensilex.DevicesService");
  }

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

    fields = [
    {
      key: "uri",
      label: "DeviceList.name",
      sortable: true
    },
    {
      key: "rdf_type",
      label: "DeviceList.rdfType",
      sortable: true
    },
    {
      key: "obtained",
      label: "DeviceList.obtained",
      sortable: true
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  refresh() {
    this.tableRef.refresh();
  }

  searchDevices(options) {
    return this.service.searchDevices(
      undefined, //namePattern filter
      undefined, // rdfTypes filter
      undefined, // brandPattern filter
      undefined, // model filter
      undefined,
      options.currentPage,
      options.pageSize,
    );
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DeviceList:
    uri: URI
    name: Name
    rdfType: Device Type 
    obtained: Obtained 
    update: Update Device
    delete: Delete Device

    filter:

fr:
  DeviceList:
    uri: URI
    name: Nom
    rdfType: Type du dispositif
    obtained: Date d'obtention
    update: Editer le dispositif
    delete: Supprimer le dispositif

    filter:

</i18n>
