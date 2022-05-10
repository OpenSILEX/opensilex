<template>
    <opensilex-SelectForm
        :label="label"
        :selected.sync="timeIntervalURI"
        :options="periodList"
        placeholder="VariableForm.time-interval-placeholder"
    ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VariableTimeIntervalSelector extends Vue {
    $opensilex: any;

    @PropSync("timeinterval")
    timeIntervalURI;

    @Prop()
    label;

    periodList: Array<any> = [];

    created(){
        for(let period of ["millisecond","second","minute","hour","day","week","month","unique"]){
            this.periodList.push({
                id: this.$i18n.t("VariableForm.dimension-values." + period),
                label: this.$i18n.t("VariableForm.dimension-values." + period)
            })
        }
    }

}
</script>