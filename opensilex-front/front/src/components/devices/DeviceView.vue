<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-thermometer"
      title="Device.title"
      description="Device.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions
      v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
    >
      <template v-slot>
        <opensilex-CreateButton 
          @click="goToDeviceCreate"
          label="Device.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-DeviceList
          ref="deviceList"
          v-bind:credentialsGroups="credentialsGroups"
        ></opensilex-DeviceList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "../../lib/HttpResponse";
// @ts-ignore
import { DevicesService, DeviceCreationDTO } from "opensilex-core/index";
import VueRouter from "vue-router";

@Component
export default class Device extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  service: DevicesService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("deviceList") readonly deviceList!: any;
  @Ref("deviceForm") readonly deviceForm!: any;
  @Ref("deviceDetails") readonly deviceDetails!: any;
  @Ref("deviceAttributesForm") readonly deviceAttributesForm!: any;

  created() {
    this.service = this.$opensilex.getService("opensilex.DevicesService");
  }

  goToDeviceCreate(){    
    this.$store.commit("storeReturnPage", this.$router);
    this.$router.push({ path: '/devices/create' });
  }
  
  callCreateDeviceService(form: DeviceCreationDTO, done) {
    done(
      this.service
        .createDevice(false,form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          this.deviceList.refresh();
        })
    );
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  Device:
    title: Device
    description: Manage Device
    add: Add device
    update: Update device
    delete: Delete device
fr:
  Device:
    title: Dispositif
    description: Gestion des dispositifs
    add: Ajouter un dispositif
    update: Editer un dispositif
    delete: Supprimer un dispositif
</i18n>
