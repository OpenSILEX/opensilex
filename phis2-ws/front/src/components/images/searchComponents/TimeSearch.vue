<template>
  <div>
    <b-form inline>
      <label class="mr-sm-2" for="inline-form-custom-select-pref">Start Date</label>
      <b-input-group class="mt-3 mb-3" size="sm" id="inline-form-custom-select-pref">
        <datePicker
          v-model="startDate"
          input-class="form-control"
          placeholder="Select a date"
          :clear-button="true"
          @input="onStartDateSelected"
          @cleared="onStartDateCleared"
        ></datePicker>
      </b-input-group>

      <label class="mr-sm-2 ml-4" for="inline-2">End Date</label>
      <b-input-group class="mt-3 mb-3" size="sm" id="inline-2">
        <datePicker
          v-model="endDate"
          input-class="form-control"
          placeholder="Select a date"
          :clear-button="true"
          @input="onEndDateSelected"
          @cleared="onEndDateCleared"
        ></datePicker>
      </b-input-group>
    </b-form>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { EventBus } from "./../event-bus";

@Component
export default class TimeSearch extends Vue {
  $router: VueRouter;
  startDate: string = "";
  endDate: string = "";

  onStartDateSelected() {
    console.log("start date" + this.startDate);
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          startDate: this.format(this.startDate)
        }
      })
      .catch(function() {});
    EventBus.$emit("startDateHasChanged", this.format(this.startDate));
  }

  onEndDateSelected() {
    console.log("end date" + this.endDate);
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          endDate: this.format(this.endDate)
        }
      })
      .catch(function() {});
    EventBus.$emit("endDateHasChanged", this.format(this.endDate));
  }

  onStartDateCleared() {
    this.startDate = "";
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          startDate: undefined
        }
      })
      .catch(function() {});
    EventBus.$emit("startDateHasChanged", undefined);
  }
  onEndDateCleared() {
    this.endDate = "";
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          endDate: undefined
        }
      })
      .catch(function() {});
    EventBus.$emit("endDateHasChanged", undefined);
  }

  format(date) {
    var d = new Date(date),
      month = "" + (d.getMonth() + 1),
      day = "" + d.getDate(),
      year = d.getFullYear();

    if (month.length < 2) month = "0" + month;
    if (day.length < 2) day = "0" + day;

    return [year, month, day].join("-");
  }

  created() {
    let query: any = this.$route.query;
    if (query.startDate) {
      this.startDate = query.startDate;
      EventBus.$emit("startDateHasChanged", this.startDate);
    }
    if (query.endDate) {
      this.endDate = query.endDate;
      EventBus.$emit("endDateHasChanged", this.endDate);
    }
  }
}
</script>

<style scoped >
</style>