<template>
    <opensilex-SelectForm
        :label="label"
        :selected.sync="timeIntervalURI"
        :options="periodList"
        placeholder="VariableForm.time-interval-placeholder"
        @keyup.enter.native="onEnter"
    ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VariableTimeIntervalSelector extends Vue {
    $opensilex: any;
    $store: any;

    @PropSync("timeinterval")
    timeIntervalURI;

    @Prop()
    label;

    periodList: Array<any> = [];

    mounted() {
        this.$store.watch(
            () => this.$store.getters.language,
            () => this.loadTimeInterval()
        );
    }

    created() {
        this.loadTimeInterval();
    }

    loadTimeInterval() {
        let period = ["millisecond","second","minute","hour","day","week","month","unique"];
        this.periodList = [];
        for(let value of period){
            this.periodList.push({
                id: value.charAt(0).toUpperCase() + value.slice(1),
                label: this.$i18n.t("VariableForm.dimension-values." + value)
            })
        }
    }

    onEnter() {
        this.$emit("handlingEnterKey")
    }
}
</script>