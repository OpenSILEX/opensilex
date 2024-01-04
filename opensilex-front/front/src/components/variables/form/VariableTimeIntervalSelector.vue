<template>
    <opensilex-SelectForm
        label="VariableForm.time-interval"
        :selected.sync="selectedTimeIntervalId"
        :options="periodList"
        placeholder="VariableForm.time-interval-placeholder"
        helpMessage="VariableForm.time-interval-help"
        @keyup.enter.native="onEnter"
    ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../../models/Store";
import {VariableTimeIntervalDTO} from "opensilex-core/model/variableTimeIntervalDTO";
import {VariablesService} from "opensilex-core/api/variables.service";

@Component
export default class VariableTimeIntervalSelector extends Vue {
    private readonly $opensilex: OpenSilexVuePlugin
    private readonly $store: OpenSilexStore
    private service: VariablesService

    @PropSync("timeinterval")
    private selectedTimeIntervalId

    private get periodList(): Array<VariableTimeIntervalDTO> {
      return this.$store.state.timeIntervalList
    }

    created() {
      this.service = this.$opensilex.getService("opensilex.VariablesService");
    }

    private onEnter() {
        this.$emit("handlingEnterKey")
    }
}
</script>