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

        <opensilex-CreateButton
            label="OntologyCsvImporter.import"
            @click="showCsvForm"
        ></opensilex-CreateButton>

        <opensilex-DeviceCsvForm
            v-if="renderCsvForm"
            ref="csvForm"
            @csvImported="onImport"
        ></opensilex-DeviceCsvForm>

      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-DeviceList
          ref="deviceList"          
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
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import DeviceCsvForm from "./csv/DeviceCsvForm.vue";
import DeviceList from "./DeviceList.vue";
import DeviceForm from "./DeviceForm.vue";

@Component
export default class Device extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $router: VueRouter;
  service: DevicesService;

  renderCsvForm = false;
  @Ref("csvForm") readonly csvForm!: DeviceCsvForm;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("deviceList") readonly deviceList!: DeviceList;
  @Ref("deviceForm") readonly deviceForm!: DeviceForm;
  // @Ref("deviceDetails") readonly deviceDetails!: any;
  // @Ref("deviceAttributesForm") readonly deviceAttributesForm!: any;

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

  showCsvForm() {
      this.renderCsvForm = true;
      this.$nextTick(() => {
          this.csvForm.show();
      });
  }

  onImport(response) {
    this.deviceList.refresh();
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
