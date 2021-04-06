<template>
    <b-modal
            ref="soModalRef"
            size="lg"
            ok-only
            :static="true"
            @hide="requiredField = false"
            @show="requiredField = true"
    >
        <template v-slot:modal-ok>{{ $t("component.common.ok") }}</template>
        <template v-slot:modal-title>{{
            $t("ScientificObjectCSVTemplateGenerator.title")
            }}</template>

        <div>
            <ValidationObserver ref="validatorRefTemplate">
                <b-col>
                    <opensilex-CSVSelectorInputForm :separator.sync="separator"> </opensilex-CSVSelectorInputForm>
                </b-col>

                <b-col>
                    <b-button @click="csvExport" variant="outline-primary">
                        {{$t("OntologyCsvImporter.downloadTemplate")}}
                    </b-button>
                </b-col>
            </ValidationObserver>
        </div>
    </b-modal>
</template>

<script lang="ts">
    import { Component, Prop, Ref } from "vue-property-decorator";
    import Vue from "vue";
    import {he} from "vuejs-datepicker/src/locale/index";

    @Component
    export default class GenerateEventTemplate extends Vue {
        $opensilex: any;
        $store: any;
        $t: any;
        $i18n: any;
        $papa: any;

        requiredField: boolean = false;
        separator = ",";

        @Ref("validatorRefTemplate") readonly validatorRefTemplate!: any;
        @Ref("soModalRef") readonly soModalRef!: any;

        @Prop({default: false})
        isMove: boolean;

        get user() {
            return this.$store.state.user;
        }

        @Prop({default: () => []})
        targets: Array<string>

        reset() {
            this.validatorRefTemplate.reset();
        }

        show() {
            this.soModalRef.show();
        }

        validateTemplate() {
            return this.validatorRefTemplate.validate();
        }

        private getDescriptionText(field : string, required : boolean) : string{
            let requiredStr = this.$t("ScientificObjectCSVTemplateGenerator.required") + ": ";
            requiredStr += required ? this.$t("component.common.yes") : this.$t("component.common.no");
            return  this.$t(field)+"\n" +requiredStr;
        }

        csvExport() {
            this.validateTemplate().then((isValid) => {
                // fill in large
                if (isValid) {

                    let headers = ["URI", "Type", "IsInstant", "Start", "End", "Target", "Description"];
                    let descriptions = [
                        {field: "Move.uri-help", required: false},
                        {field: "EventHelpTableView.type-help", required: false},
                        {field: "EventHelpTableView.is-instant-help", required: true},
                        {field: "EventHelpTableView.start-help", required: false},
                        {field: "EventHelpTableView.end-help", required: false},
                        {field: "EventHelpTableView.target-help", required: true},
                        {field: "EventHelpTableView.description-help", required: false},
                    ];

                    if(this.isMove){

                        headers.push("From", "To", "Coordinates", "X", "Y", "Z", "TextualPosition");
                        descriptions.push(
                            {field: "EventHelpTableView.from-help", required: false},
                            {field: "EventHelpTableView.to-help", required: false},
                            {field: "EventHelpTableView.coordinates-help", required: false},
                            {field: "EventHelpTableView.x-help", required: false},
                            {field: "EventHelpTableView.y-help", required: false},
                            {field: "EventHelpTableView.z-help", required: false},
                            {field: "EventHelpTableView.textual-position-help", required: false}
                        );
                    }

                    let headersDescription = [];
                    descriptions.forEach(description => {
                        headersDescription.push(this.getDescriptionText(description.field, description.required));
                    })

                    let data = [headers, headersDescription];

                    let targetIndex = headers.indexOf("Target");
                    let isInstantIndex = headers.indexOf("IsInstant");
                    let endIndex = headers.indexOf("End");

                    // append row with pre-filled target column
                    this.targets.forEach(target => {

                        if(target && target.length > 0){
                            let row = new Array(headers.length).fill('');
                            row[isInstantIndex] = "true";
                            row[endIndex] = new Date().toISOString();
                            row[targetIndex] = target;
                            data.push(row);
                        }
                    });

                    let templateName = this.isMove ? "move" : "event";
                    templateName += "_template_"+new Date().getTime();

                    this.$papa.download(this.$papa.unparse(data , {delimiter: this.separator}), templateName);
                }
            });
        }
    }
</script>