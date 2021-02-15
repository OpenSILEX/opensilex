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
            placeholder="DeviceList.filter.rdfTypes-placeholder"
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

        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DeviceList.filter.brand')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.brand"
            placeholder="DeviceList.filter.brand-placeholder"
          ></opensilex-StringFilter>
        </div>

        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DeviceList.filter.model')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.model"
            placeholder="DeviceList.filter.model-placeholder"
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
  
    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="DeviceList.add"
      editTitle="DeviceList.update"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-settings"
      @onCreate="refresh()"
      @onUpdate="refresh()"
    ></opensilex-ModalForm>

    <opensilex-ModalForm
      ref="deviceForm"
      component="opensilex-DeviceForm"
      editTitle="udpate"
      icon="ik#ik-user"
      modalSize="lg"
      @onUpdate="refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { DevicesService, DocumentsService, DeviceGetSingleDTO} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DeviceList extends Vue {
  $opensilex: any;
  service: DevicesService;
  $store: any;
  $route: any;

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("documentForm") readonly documentForm!: any;
  @Ref("deviceForm") readonly deviceForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  filter = {
    rdf_type: undefined,
    namePattern: undefined,
    start_up: undefined,
    brand: undefined,
    model: undefined
  };

  resetFilters() {
    this.filter = {
      rdf_type: undefined,
      namePattern: undefined,
      start_up: undefined,
      brand: undefined,
      model: undefined
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

  editDevice(uri: string) {
    console.debug("editDevice" + uri);
    this.service
      .getDevice(uri)
      .then((http: HttpResponse<OpenSilexResponse<DeviceGetSingleDTO>>) => {
        let device = http.response.result;
        let form = {
          uri: device.uri,
          name: device.name,
          rdf_type: device.rdf_type,
          brand: device.brand,
          constructor_model: device.constructor_model,
          serial_number: device.serial_number,
          person_in_charge: device.person_in_charge,
          start_up: device.start_up,
          removal: device.removal,
          description: device.description,
          metadata: device.metadata
        };
        this.deviceForm.showEditForm(form);
      })
      .catch(this.$opensilex.errorHandler);
  }

    fields = [
    {
      key: "uri",
      label: "DeviceList.name",
      sortable: true
    },
    {
      key: "rdf_type_name",
      label: "DeviceList.rdfTypes",
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
      this.filter.rdf_type, // rdfTypes filter
      this.filter.namePattern, // namePattern filter
      this.filter.start_up, // year filter
      this.filter.brand, // brandPattern filter
      this.filter.model, // model filter
      undefined, // snPattern filter
      options.currentPage,
      options.pageSize,
    );
  }

  createDocument() {
    this.documentForm.showCreateForm();
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
      }
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
    rdfTypes: Device Type 
    start_up: Start up 
    update: Update Device
    delete: Delete Device
    selected: Devices
    facility: Facility
    addDocument: Add document

    filter:
      namePattern: Name
      namePattern-placeholder: Enter name
      rdfTypes: Type
      rdfTypes-placeholder: Select a device type
      start_up: Start up
      start_up-placeholder: Enter year
      brand: Brand
      brand-placeholder: Enter brand
      model: Constructor model
      model-placeholder: Enter constructor model

fr:
  DeviceList:
    uri: URI
    name: Nom
    rdfTypes: Type du dispositif
    start_up: Date d'obtention
    update: Editer le dispositif
    delete: Supprimer le dispositif
    selected: Dispositifs
    facility: Facility
    addDocument: Ajouter un document

    filter:
      namePattern: Nom
      namePattern-placeholder: Entrez un nom
      rdfTypes: Type
      rdfTypes-placeholder: Selectionner un type de dispositif
      start_up: Date d'obtention
      start_up-placeholder: Entrer une année
      brand: Marque 
      brand-placeholder: Entrer une marque
      model: Modèle constructeur
      model-placeholder: Entrer le nom du modèle constructeur
</i18n>
