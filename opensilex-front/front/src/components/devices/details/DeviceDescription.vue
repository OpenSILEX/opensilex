<template>
  <div class="container-fluid">

    <opensilex-PageContent>
      <b-row>
        <b-col sm="5">
          <opensilex-Card label="DeviceDescription.description" icon="ik#ik-clipboard">            
          <template v-slot:rightHeader>
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
              label="DeviceDescription.update"
              @click="update"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_DELETE_ID)"
              label="DeviceDescription.delete"
              :small="true"
              @click="deleteDevice(device.uri)"
            ></opensilex-DeleteButton>

          </template>
            <template v-slot:body>
              <opensilex-UriView :uri="device.uri"></opensilex-UriView>
              <opensilex-StringView label="DeviceDescription.type" :value="device.rdf_type_name"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.brand" :value="device.brand"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.constructorModel" :value="device.constructor_model"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.serialNumber" :value="device.serial_number"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.personInCharge" :uri="device.person_in_charge"></opensilex-StringView> 
              <opensilex-StringView label="DeviceDescription.start_up" :value="device.start_up"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.removal" :value="device.removal"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.description" :value="device.description"></opensilex-StringView>
            </template>
          </opensilex-Card>
      </b-col>
      <b-col sm="5">
        <opensilex-Card label="DeviceDescription.variables" icon="ik#ik-clipboard">
            <template v-slot:body>
              <opensilex-StringView label="DeviceDescription.variables" :value="device.relations">
                <span :key="relations" v-for="(relations) in device.relations"> 
                  <opensilex-UriLink 
                  :uri="relations.value" 
                  :to="{path: '/variable/details/'+ encodeURIComponent(relations.value)}"
                ></opensilex-UriLink><br>
                </span>
              </opensilex-StringView> 
            </template>
        </opensilex-Card>
      </b-col>
      <!-- <b-col sm="5">
        <opensilex-Card label="DeviceDescription.events" icon="ik#ik-clipboard">
            <template v-slot:body>

            </template>
        </opensilex-Card>
      </b-col> -->
      </b-row>

      <b-row>
        <!-- <b-col sm="5">
          <opensilex-Card label="DeviceDescription.localisation" icon="ik#ik-clipboard">            
          <template v-slot:rightHeader>

          </template>
            <template v-slot:body>

            </template>
          </opensilex-Card>
      </b-col> -->


      </b-row>

      <b-row>
        <b-col sm="5">
          <opensilex-Card label="DeviceDescription.additionalInfo" icon="ik#ik-clipboard" v-if="addInfo.length != 0">
            <template v-slot:body>
              <b-table
                ref="tableAtt"
                striped
                hover
                small
                responsive
                :fields="attributeFields"
                :items="addInfo"
              >
                <template v-slot:head(attribute)="data">{{$t(data.label)}}</template>
                <template v-slot:head(value)="data">{{$t(data.label)}}</template>
              </b-table>
            </template>
          </opensilex-Card>
        </b-col>
      </b-row>
    </opensilex-PageContent>

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
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  DevicesService,
  VariablesService,
  DeviceGetDetailsDTO
} from "opensilex-core/index";
import {
  SecurityService,
  UserGetDTO
} from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

@Component
export default class DeviceDescription extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $t: any;
  $i18n: any;
  service: DevicesService;
  uri: string = null;
  addInfo = [];

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("tableAtt") readonly tableAtt!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  
  refresh() {
    this.loadDevice(this.uri);
  }

  device: DeviceGetDetailsDTO = { 
        uri: null,
        rdf_type: null,
        name: null,
        brand: null,
        constructor_model: null,
        serial_number: null,
        person_in_charge: null,
        start_up: null,
        removal: null,
        relations: null,
        description: null,
        metadata: null
      };
  
  created() {
    this.service = this.$opensilex.getService("opensilex.DevicesService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadDevice(this.uri);
  }

  loadDevice(uri: string) {
    this.service
      .getDevice(uri)
      .then((http: HttpResponse<OpenSilexResponse<DeviceGetDetailsDTO>>) => {
        this.device = http.response.result;
        this.getAddInfo();
      })
      .catch(this.$opensilex.errorHandler);
  }

  getAddInfo() {
    this.addInfo = []
    for (const property in this.device.metadata) {
      let tableData = {
        attribute: property,
        value: this.device.metadata[property],
      };
      this.addInfo.push(tableData);
    }
  }

  attributeFields = [
    {
      key: "attribute",
      label: "DeviceDescription.attribute",
    },
    {
      key: "value",
      label: "DeviceDescription.value",
    },
  ];

  deleteDevice(uri: string) {
    this.service
      .deleteDevice(uri)
      .then(() => {
        this.$router.go(-1);
        this.$emit("onDelete", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  @Ref("deviceForm") readonly deviceForm!: any;
  update() {
    this.deviceForm.getFormRef().getAttributes(this.device);
    let device = {
        uri: this.device.uri,
        name: this.device.name,
        rdf_type: this.device.rdf_type,
        brand: this.device.brand,
        constructor_model: this.device.constructor_model,
        serial_number: this.device.serial_number,
        person_in_charge: this.device.person_in_charge,
        start_up: this.device.start_up,
        removal: this.device.removal,
        description: this.device.description,
        metadata: this.device.metadata
    }
    this.deviceForm.showEditForm(device);
  }

  
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DeviceDescription:
    events: Events
    title: Device
    description: Description
    uri: URI
    name: Name
    type: Type
    start_up: Start up
    brand: Brand
    constructorModel: Constructor model
    serialNumber: Serial number
    personInCharge: Person in charge
    localisation: Localisation
    variables: Variables
    update: Update device
    additionalInfo: Additional information
    removal: Removal
    attribute: Attribute
    value: Value

fr:
  DeviceDescription:
    events: Événements
    title: Dispositif
    description: Description
    uri: URI
    name: Nom
    type: Type
    start_up: Date de mide en service
    brand: Marque
    constructorModel: Modèle du constructeur
    serialNumber: Numéro de série
    personInCharge: Personne en charge
    localisation: Localisation
    variables: Variables
    update: Update device
    additionalInfo: Informations supplémentaires
    removal: Date de mise hors service
    attribute: Attribut
    value: Valeur

</i18n>
