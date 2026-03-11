<template>
    <opensilex-FormSelector
        ref="formSelector"
        :label="label"
        :selected.sync="timeIntervalURI"
        :options="periodList"
        placeholder="VariableForm.time-interval-placeholder"
        @keyup.enter.native="onEnter"
    ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import FormSelector from "../../common/forms/FormSelector.vue";

@Component
export default class VariableTimeIntervalSelector extends Vue {
    $opensilex: any;
    $store: any;
    @Ref("formSelector") readonly formSelector!: FormSelector;

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


    setSelectedNode(node: { id: string; label: string }) {
        this.tutorialLabels[node.id] = node.label;
        this.formSelector.select(node);
    }

    private tutorialLabels: Record<string, string> = {};


    refresh() {
        this.formSelector.refresh();
    }

    loadTimeInterval() {
        const periods = ["millisecond","second","minute","hour","day","week","month","unique"];
        this.periodList = periods.map(value => ({
            id: value,
            label: this.$i18n.t("VariableForm.dimension-values." + value)
        }));
    }

    onEnter() {
        this.$emit("handlingEnterKey")
    }
}
</script>