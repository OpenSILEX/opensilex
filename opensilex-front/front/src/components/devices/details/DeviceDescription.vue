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
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_DELETE_ID)"
              label="DeviceDescription.delete"
              :small="true"
            ></opensilex-DeleteButton>

          </template>
            <template v-slot:body>
              <opensilex-UriView :uri="device.uri"></opensilex-UriView>
              <opensilex-StringView label="DeviceDescription.type" :value="device.rdf_type"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.brand" :value="device.brand"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.constructorModel" :value="device.constructor_model"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.serialNumber" :value="device.serial_number"></opensilex-StringView>
              <opensilex-StringView label="DeviceDescription.personInCharge" :uri="device.person_in_charge"></opensilex-StringView> 
              <opensilex-StringView label="DeviceDescription.startUp" :value="device.startUp"></opensilex-StringView>
            </template>
          </opensilex-Card>
      </b-col>

      <b-col sm="5">
        <opensilex-Card label="DeviceDescription.events" icon="ik#ik-clipboard">
            <template v-slot:body>

            </template>
        </opensilex-Card>
      </b-col>
      </b-row>

      <b-row>
        <b-col sm="5">
          <opensilex-Card label="DeviceDescription.localisation" icon="ik#ik-clipboard">            
          <template v-slot:rightHeader>

          </template>
            <template v-slot:body>

            </template>
          </opensilex-Card>
      </b-col>

      <b-col sm="5">
        <opensilex-Card label="DeviceDescription.variables" icon="ik#ik-clipboard">
            <template v-slot:body>

            </template>
        </opensilex-Card>
      </b-col>

      </b-row>
    </opensilex-PageContent>

  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  DeviceDTO,
  DevicesService
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

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  
  refresh() {
    this.modalRef.refresh();
  }

  device: DeviceDTO = { 
        uri: null,
        rdf_type: null,
        name: null,
        brand: null,
        constructor_model: null,
        serial_number: null,
        person_in_charge: null,
        start_up: null,
        removal: null,
        relations: null
      };
  
  created() {
    this.service = this.$opensilex.getService("opensilex.DevicesService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadDevice(this.uri);
  }

  loadDevice(uri: string) {
    this.service
      .getDevice(uri)
      .then((http: HttpResponse<OpenSilexResponse<DeviceDTO>>) => {
        this.device = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
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
    startUp: Start-up
    brand: Brand
    constructorModel: Constructor model
    serialNumber: Serial number
    personInCharge: Person in charge
    localisation: Localisation
    variables: Variables

fr:
  DeviceDescription:
    events: Événements
    title: Dispositif
    description: Description
    uri: URI
    name: Nom
    type: Type
    startUp: Date de mide en service
    brand: Marque
    constructorModel: Modèle du constructeur
    serialNumber: Numéro de série
    personInCharge: Personne en charge
    localisation: Localisation
    variables: Variables


</i18n>
