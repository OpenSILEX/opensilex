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
              {{ $t("FacilityHistogramSettings.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>

    <!-- periods -->
    <opensilex-DatePeriodPicker
        ref="periodPicker"
        :period.sync="period"
        @update="updateDatePeriod"
    >
    </opensilex-DatePeriodPicker>
    <br><br>

    <!-- All devices selection checkbox -->
    <b-form-group
      :label="$t('FacilityHistogramSettings.filter') + ':'"
    >
      <b-row class="selectAllDevicesBox">
        <b-col cols="0">
          <b-form-checkbox
              class="selection-box custom-control custom-checkbox"
              v-model="selectAll"
              @click="selectAll = !selectAll"
              switches
            >
          </b-form-checkbox>
        </b-col>
        <span class="ml-1 mt-1 selectLabel"> {{ $t('FacilityHistogramSettings.all_provenances') }}</span>
        <font-awesome-icon icon="question-circle" class="selectionHelp" v-b-tooltip.hover.right=" $t('HistogramSettings.help') "/>
      </b-row>
    </b-form-group>

    <!-- footer buttons -->
    <template v-slot:modal-footer> 
      <button
        type="button"
        class="btn btn-secondary"
        v-on:click="hide()"
      >
        {{ $t('FacilityHistogramSettings.cancel') }}
      </button>
      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="validate()"
      >
        {{ $t('FacilityHistogramSettings.apply') }}
      </button>
    </template>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop , PropSync} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import DatePeriodPicker, {Period} from "./DatePeriodPicker.vue";

@Component
export default class FacilityHistogramSettings extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Ref("modal") readonly modal!: any;
  @Ref("periodPicker") readonly periodPicker!: DatePeriodPicker;

  period: Period = "week";
  startDate: string;
  endDate: string;

  selectAll : Boolean = false;

  created() {
    this.endDate = new Date().toISOString();
    let begin = new Date();
    begin.setDate(begin.getDate() - 7);
    this.startDate = begin.toISOString();
    console.debug(this.startDate + "->" + this.endDate);
  }

  updateDatePeriod(begin: Date, end: Date) {
    this.startDate = begin.toISOString();
    this.endDate = end.toISOString();
  }

  sendUpdate() {
    this.$emit('update', this.period, this.startDate, this.endDate, this.selectAll);
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
  FacilityHistogramSettings:
    title : Graphic settings
    apply: Apply
    cancel: Cancel
    display_by: Display by
    filter: Filter selection
    all_provenances: All Provenances
    select_devices: Select Devices
    hour: Hour
    day: Day
    week: Week
    month: Month
    year: Year
    help: Uncheck to use bellow selector

fr:
  FacilityHistogramSettings:
    title : Paramètres du graphique
    apply: Appliquer
    cancel: Annuler
    display_by: Afficher par
    filter: Filtrer la selection
    all_provenances: Toutes les provenances
    select_devices: Choisir des dispositifs
    hour: Heure
    day: Jour
    week: Semaine
    month: Mois
    year: année
    help: Décochez pour utiliser le sélecteur ci-dessous

</i18n>
