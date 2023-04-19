<template>
    <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID) && renderModalForm"
        ref="modalForm"
        component="opensilex-DeviceForm"
        createTitle="Device.add"
        editTitle="Device.update"
        icon="ik#ik-thermometer"
        modalSize="lg"
        :successMessage="successMessage"
        @onCreate="$emit('onCreate', $event)"
        @onUpdate="$emit('onUpdate', $event)"
    ></opensilex-ModalForm>
</template>

<script lang="ts">

import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import ModalForm from "../../common/forms/ModalForm.vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import DeviceForm from "./DeviceForm.vue";
import {DevicesService} from "opensilex-core/api/devices.service";
import {DeviceCreationDTO, DeviceGetDetailsDTO} from 'opensilex-core/index';
import DTOConverter from "../../../models/DTOConverter";

@Component
export default class DeviceModalForm extends Vue {

    $opensilex: OpenSilexVuePlugin;
    service: DevicesService;
    $store: any;

    renderModalForm: boolean = false;
    @Ref("modalForm") readonly modalForm!: ModalForm<DeviceForm, DeviceCreationDTO, DeviceCreationDTO>;

    created() {
        this.service = this.$opensilex.getService("opensilex.DevicesService");
    }

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    showCreateForm() {
        this.renderModalForm = true;
        this.$nextTick(() => {
            this.modalForm.showCreateForm();
        })
    }

    showEditForm(uri: string) {
        // get device from API
        this.service.getDevice(uri)
            .then((http: HttpResponse<OpenSilexResponse<DeviceGetDetailsDTO>>) => {
                let device = http.response.result;

                // render form and pass device to form
                this.renderModalForm = true;
                this.$nextTick(() => {
                    let form: DeviceForm = this.modalForm.getFormRef();
                    form.readAttributes(device.metadata)
                    form.typeSwitch(device.rdf_type, true);
                    const editDto = DTOConverter.extractURIFromResourceProperties<DeviceGetDetailsDTO, DeviceCreationDTO>(device);
                    this.modalForm.showEditForm(editDto);
                });
            }).catch(this.$opensilex.errorHandler);
    }

    successMessage(device: DeviceCreationDTO): string {
        return this.$i18n.t("DeviceForm.name") + " " + device.name + " ";
    }

}
</script>

<style scoped>

</style>