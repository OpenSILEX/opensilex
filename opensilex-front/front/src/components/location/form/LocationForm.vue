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
            v-if="doShowDateFields"
            :startDate.sync="form.startDate"
            :endDate.sync="form.endDate"
            :start_required="false"
            :end_required="!!form.geojson || !!form.startDate"
            :isInstant="locationIsInstant"
            :canBeInstant="true"
        >
        </opensilex-DateTimeRangeForm>

      <div class="row">
        <div class="col-lg-5">
          <opensilex-FacilitySelector
            label="LocationForm.from"
            :facilities.sync="form.from"
            :multiple="false"
            :required="fromRequired"
            @select="updateRequiredProps()"
            @clear="updateRequiredProps()"
            helpMessage="LocationForm.from-help"
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
            helpMessage="LocationForm.to-help"
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
      <div class="row">
        <div class="col-lg-3">
          <opensilex-InputForm
            :value.sync="form.x"
            label="component.common.geometry.x"
            helpMessage="LocationForm.x-help"
            placeholder="LocationForm.x-placeholder"
            type="text"
          ></opensilex-InputForm>
        </div>

        <div class="col-lg-3">
          <opensilex-InputForm
            :value.sync="form.y"
            label="component.common.geometry.y"
            helpMessage="LocationForm.y-help"
            placeholder="LocationForm.y-placeholder"
            type="text"
          ></opensilex-InputForm>
        </div>

        <div class="col-lg-3">
          <opensilex-InputForm
            :value.sync="form.z"
            label="component.common.geometry.z"
            helpMessage="LocationForm.z-help"
            placeholder="LocationForm.z-placeholder"
            type="text"
          ></opensilex-InputForm>
        </div>
      </div>
      <!--Textual position form-->
      <div class="row">
        <div class="col">
          <!-- Comment -->
          <opensilex-TextAreaForm
            :value.sync="form.text"
            label="component.common.geometry.textual-position"
            helpMessage="LocationForm.textual-position-help"
            placeholder="LocationForm.textual-position-placeholder"
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
    isMove: boolean

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
        x-help: "1th dimension of a custom coordinate system"
        x-placeholder: "A or 10 or 10.5"
        y-help: "2nd dimension of a custom coordinate system"
        y-placeholder: "A or 10 or 10.5"
        z-placeholder: "A or 10 or 10.5"
        z-help: "3rd dimension of a custom coordinate system"
        textual-position-placeholder: Beside the left door
        textual-position-help: Description of the position with text
        from: From
        from-placeholder: dev-infra:greenHouseA
        from-help: "Starting facility URI (The facility must exists)."
        to: To
        to-placeholder: dev-infra:greenHouseB
        to-help: "Arrival facility URI (The facility must exists)."

fr:
    LocationForm:
        update: Modifier la localisation
        positionTitle: Position
        x-help: "1ère dimension d'un système de coordonnées"
        x-placeholder: "A ou 10 ou 10,5"
        y-help: "2ème dimension d'un système de coordonnées"
        y-placeholder: "A ou 10 ou 10,5"
        z-placeholder: "A ou 10 ou 10,5"
        z-help: "3ème dimension d'un système de coordonnées"
        textual-position-placeholder: "à côté de la porte gauche"
        textual-position-help: Description textuelle de la position
        from: De
        from-placeholder: dev-infra:serreA
        from-help: "URI de l'installation environnementale de départ (L'installation doit exister)."
        to: Vers
        to-placeholder: dev-infra:serreB
        to-help: "URI de l'installation environnementale d'arrivée (L'installation doit exister)."

</i18n>