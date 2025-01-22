<!--
  - ******************************************************************************
  -                         LocationModalForm.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 02/10/2024 14:11
  - Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  -
  -
  - ******************************************************************************
  -
  -->
<template>
    <opensilex-ModalForm
            ref="locationForm"
            component="opensilex-LocationForm"
            :updateAction="updateLocation"
            editTitle="LocationForm.update"
            icon="ik#ik-globe"
    ></opensilex-ModalForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Ref} from "vue-property-decorator";
import LocationForm from "../../../components/location/form/LocationForm.vue";

@Component({})
export default class LocationModalForm extends Vue {
    //#region Plugins and services
    //endregion

    //#region Props
    //endregion

    //#region Refs
    @Ref("locationForm")
    private readonly locationForm!: LocationForm;
    //endregion

    //#region Data
    //endregion

    //#region Computed
    //endregion

    //#region Events
    private updateLocation(form) {
        this.$emit('onUpdate', form)
    }
    //endregion

    //#region Events handlers
    //endregion

    //#region Public methods
    //endregion

    //#region Hooks
    //endregion

    //#region Private methods
    //endregion
}
</script>

<style scoped lang="scss">

</style>