<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-file-text"
      title="DeviceDetails.title"
      :description="device.name"
    ></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" >       
    </opensilex-PageActions>

    <opensilex-PageContent>
      <b-row>
        <b-col sm="5">
          <opensilex-Card label="DeviceDetails.description" icon="ik#ik-clipboard">            
          <template v-slot:rightHeader>
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
              label="DeviceDetails.update"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_DELETE_ID)"
              label="DeviceDetails.delete"
              :small="true"
            ></opensilex-DeleteButton>

          </template>
            <template v-slot:body>
              <opensilex-UriView :uri="device.uri"></opensilex-UriView>
              <opensilex-StringView label="DeviceDetails.type" :value="device.rdf_type"></opensilex-StringView>
              <opensilex-StringView label="DeviceDetails.brand" :value="device.brand"></opensilex-StringView>
              <opensilex-StringView label="DeviceDetails.constructorModel" :value="device.constructor_model"></opensilex-StringView>
              <opensilex-StringView label="DeviceDetails.serialNumber" :value="device.serial_number"></opensilex-StringView>
              <opensilex-StringView label="DeviceDetails.personInCharge" :uri="device.person_in_charge"></opensilex-StringView> 
              <opensilex-StringView label="DeviceDetails.obtained" :value="device.obtained"></opensilex-StringView>
            </template>
          </opensilex-Card>
      </b-col>

      <b-col sm="5">
        <opensilex-Card label="DeviceDetails.annex">
            <template v-slot:body>

            </template>
        </opensilex-Card>
      </b-col>
      </b-row>

      <b-row>
        <b-col sm="5">
          <opensilex-Card label="DeviceDetails.localisation" icon="ik#ik-clipboard">            
          <template v-slot:rightHeader>

          </template>
            <template v-slot:body>

            </template>
          </opensilex-Card>
      </b-col>

      <b-col sm="5">
        <opensilex-Card label="DeviceDetails.variables">
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
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DeviceDetails extends Vue {
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
        obtained: null,
        date_of_last_use: null,
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
  DeviceDetails:
    annex: Annex
    title: Device
    description: Description
    uri: URI
    name: Name
    type: Type
    obtained: Obtained
    brand: Brand
    constructorModel: Constructor model
    serialNumber: Serial number
    personInCharge: Person in charge
    localisation: Localisation
    variables: Variables

fr:
  DeviceDetails:
    annex: Annexe
    title: Dispositif
    description: Description
    uri: URI
    name: Nom
    type: Type
    obtained: Date d'obtention
    brand: Marque
    constructorModel: Modèle du constructeur
    serialNumber: Numéro de série
    personInCharge: Personne en charge
    localisation: Localisation
    variables: Variables


</i18n>
