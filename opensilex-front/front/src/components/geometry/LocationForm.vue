<!--
  - ******************************************************************************
  -                         LocationForm.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 02/10/2024 15:26
  - Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  -
  -
  - ******************************************************************************
  -
  -->

<template>
  <ValidationObserver ref="validatorRef">
    <!-- Dates -->
    <div class="row">
      <div class="col">
        <opensilex-DateTimeForm
                :value.sync="form.startDate"
                label="component.common.begin"
                :maxDate="form.endDate"
                :required="false"
        ></opensilex-DateTimeForm>
      </div>
      <div class="col">
        <opensilex-DateTimeForm
                :value.sync="form.endDate"
                label="component.common.end"
                :minDate="form.startDate"
                :required="true"
        ></opensilex-DateTimeForm>
      </div>
    </div>

    <!-- Geometry -->
    <div class="row">
      <div class="col-lg-9">
        <opensilex-GeometryForm
                :value.sync="form.geometry"
                label="component.common.geometry"
                helpMessage="component.common.geometry-help"
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
  //endregion

  //#region Refs
  @Ref("validatorRef")
  private readonly validatorRef!: any;
  //endregion

  //#region Data
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
  //endregion

  //#region Private methods
  static getEmptyForm() : LocationObservationDTO{
    return {
      geojson: undefined,
      startDate: undefined,
      endDate: undefined
    };
  }

  getEmptyForm() {
    return LocationForm.getEmptyForm();
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