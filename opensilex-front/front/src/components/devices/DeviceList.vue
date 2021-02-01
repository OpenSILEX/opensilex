<template>
  <div>
    <opensilex-SearchFilterField
      @search="updateFilter()"
      @clear="resetFilters()"
      withButton="false"
    >
      <template v-slot:filters>
        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DeviceList.filter.namePattern')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.namePattern"
            placeholder="DeviceList.filter.namePattern-placeholder"
          ></opensilex-StringFilter>
        </div>

        <div class="col col-xl-3 col-sm-6 col-12">
          <opensilex-TypeForm
            :type.sync="filter.rdf_type"
            :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
            placeholder="DeviceList.filter.rdf_type-placeholder"
          ></opensilex-TypeForm>
        </div>

        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DeviceList.filter.start_up')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.start_up"
            placeholder="DeviceList.filter.start_up-placeholder"
            type="number"
          ></opensilex-StringFilter>
        </div>

      </template>
    </opensilex-SearchFilterField>

    <opensilex-TableAsyncView
          ref="tableRef"
          :searchMethod="searchDevices"
          :fields="fields"
          defaultSortBy="label"
          :isSelectable="true"
          labelNumberOfSelectedRow="DeviceList.selected"
        >
          <template v-slot:cell(uri)="{data}">
            <opensilex-UriLink :uri="data.item.uri"
            :value="data.item.name"
            :to="{path: '/device/details/'+ encodeURIComponent(data.item.uri)}"
            ></opensilex-UriLink>
          </template>

          <template v-slot:row-details>
          </template>

          <template v-slot:cell(actions)="{data}">
            <b-button-group size="sm">
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
                @click="editDevice(data.item.uri)"
                label="DeviceList.update"
                :small="true"
              ></opensilex-EditButton>
            </b-button-group>
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
    namePattern: undefined,
    rdf_type: undefined,
    start_up: undefined
  };

  resetFilters() {
    this.filter = {
      namePattern: undefined,
      rdf_type: undefined,
      start_up: undefined
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
      key: "rdf_type_name",
      label: "DeviceList.rdf_type",
      sortable: true
    },
    {
      key: "start_up",
      label: "DeviceList.start_up",
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
      this.filter.namePattern, // namePattern filter
      this.filter.rdf_type, // rdfTypes filter
      this.filter.start_up, // year filter
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
    rdf_type: Device Type 
    start_up: Start up 
    update: Update Device
    delete: Delete Device
    selected: Devices
    facility: Facility

    filter:
      namePattern: Name
      namePattern-placeholder: Enter name
      rdf_type: Type
      rdf_type-placeholder: Select a device type
      start_up: Start up
      start_up-placeholder: Enter year

fr:
  DeviceList:
    uri: URI
    name: Nom
    rdf_type: Type du dispositif
    start_up: Date d'obtention
    update: Editer le dispositif
    delete: Supprimer le dispositif
    selected: Dispositifs
    facility: Facility

    filter:
      namePattern: Nom
      namePattern-placeholder: Entrez un nom
      rdf_type: Type
      rdf_type-placeholder: Selectionner un type de dispositif
      start_up: Date d'obtention
      start_up-placeholder: Entrer une année
</i18n>
