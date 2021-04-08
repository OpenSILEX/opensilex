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
                <b-button @click="csvExport" variant="outline-primary">{{
                    $t("OntologyCsvImporter.downloadTemplate")
                    }}</b-button>
            </ValidationObserver>
        </div>
    </b-modal>
</template>

<script lang="ts">
    import { Component, Prop, Ref } from "vue-property-decorator";
    import Vue from "vue";
    import {he} from "vuejs-datepicker/src/locale/index";

    @Component
    export default class GenerateMoveTemplate extends Vue {
        $opensilex: any;
        $store: any;
        $t: any;
        $i18n: any;
        $papa: any;

        requiredField: boolean = false;

        separator = ",";

        @Ref("validatorRefTemplate") readonly validatorRefTemplate!: any;

        @Ref("soModalRef") readonly soModalRef!: any;

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

                    let headers = [
                        "URI", "Type", "IsInstant", "Start", "End", "Target", "Description",
                        "From", "To", "Coordinates", "X", "Y", "Z", "TextualPosition"
                    ];

                    let descriptions = [
                        {field: "Move.uri-help", required: false},
                        {field: "MoveHelpTableView.type-help", required: false},
                        {field: "MoveHelpTableView.is-instant-help", required: true},
                        {field: "MoveHelpTableView.start-help", required: false},
                        {field: "MoveHelpTableView.end-help", required: false},
                        {field: "MoveHelpTableView.target-help", required: true},
                        {field: "MoveHelpTableView.description-help", required: false},
                        {field: "MoveHelpTableView.from-help", required: false},
                        {field: "MoveHelpTableView.to-help", required: false},
                        {field: "MoveHelpTableView.coordinates-help", required: false},
                        {field: "MoveHelpTableView.x-help", required: false},
                        {field: "MoveHelpTableView.y-help", required: false},
                        {field: "MoveHelpTableView.z-help", required: false},
                        {field: "MoveHelpTableView.textual-position-help", required: false},
                    ];

                    let headersDescription = [];
                    descriptions.forEach(description => {
                        headersDescription.push(this.getDescriptionText(description.field, description.required));
                    })

                    let data = [headers, headersDescription];

                    let targetIndex = headers.indexOf("Target");
                    let isInstantIndex = headers.indexOf("IsInstant");

                    this.targets.forEach(target => {

                        if(target && target.length > 0){
                            let row = new Array(headers.length).fill('');
                            row[isInstantIndex] = "true";
                            row[targetIndex] = target;
                            data.push(row);
                        }
                    });
                    this.$papa.download(
                        this.$papa.unparse(data),
                        "moveTemplate"
                    );
                }
            });
        }
    }
</script>