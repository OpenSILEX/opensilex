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
            v-if="displayDateFields"
            :startDate.sync="form.startDate"
            :endDate.sync="form.endDate"
            :start_required="false"
            :end_required="!!form.geojson || !!form.startDate"
            :isInstant.sync="locationIsInstant"
            :canBeInstant="true"
        >
        </opensilex-DateTimeRangeForm>

      <div class="row" v-if="!isForFacilityLocation">
        <div class="col-lg-5">
          <opensilex-FacilitySelector
            label="LocationForm.from"
            :facilities.sync="form.from"
            :multiple="false"
            :required="fromRequired"
            @select="updateRequiredProps()"
            @clear="updateRequiredProps()"
            helpMessage="component.common.geometry.from-help"
          ></opensilex-FacilitySelector>
        </div>
        <div class="col-lg-5">
          <opensilex-FacilitySelector
            label="LocationForm.to"
            :facilities.sync="form.to"
            :multiple="false"
            :required="toRequired"
            @select="updateRequiredProps()"
            @clear="updateRequiredProps()"
            helpMessage="component.common.geometry.to-help"
          ></opensilex-FacilitySelector>
        </div>
      </div>

      <div>
        <p><b> {{ $t("LocationForm.positionTitle") }}</b></p>
        <hr/>

        <!-- Geometry -->
        <div class="row">
            <div class="col-lg-9">
                <opensilex-GeometryForm
                        :value.sync="form.geojson"
                        label="component.common.geometry.geometry-title"
                        helpMessage="component.common.geometry.geometry-help"
                        :required="false"
                ></opensilex-GeometryForm>
            </div>
        </div>
      <!--XYZ form-->
      <div class="row" v-if="!isForFacilityLocation">
        <div class="col-lg-3">
          <opensilex-InputForm
            :value.sync="form.x"
            label="component.common.geometry.x"
            helpMessage="component.common.geometry.x-help"
            placeholder="component.common.geometry.x-placeholder"
            type="text"
          ></opensilex-InputForm>
        </div>

        <div class="col-lg-3">
          <opensilex-InputForm
            :value.sync="form.y"
            label="component.common.geometry.y"
            helpMessage="component.common.geometry.y-help"
            placeholder="component.common.geometry.y-placeholder"
            type="text"
          ></opensilex-InputForm>
        </div>

        <div class="col-lg-3">
          <opensilex-InputForm
            :value.sync="form.z"
            label="component.common.geometry.z"
            helpMessage="component.common.geometry.z-help"
            placeholder="component.common.geometry.z-placeholder"
            type="text"
          ></opensilex-InputForm>
        </div>
      </div>
      <!--Textual position form-->
      <div class="row" v-if="!isForFacilityLocation">
        <div class="col">
          <!-- Comment -->
          <opensilex-TextAreaForm
            :value.sync="form.text"
            label="component.common.geometry.textual-position"
            helpMessage="component.common.geometry.textual-position-help"
            placeholder="component.common.geometry.textual-position-placeholder"
            @keydown.native.enter.stop
          ></opensilex-TextAreaForm>
        </div>
      </div>
      </div>
    </ValidationObserver>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop, Ref} from "vue-property-decorator";
import {LocationObservationDTO} from "opensilex-core/index";
import {GeoJsonObject} from "opensilex-core/model/geoJsonObject";

@Component({})
export default class LocationForm extends Vue {
    //#region Plugins and services
    //endregion

    //#region Props
    @Prop({default: () => LocationForm.getEmptyForm()})
    form: LocationObservationDTO;

    @Prop({default: false})
    disableValidation: boolean;

    @Prop({ default: false })
    isForFacilityLocation: boolean

    @Prop({ default: true })
    displayDateFields: boolean;
    //endregion

    //#region Refs
    @Ref("validatorRef") readonly validatorRef!: any;
    //endregion

    //#region Data
    private locationIsInstant: boolean = true;
    fromRequired: boolean = false;
    toRequired: boolean = false;
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
          featureOfInterest: undefined,
          label: undefined,
          startDate: undefined,
          from: undefined,
          to: undefined,
          endDate: undefined,
          x: undefined,
          y: undefined,
          z: undefined,
          text: undefined,

        };
    }

  /**
   * The "From" field is optional, and becomes required from the moment the "To" field is completed.
   */
  private updateRequiredProps() {
    if(this.form.from == undefined
      && this.form.to == undefined
      && this.form.geojson !== undefined
      && this.form.geojson !== "") {
      this.fromRequired = false;
      this.toRequired = false;
    } else {
      this.toRequired = true;
    }
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
        update: Update location,
        positionTitle: Position,
        from: From
        to: To

fr:
    LocationForm:
        update: Modifier la localisation
        positionTitle: Position
        from: De
        to: Vers

</i18n>