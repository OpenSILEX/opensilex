<template>
  <b-modal
    ref="modal"
    size="md"
    centered
    :no-close-on-backdrop="true"
  >
    <!-- header -->
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <opensilex-Icon icon="fa#cog" />
              {{ $t("HistogramSettings.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>

    <!-- periods -->
    <b-form-group
      :label="$t('HistogramSettings.display_by') + ':'"
    > 
    </b-form-group> 

    <div class="btn-group btn-group-toggle btnsGroup" data-toggle="buttons" :options="periods">
      <!-- day -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
        :class="{
          active: selectedPeriod === 'day'
        }"
      >
        <input
          type="radio"
          name="options"
          id="option1"
          value="day"
          checked
          v-model="selectedPeriod"
        >
        {{$t('HistogramSettings.day')}}
      </label>

      <!-- week -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
        :class="{
          active: selectedPeriod === 'week'
        }"
      >
        <input
          type="radio"
          name="options"
          id="option2"
          value="week"
          v-model="selectedPeriod"
        >
        {{$t('HistogramSettings.week')}}
      </label>

      <!-- month -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
        :class="{
          active: selectedPeriod === 'month'
        }"
      >
        <input
          type="radio"
          name="options"
          id="option3"
          value="month"
          v-model="selectedPeriod"
        > 
        {{$t('HistogramSettings.month')}}
      </label>

      <!-- year -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
        :class="{
          active: selectedPeriod === 'year'
        }"
      >
        <input
          type="radio"
          name="options"
          id="option4"
          value="year"
          v-model="selectedPeriod"
        >
        {{$t('HistogramSettings.year')}}
      </label>
    </div>
    <br><br>
    
    <!-- All devices selection checkbox -->
    <b-form-group
      :label="$t('HistogramSettings.filter') + ':'"
    > 
      <b-row class="selectAllDevicesBox">
        <b-col cols="0">
          <b-form-checkbox
              class="selection-box custom-control custom-checkbox"
              v-model="selectAll"
              @click="selectAll = !selectAll"
              @change="changeSelectorAccess()"
              checked
              switches
            >
          </b-form-checkbox>
        </b-col>
        <span class="ml-1 mt-1 selectLabel"> {{ $t('HistogramSettings.all_devices') }}</span>
        <font-awesome-icon icon="question-circle" class="selectionHelp" v-b-tooltip.hover.right=" $t('HistogramSettings.help') "/>
      </b-row>
    </b-form-group>

    <!-- Devices selection filter -->
    <opensilex-SelectForm
      :multiple="true"
      :selected.sync="selectedDevices"
      :disabled="selectorAccess"
      :options="devicesLoaded"
      placeholder="HistogramSettings.select_devices"
    ></opensilex-SelectForm>

    <!-- footer buttons -->
    <template v-slot:modal-footer> 
      <button
        type="button"
        class="btn btn-secondary"
        v-on:click="hide()"
      >
        {{ $t('HistogramSettings.cancel') }}
      </button>
      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="validate()"
      >
        {{ $t('HistogramSettings.apply') }}
      </button>
    </template>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop , PropSync} from "vue-property-decorator";
import Vue from "vue";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import TreeViewAsync from "../../common/views/TreeViewAsync.vue";
import Histogram from "./Histogram.vue"

@Component
export default class HistogramSettings extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Ref("modal") readonly modal!: any;
  @Ref("devicesTree") readonly devicesTree!: TreeViewAsync;
  @Ref("histogram") readonly Histogram!: Histogram;

  vSelected = null;
  dSelected = null;
  selectedDevices : Array<{text: string; value: string}> = [];
  selectAll : Boolean = true;
  selectorAccess : Boolean = true; // indicate prop "disabled" of selectform = true
  
  variable: VariableDetailsDTO;

  @PropSync("period")
  selectedPeriod;

  @Prop()
  devicesLoaded;

  get periods() {
    return [
      { text: this.$i18n.t("HistogramSettings.day"), value: 'day'},
      { text: this.$i18n.t("HistogramSettings.week"), value: 'week'},
      { text: this.$i18n.t("HistogramSettings.month"), value: 'month'},
      { text: this.$i18n.t("HistogramSettings.year"), value: 'year'},
    ];
  }

  get filterOptions() {
    return [].concat(this.firstFilterValue, this.devicesLoaded);
  }

  get firstFilterValue() {
    return [{
      value: undefined,
      text: this.$i18n.t("HistogramSettings.all_devices")
    }];
  }

  /**
   * toggle boolean to enable / disable devices selector according to "all devices" checkbox is checked or not
   */
  changeSelectorAccess(){
    this.selectorAccess = !this.selectorAccess
  }

/**
 * we look at the uris from selectedDevices[] having a correspondence with those of devicesLoaded[], we recover the text of these
 * and we push in a third array of objects [uri / names] devicesToDisplay[]
 * which will be used during the update event called in validation
 */

  devicesToDisplay: Array<{label: string; id: string}> = [];

  sendUpdate(){
    if (this.selectAll === true){
      this.selectedDevices = [];
      this.$emit('update', this.devicesLoaded, this.selectedPeriod)
    }
    else {
      this.devicesToDisplay = [];
      for (let i = 0; i < this.selectedDevices.length; i++){
        for (let j = 0; j< this.devicesLoaded.length; j++) {

          /**
           * if selectedDevice(uri) = to the id contained in the object devicesLoaded : (id = uri)
           * push this element in a new array deviceToDisplay with parameters : name(label) and uri(id) of the object
           */ 
          if (this.selectedDevices[i] === this.devicesLoaded[j].id) {
            this.devicesToDisplay.push({
              label: this.devicesLoaded[j].label,
              id: this.devicesLoaded[j].id,
            });
          }
        }
      }
      if (this.devicesToDisplay.length === 0 || this.selectedDevices.length === 1 && this.selectedDevices[0] === null) {
        this.$emit('update', this.devicesLoaded, this.selectedPeriod);
      }
      else {
        this.$emit('update', this.devicesToDisplay, this.selectedPeriod);
      }
    }
  }

  show() {
    this.modal.show();
  }

  hide(){
    this.modal.hide()
  }

  validate() {
    this.sendUpdate()
    this.hide()
  }
}
</script>

<style scoped lang="scss">

.periodBtn{
  border-color:#018371;
  background: #fff;
  color: #018371
}

.active {
  background-color: #00A38D;
  border-color:#00A38D;
  color: #fff;
}

.selectAllDevicesBox {
  margin-left: 0;
}
.selectionHelp{
  margin-top: 7px;
  margin-left: 8px;

}
</style>

<i18n>
en:
  HistogramSettings:
    title : Graphic settings
    apply: Apply
    cancel: Cancel
    display_by: Display by
    filter: Filter selection
    all_devices: All Devices
    select_devices: Select Devices
    hour: Hour
    day: Day
    week: Week
    month: Month
    year: Year
    help: Uncheck to use bellow selector

fr:
  HistogramSettings:
    title : Paramètres du graphique
    apply: Appliquer
    cancel: Annuler
    display_by: Afficher par
    filter: Filtrer la selection
    all_devices: Tout les appareils
    select_devices: Choisir des appareils
    hour: Heure
    day: Jour
    week: Semaine
    month: Mois
    year: année
    help: Décochez pour utiliser le sélecteur ci-dessous

</i18n>
