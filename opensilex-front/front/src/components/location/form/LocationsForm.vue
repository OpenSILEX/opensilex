<!--
  - ******************************************************************************
  -                         LocationsForm.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 16/10/2024 14:58
  - Contact: alexia.chiavarino@inrae.fr
  - ******************************************************************************
  -
  -->

<template>
    <ValidationObserver ref="validatorRef">
        <!-- POSITIONS -->
        <br/>
        <p>
            <b>{{ $t('LocationsForm.positions-geospatial') }} </b>
        </p>
        <hr/>

        <b-form>
            <!-- Dates -->
            <opensilex-DateTimeRangeForm
                :startDate.sync="position.startDate"
                :endDate.sync="position.endDate"
                :start_required="false"
                :end_required="!!position.geojson || !!position.startDate"
                :isInstant.sync="currentLocationIsInstant"
                :canBeInstant="true"
            >
            </opensilex-DateTimeRangeForm>

            <div class="row">
                <!-- Geometry -->
                <div class="col-8">
                    <opensilex-GeometryForm
                            :value.sync="position.geojson"
                            label="component.common.geometry"
                            :required="!!position.endDate"
                            placeholder="LocationsForm.geometry"
                            @onUpdate="checkGeometryNotSaved"
                    >
                    </opensilex-GeometryForm>
                </div>

                <!-- Add position -->
                <div class="col-4" style="padding-top: 25px">
                    <opensilex-AddChildButton
                            @click="addPosition"
                            label="LocationsForm.add-position"
                            :small="true"
                    ></opensilex-AddChildButton>
                    <span> {{ $t('LocationsForm.add-position') }}</span>
                </div>
            </div>
        </b-form>

        <!-- Position list -->
        <opensilex-TableView :fields="fields" :items="facility.locations">
            <template v-slot:cell(startDate)="{ data }">
                <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
            </template>

            <template v-slot:cell(endDate)="{ data }">
                <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
                <!-- Warning iff the endDate is equal to 1970 == default date for facility geometry from migration-->
                <b-alert
                        v-if="data.item.endDate === DEFAULT_DATE"
                        variant="warning"
                        show
                >
                    {{ $t("component.facility.warning.facility-default-date") }}
                </b-alert>
            </template>

            <template v-slot:cell(geometry)="{data}">
                <opensilex-GeometryCopy
                        label="" :value="data.item.geojson">
                </opensilex-GeometryCopy>
            </template>

            <template v-slot:cell(actions)="{ data }">
                <b-button-group size="sm">
                    <opensilex-EditButton
                            @click="updatePosition(data)"
                            label="component.common.list.buttons.update"
                            :small="true"
                    ></opensilex-EditButton>

                    <opensilex-DeleteButton
                            @click="deletePosition(data)"
                            label="component.common.list.buttons.delete"
                    ></opensilex-DeleteButton>
                </b-button-group>
            </template>
        </opensilex-TableView>
        <opensilex-WizardForm
                ref="locationForm"
                :steps="locationSteps"
                editTitle="LocationsForm.update"
                icon="ik#ik-globe"
                :static="false"
                :initForm="getEmptyLocationForm"
                :updateAction="updateLocationForm"
        ></opensilex-WizardForm>
    </ValidationObserver>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {LocationObservationDTO, FacilityUpdateDTO} from 'opensilex-core/index';
import {PropSync, Ref} from "vue-property-decorator";
import WizardForm from "../../../components/common/forms/WizardForm.vue";

@Component({})
export default class LocationsForm extends Vue {
    //#region Plugins and services
    //endregion

    //#region Props
    @PropSync("form")
    private facility: FacilityUpdateDTO;
    //endregion

    //#region Refs
    @Ref("validatorRef")
    readonly validatorRef!: any;
    @Ref("locationForm")
    private readonly locationForm!: WizardForm;
    //endregion

    //#region Data
    private readonly DEFAULT_DATE: string = "1970-01-01T00:00:00Z"
    private position: LocationObservationDTO = this.getPositionEmpty();
    private currentLocationIsInstant: boolean = true;
    private fields = [
        {
            key: "startDate",
            label: "component.common.begin",
            sortable: true,
        },
        {
            key: "endDate",
            label: "component.common.end",
            sortable: true,
        },
        {
            key: "geometry",
            label: "component.common.geometry",
        },
        {
            key: "actions",
            label: "component.common.actions",
        },
    ];
    private locationSteps = [
        {component: "opensilex-LocationForm"}
    ]
    private index: number;
    //endregion

    //#region Computed
    //endregion

    //#region Events
    private onUpdate() {
        this.$emit("onUpdate");
    }
    //endregion

    //#region Events handlers

    // Check is the two fields have a value
    private checkGeometryNotSaved() {
        const hasEndDate = !!this.position.endDate;
        const hasGeometry = !!this.position.geojson;

        // If there are completed, check if addPosition has been validate or not (button)
        if (hasEndDate && hasGeometry && !this.positionIsValid()) {
            console.log("géo not saved")
            this.$emit("geometryIsNotSaved");
        }
    }

    // Simulate validation of addPosition
    private positionIsValid(): boolean {
        return (
            this.facility.locations.some(
                (location) =>
                    location.endDate === this.position.endDate &&
                    location.geojson === this.position.geojson
            )
        );
    }

    private addPosition() {
        let isValid = this.validatorRef.validate().then(isValid => {
            return isValid
        });

        if (isValid) {
            if (this.position.geojson && this.position.endDate) {
                this.facility.locations.push(this.position)
                this.position = this.getPositionEmpty();
                this.$emit("positionIsValid")
            }
        }
    }

    private updatePosition(data) {
        this.index = data.index;
      //Copy item to prevent the update in the modal from directly modifying "data"
      let form = JSON.parse(JSON.stringify(data.item));
      this.locationForm.showEditForm(form);
    }

    private deletePosition(data) {
        this.facility.locations.splice(this.facility.locations.indexOf(data.item), 1)
    }

    getEmptyLocationForm(){
        return {
            geojson: this.position.geojson,
            startDate: this.position.startDate,
            endDate: this.position.endDate
        };
    }
    updateLocationForm(form){
        this.facility.locations[this.index].geojson = form.geojson;
        this.facility.locations[this.index].startDate = form.startDate;
        this.facility.locations[this.index].endDate = form.endDate;
    }
    //endregion

    //#region Public methods
    public reset() {
      this.position = this.getPositionEmpty();
      this.validatorRef.reset();
    }

    public validate() {
        return this.validatorRef.validate();
    }
    //endregion

    //#region Hooks
    //endregion

    //#region Private methods
    private getPositionEmpty(): LocationObservationDTO {
        return {
            geojson: undefined,
            startDate: undefined,
            endDate: undefined
        }
    }
    //endregion
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
    LocationsForm:
        positions-geospatial: Geospatial positions
        add-position: Add position
        geometry:  POINT (10 10)
        update: Update the geospatial position
fr:
    LocationsForm:
        positions-geospatial: Positions géospatiales
        add-position: Ajouter une position
        geometry: POINT (10 10)
        update: Mettre à jour la position géospatiale
</i18n>