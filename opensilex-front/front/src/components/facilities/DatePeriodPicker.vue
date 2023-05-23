<template>
    <div>
      <!-- periods -->
      <b-form-group
          :label="$t('DatePeriodPicker.display_by') + ':'"
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
          {{$t('DatePeriodPicker.day')}}
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
          {{$t('DatePeriodPicker.week')}}
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
          {{$t('DatePeriodPicker.month')}}
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
              id="option3"
              value="year"
              v-model="selectedPeriod"
          >
          {{$t('DatePeriodPicker.year')}}
        </label>
      </div>
    </div>
</template>

<script lang="ts">
import {Component, Ref, Prop, PropSync, Watch} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class DatePeriodPicker extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @PropSync("period")
  selectedPeriod;

  startDate: Date;
  endDate: Date;


  get periods() {
    return [
      { text: this.$i18n.t("HistogramSettings.day"), value: 'day'},
      { text: this.$i18n.t("HistogramSettings.week"), value: 'week'},
      { text: this.$i18n.t("HistogramSettings.month"), value: 'month'},
      { text: this.$i18n.t("HistogramSettings.year"), value: 'year'},
    ];
  }

  created() {
    this.endDate = new Date();
    this.updateDatePeriod();
  }

  @Watch("selectedPeriod")
  onSelectedPeriodChange() {
    this.updateDatePeriod();
    this.$emit("update", this.startDate, this.endDate);
  }

  getPeriodDates() {
    return {begin: this.startDate, end: this.endDate};
  }

  updateDatePeriod() {
    this.startDate = new Date();

    if (this.selectedPeriod === "day") {
      this.startDate.setDate(this.endDate.getDate() - 1);
    }
    else if (this.selectedPeriod === "week") {
      this.startDate.setDate(this.endDate.getDate() - 7);
    }
    else if (this.selectedPeriod === "month") {
      this.startDate.setMonth(this.endDate.getMonth() - 1);
    }
    else if (this.selectedPeriod === "year") {
      this.startDate.setMonth(this.endDate.getMonth() - 12);
    }
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
  DatePeriodPicker:
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
  DatePeriodPicker:
    title : Paramètres du graphique
    apply: Appliquer
    cancel: Annuler
    display_by: Afficher par
    filter: Filtrer la selection
    all_devices: Tout les dispositifs
    select_devices: Choisir des dispositifs
    hour: Heure
    day: Jour
    week: Semaine
    month: Mois
    year: année
    help: Décochez pour utiliser le sélecteur ci-dessous

</i18n>
