<template>
    <opensilex-SelectForm
        label="VariableForm.time-interval"
        :selected.sync="selectedTimeIntervalId"
        :options="periodList"
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
import {VariableTimeIntervalDTO} from "opensilex-core/model/variableTimeIntervalDTO";
import {VariablesService} from "opensilex-core/api/variables.service";

@Component
export default class VariableTimeIntervalSelector extends Vue {
  //#region Plugins and services
    private readonly $opensilex: OpenSilexVuePlugin
    public readonly $store: OpenSilexStore
    private service: VariablesService
  //#endregion

  //#region Props
    @PropSync("timeinterval")
    private selectedTimeIntervalId
  //#endregion

  //#region Computed
  private get periodList(): Array<VariableTimeIntervalDTO> {
      return this.$store.state.time_interval_list
    }
  //#endregion

  //#region Events
  private emitHandlingEnterKey() {
    this.$emit("handlingEnterKey")
  }
  //#endregion

  //#region Hooks
  created() {
      this.service = this.$opensilex.getService("opensilex.VariablesService");
    }
  //#endregion

}
</script>