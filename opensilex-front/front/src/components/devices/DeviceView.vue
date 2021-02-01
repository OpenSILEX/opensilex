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
        <opensilex-CreateButton 
          @click="goToDeviceCreate"
          label="Device.add"
        ></opensilex-CreateButton>

        <opensilex-CreateButton
          label="DeviceList.addDocument"
          :small="false"
          @click="deviceList.createDocument()"
        ></opensilex-CreateButton>
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

    <opensilex-ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      editTitle="Device.update"
      createTitle="Device.add"
      icon="ik#ik-user"
      modalSize="lg"
      @onCreate="refresh()"
      @onUpdate="refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "../../lib/HttpResponse";

import { 
  DevicesService, 
  DocumentsService,
  DeviceDTO
  } from "opensilex-core/index"
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
  @Ref("documentForm") readonly documentForm!: any;

  @Prop()
    uri;

  created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.DevicesService");

    if (this.uri == null){
        this.uri = decodeURIComponent(this.$route.params.uri);
    }
  }

  goToDeviceCreate(){    
    this.$store.commit("storeReturnPage", this.$router);
    this.$router.push({ path: '/devices/create' });
  }
  
  callCreateDeviceService(form: DeviceDTO, done) {
    done(
      this.service
        .createDevice(false,form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("device created", uri);
          this.deviceList.refresh();
        })
    );
  }

  createDocument() {
    this.documentForm.showCreateForm();
  }

  initForm() {
    if(this.uri){
      return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: decodeURIComponent(this.uri),
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined
      },
      file: undefined
      }
    }
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
    addDocument: Add document
    update: Update device
    delete: Delete device
fr:
  Device:
    title: device
    description: Gestion des dispositifs
    add: Ajouter un dispositif
    addDocument: Ajout de document
    update: Editer un dispositif
    delete: Supprimer un dispositif
</i18n>
