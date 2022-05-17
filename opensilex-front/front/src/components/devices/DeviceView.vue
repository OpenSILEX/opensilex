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
                    label="Device.add"
                    @click="modalForm.showCreateForm()"
                ></opensilex-CreateButton>

                <opensilex-CreateButton
                    label="OntologyCsvImporter.import"
                    @click="showCsvForm"
                ></opensilex-CreateButton>

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
