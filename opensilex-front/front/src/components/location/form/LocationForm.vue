<!--
  - ******************************************************************************
  -                         LocationForm.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 09/10/2024 13:52
  - Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  -
  -
  - ******************************************************************************
  -
  -->

<template>
    <ValidationObserver ref="validatorRef">
        <!-- Dates -->
        <opensilex-DateTimeRangeForm
            :startDate.sync="form.startDate"
            :endDate.sync="form.endDate"
            :start_required="false"
            :end_required="!!form.geojson || !!form.startDate"
            :isInstant="locationIsInstant"
            :canBeInstant="true"
        >
        </opensilex-DateTimeRangeForm>

        <!-- Geometry -->
        <div class="row">
            <div class="col-lg-9">
                <opensilex-GeometryForm
                        :value.sync="form.geojson"
                        label="component.common.geometry"
                        helpMessage="component.common.geometry-help"
                        :required="true"
                ></opensilex-GeometryForm>
            </div>
        </div>
    </ValidationObserver>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop, Ref} from "vue-property-decorator";
import {LocationObservationDTO} from "opensilex-core/index";

@Component({})
export default class LocationForm extends Vue {
    //#region Plugins and services
    //endregion

    //#region Props
    @Prop({default: () => LocationForm.getEmptyForm()})
    form: LocationObservationDTO;
    @Prop({default: false})
    disableValidation: boolean;
    //endregion

    //#region Refs
    @Ref("validatorRef") readonly validatorRef!: any;
    //endregion

    //#region Data
    private locationIsInstant: boolean = true;
    //endregion

    //#region Computed
    //endregion

    //#region Events
    //endregion

    //#region Events handlers
    //endregion

    //#region Public methods
    //endregion

    //#region Hooks
    private mounted(){
        this.locationIsInstant = !this.form.startDate;
    }
    //endregion

    //#region Private methods
    static getEmptyForm(): LocationObservationDTO {
        return {
            geojson: undefined,
            startDate: undefined,
            endDate: undefined
        };
    }

    getEmptyForm() {
        return LocationForm.getEmptyForm();
    }

    reset() {
        return this.validatorRef.reset();
    }

    validate() {
        return this.validatorRef.validate();
    }
    //endregion
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
    LocationForm:
        update: Update location

fr:
    LocationForm:
        update: Modifier la localisation

</i18n>