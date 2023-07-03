<template>
    <div class="container-fluid">
        <opensilex-PageActions
            class= "pageActions"
            v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
        >
            <template v-slot>

                <opensilex-CreateButton
                    label="Device.add"
                    @click="modalForm.showCreateForm()"
                    class="createButton"
                ></opensilex-CreateButton>

                <opensilex-CreateButton
                    label="OntologyCsvImporter.import"
                    @click="showCsvForm"
                    class="createButton"
                ></opensilex-CreateButton>

                <font-awesome-icon
                    icon="question-circle"
                    class="devicesHelp"
                    v-b-tooltip.hover.top="$t('Device.devices-help')"
                />

                <opensilex-DeviceModalForm
                    ref="modalForm"
                    @onCreate="displayAfterCreation($event)"
                ></opensilex-DeviceModalForm>

                    <opensilex-DeviceCsvForm
                        v-if="renderCsvForm"
                        ref="csvForm"
                        @csvImported="deviceList.refresh()"
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
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
    OpenSilexResponse
} from "../../lib/HttpResponse";
// @ts-ignore
import {DeviceCreationDTO} from "opensilex-core/index";
import VueRouter from "vue-router";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import DeviceCsvForm from "./csv/DeviceCsvForm.vue";
import DeviceDetails from "./DeviceDetails.vue";
import DeviceList from "./DeviceList.vue";
import DeviceModalForm from "./form/DeviceModalForm.vue";

@Component
export default class DeviceView extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    $router: VueRouter;

    @Ref("modalForm") readonly modalForm!: DeviceModalForm;

    renderCsvForm = false;
    @Ref("csvForm") readonly csvForm!: DeviceCsvForm;

    @Ref("deviceList") readonly deviceList!: DeviceList;
    @Ref("deviceDetails") readonly deviceDetails!: DeviceDetails;
    @Ref("deviceAttributesForm") readonly deviceAttributesForm!: any;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    created() {
    }

    showCsvForm() {
        this.renderCsvForm = true;
        this.$nextTick(() => {
            this.csvForm.show();
        });
    }

    displayAfterCreation(device : DeviceCreationDTO){
        this.$store.commit("storeReturnPage", this.$router);
        this.$router.push({path: "/device/details/" + encodeURIComponent(device.uri)});
    }


}
</script>

<style scoped lang="scss">

.createButton {
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
  margin-right: 5px;
}

.devicesHelp{
  font-size: 1.8em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
    margin-top: -10px;
}
</style>

<i18n>
en:
    Device:
        title: Devices
        description: Manage Devices
        add: Add device
        update: Update device
        delete: Delete device
        devices-help: Devices include objects, machines, electrical, electronic or mechanical devices, etc.
fr:
    Device:
        title: Appareils
        description: Gestion des appareils
        add: Ajouter un appareil
        update: Editer un appareil
        delete: Supprimer un appareil
        devices-help: Les appareils sont des objets, machines, dispositifs électriques, électroniques, mécaniques, etc.

</i18n>
