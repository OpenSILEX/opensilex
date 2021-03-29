<template>
  <div>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="resetFilters()"
      withButton="false"
      :showAdvancedSearch="true"
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
      <template v-slot:advancedSearch>
        <opensilex-FilterField>
          <opensilex-StringFilter
            :filter.sync="filter.metadataKey"
            label="attribute key"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
        <opensilex-FilterField>
          <opensilex-StringFilter
            :filter.sync="filter.metadataValue"
            label="attribute value"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
      </template>     
    </opensilex-SearchFilterField>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDevices"
      :fields="fields"
      defaultSortBy="name"
      :isSelectable="true"
      labelNumberOfSelectedRow="DeviceList.selected"
    >

    <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
      <b-dropdown
        dropright
        class="mb-2 mr-2"
        :small="true"
        :disabled="numberOfSelectedRows == 0"
        text=actions>
          <b-dropdown-item-button    
            @click="createDocument()"
          >{{$t('component.common.addDocument')}}</b-dropdown-item-button>
          <b-dropdown-item-button
            @click="exportDevices()"
          >{{$t('DeviceList.export')}}</b-dropdown-item-button>
          <b-dropdown-item-button
            disabled
            @click="addVariable()"
          >{{$t('DeviceList.addVariable')}}</b-dropdown-item-button>
          <b-dropdown-divider></b-dropdown-divider>
          <b-dropdown-item-button 
            disabled
          >{{$t('DeviceList.addEvent')}}</b-dropdown-item-button>
          <b-dropdown-item-button 
            disabled
          >{{$t('DeviceList.addAnnotation')}}</b-dropdown-item-button>         
          <b-dropdown-item-button 
            disabled
          >{{$t('DeviceList.addMove')}}</b-dropdown-item-button>  
          <b-dropdown-divider></b-dropdown-divider>
          <b-dropdown-item-button 
            disabled
          >{{$t('DeviceList.showMap')}}</b-dropdown-item-button>
      </b-dropdown>
    </template>

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
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_DELETE_ID)"
            label="DeviceList.delete"
            :small="true"
            @click="deleteDevice(data.item.uri)"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  
    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-settings"
    ></opensilex-ModalForm>

    <opensilex-ModalForm
      ref="deviceForm"
      component="opensilex-DeviceForm"
      editTitle="update"
      icon="ik#ik-user"
      modalSize="lg"
      @onUpdate="refresh()"
    ></opensilex-ModalForm>

    <opensilex-VariableModalList
      label="label"
      ref="deviceVarForm"
      :isModalSearch="true"
      :selected.sync="variablesSelected"
      :required="true"
      :multiple="true"
      @click="editDeviceVar()"
    ></opensilex-VariableModalList>      
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import { DevicesService, DeviceGetDetailsDTO, VariableGetDTO} from "opensilex-core/index";
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
  @Ref("deviceVarForm") readonly deviceVarForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  filter = {
    namePattern: undefined,
    rdf_type: undefined,
    start_up: undefined,
    existence_date: undefined,
    brand: undefined,
    model: undefined,
    metadataKey: undefined,
    metadataValue: undefined
  };

  exportFilter = {
    namePattern: undefined,
    rdf_type: undefined,
    start_up: undefined,
    existence_date: undefined,
    brand: undefined,
    model: undefined,
    serial_number: undefined,
    metadata: undefined
  }

  resetFilters() {
    this.filter = {
      namePattern: undefined,
      rdf_type: undefined,
      start_up: undefined,
      existence_date: undefined,
      brand: undefined,
      model: undefined,
      metadataKey: undefined,
      metadataValue: undefined
    };
    this.tableRef.selectAll = false;
    this.tableRef.onSelectAll();

    /*this.exportFilter = {
      namePattern: undefined,
      rdf_type: undefined,
      start_up: undefined,
      existence_date: undefined,
      brand: undefined,
      model: undefined,
      serial_number: undefined,
      metadata: undefined
    }*/
    this.refresh();
  }

  created() {
    let query: any = this.$route.query;
    this.service = this.$opensilex.getService("opensilex.DevicesService");
  }

  editDevice(uri: string) {
    this.service
      .getDevice(uri)
      .then((http: HttpResponse<OpenSilexResponse<DeviceGetDetailsDTO>>) => {
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
          metadata: device.metadata,
          relations: device.relations
        };
        this.deviceForm.showEditForm(form);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteDevice(uri: string) {
    this.service
      .deleteDevice(uri)
      .then(() => {
        this.refresh();
        this.$emit("onDelete", uri);
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
    //this.updateExportFilters();
    return this.service.searchDevices(
      this.filter.namePattern, // namePattern filter
      this.filter.rdf_type, // rdfTypes filter
      this.filter.start_up, // year filter
      this.filter.existence_date, // existence filter
      this.filter.brand, // brandPattern filter
      this.filter.model, // model filter
      undefined, // snPattern filter
      this.addMetadataFilter(), //metadata filter
      options.orderBy,
      options.currentPage,
      options.pageSize,
    );
  }

  exportDevices() {
    let path = "/core/devices/exportList";
    let today = new Date();
    let filename = "export_devices_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, '0') + String(today.getDate()).padStart(2, '0');

    var exportList = []
    for (let select of this.tableRef.getSelected()) {
      exportList.push(select.uri);
    }
    this.$opensilex.downloadFilefromService(path, filename, "csv", {devices_list: exportList});
  }

  // addVariable() {
  //   this.deviceVarForm.show();
  // }
  
  // variablesSelected = []; 

  // editDeviceVar() {
  //   console.log("result var" + this.variablesSelected);
  //   for (let select of this.tableRef.getSelected()) {
  //     this.variablesSelected.push(select.uri);
  //   }
  // }

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

  addMetadataFilter() {
    let metadata = undefined;
    if (this.filter.metadataKey != undefined && this.filter.metadataKey != ""
    && this.filter.metadataValue != undefined && this.filter.metadataValue != "") {
      metadata = '{"' + this.filter.metadataKey + '":"' + this.filter.metadataValue + '"}'
      return metadata;
    }
  }

  updateExportFilters() {
    this.exportFilter.namePattern = this.filter.namePattern;
    this.exportFilter.rdf_type = this.filter.rdf_type;
    this.exportFilter.start_up = this.filter.start_up;
    this.exportFilter.existence_date = this.filter.existence_date;
    this.exportFilter.brand = this.filter.brand;
    this.exportFilter.model = this.filter.model;
    this.exportFilter.serial_number = undefined;
    this.exportFilter.metadata = this.addMetadataFilter();
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
    selected: Selected devices
    facility: Facility
    addVariable: Add variable
    export: Export Device list
    alertSelectSize: The selection has too many lines, 1000 lines maximum
    addEvent: Add event
    addAnnotation: Add annotation
    addMove: Move
    showMap: Show in a map


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
    selected: Dispositifs selectionnés
    facility: Facility
    addVariable: Ajout de variable
    export: Exporter la liste
    alertSelectSize: La selection contient trop de ligne, 1000 lignes maximum
    addEvent: Ajouter un évènement
    addAnnotation: Ajouter une annotation
    addMove: Déplacement
    showMap: Afficher sur une carte

    filter:
      namePattern: Nom
      namePattern-placeholder: Entrez un nom
      rdfTypes: Type
      rdfTypes-placeholder: Sélectionner un type de dispositif
      start_up: Date d'obtention
      start_up-placeholder: Entrer une année
      brand: Marque 
      brand-placeholder: Entrer une marque
      model: Modèle constructeur
      model-placeholder: Entrer le nom du modèle constructeur
</i18n>

