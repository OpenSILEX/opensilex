<template>
    <opensilex-SelectForm
        :label="label"
        :selected.sync="selectedTimeIntervalId"
        :options="periodList"
        placeholder="VariableForm.time-interval-placeholder"
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

    @Prop()
    private readonly label

    private periodList: Array<VariableTimeIntervalDTO> = []


    mounted() {
        this.$store.watch(
            () => this.$store.getters.language,
            () => this.loadTimeInterval()
        );
    }

    created() {
      this.service = this.$opensilex.getService("opensilex.VariablesService");
      this.loadTimeInterval();
    }

    private async loadTimeInterval() {
        const response = await this.service.getTimeIntervals(this.$opensilex.getLang())
        this.periodList = response.response.result
    }

    private onEnter() {
        this.$emit("handlingEnterKey")
    }
}
</script>