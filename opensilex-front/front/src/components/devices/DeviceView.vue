<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-file-text"
      title="Device.title"
      description="Device.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions
      v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
    >
      <template v-slot>
        <opensilex-CreateButton label="Device.add"></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-DeviceList
          v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_READ_ID)"
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
import { DevicesService, DeviceDTO} from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse
} from "../../lib/HttpResponse";

@Component
export default class Device extends Vue {
  $opensilex: any;
  service: DevicesService;
  $store: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("deviceList") readonly deviceList!: any;

   created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.DevicesService");
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
    title: device
    description: Gestion des devices
    add: Ajouter un device
    update: Editer un device
    delete: Supprimer un device
</i18n>
