<template>
    <opensilex-SelectForm
        label="VariableForm.time-interval"
        :selected.sync="selectedTimeIntervalId"
        :options="timeintervalsAsOptions"
        placeholder="VariableForm.time-interval-placeholder"
        helpMessage="VariableForm.time-interval-help"
        @keyup.enter.native="emitHandlingEnterKey"
    ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, PropSync} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../../models/Store";
import {SelectableItem} from "../../common/forms/SelectForm.vue";
import {BasicURIAndLabelDTO} from "opensilex-core/model/basicURIAndLabelDTO";

@Component
export default class VariableTimeIntervalSelector extends Vue {
  //#region Plugins and services
    private readonly $opensilex: OpenSilexVuePlugin
    public readonly $store: OpenSilexStore
  //#endregion

  //#region Props
    @PropSync("timeinterval")
    private selectedTimeIntervalId
  //#endregion

  //#region Computed
  private get timeintervalsAsOptions(): Array<SelectableItem> {
      return this.$store.state.time_interval_list.map((timeinterval: BasicURIAndLabelDTO) => {
        return {
          id: timeinterval.uri,
          label: timeinterval.label
        }
      })
    }
  //#endregion

  //#region Events
  private emitHandlingEnterKey() {
    this.$emit("handlingEnterKey")
  }
  //#endregion

}
</script>