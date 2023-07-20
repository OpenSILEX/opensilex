<template>
    <div>
      <div class="btn-group btn-group-toggle btnsGroup" data-toggle="buttons" :options="periods">
        <!-- day -->
        <label class="btn periodBtn btn-toggle greenThemeColor"
          :class="{
            active: selectedPeriod == 'day'
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
            active: selectedPeriod == 'week'
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
            active: selectedPeriod == 'month'
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

        <!-- 6 month -->
        <label class="btn periodBtn btn-toggle greenThemeColor"
               :class="{
            active: selectedPeriod == '6month'
          }"
        >
          <input
              type="radio"
              name="options"
              id="option4"
              value="6month"
              v-model="selectedPeriod"
          >
          {{$t('DatePeriodPicker.6month')}}
        </label>
      </div>
    </div>
</template>

<script lang="ts">
import {Component, Ref, Prop, PropSync, Watch} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

export type Period = "day" | "week" | "month" | "6month" | "year";

@Component
export default class DatePeriodPicker extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @PropSync("period")
  selectedPeriod: Period;

  startDate: Date;
  endDate: Date;


  get periods() {
    return [
      { text: this.$i18n.t("DatePeriodPicker.day"), value: 'day'},
      { text: this.$i18n.t("DatePeriodPicker.week"), value: 'week'},
      { text: this.$i18n.t("DatePeriodPicker.month"), value: 'month'},
      { text: this.$i18n.t("DatePeriodPicker.6month"), value: '6month'},
      { text: this.$i18n.t("DatePeriodPicker.year"), value: 'year'},
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
    else if (this.selectedPeriod === "6month") {
      this.startDate.setMonth(this.endDate.getMonth() - 6);
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
    6month: 6 Month
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
    6month: 6 Mois
    year: Année
    help: Décochez pour utiliser le sélecteur ci-dessous

</i18n>
