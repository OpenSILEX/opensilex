<template>
    <div>
        <opensilex-OntologyCsvImporter
                ref="importForm"
                :baseType="isMove ? this.$opensilex.Oeev.MOVE_TYPE_URI : this.$opensilex.Oeev.EVENT_TYPE_URI"
                :validateCSV="validateCSV"
                :uploadCSV="uploadCSV"
                @csvImported="onCsvImported"
                successImportMsg="Event.multiple-insert"
                :title="isMove ? 'EventHelpTableView.move-csv-import-title' : 'EventHelpTableView.csv-import-title'"
        >
            <template v-slot:icon>
                <opensilex-Icon icon="ik#ik-target" class="icon-title" />
            </template>

            <template v-slot:help>
                <opensilex-EventHelpTableView
                    :isMove="isMove"
                ></opensilex-EventHelpTableView>
            </template>

            <template v-slot:generator>
                <b-col cols="2">
                    <opensilex-Button
                            class="mr-2 greenThemeColor"
                            :small="false"
                            @click="templateGenerator.show()"
                            icon
                            label="DataView.buttons.generate-template"
                    ></opensilex-Button>
                    <opensilex-GenerateEventTemplate
                            ref="templateGenerator"
                            :targets="targets"
                            :isMove="isMove"
                    ></opensilex-GenerateEventTemplate>
                </b-col>
            </template>


        </opensilex-OntologyCsvImporter>
    </div>
</template>

<script lang="ts">
    import { Component, Prop, Ref } from "vue-property-decorator";
    import Vue from "vue";
    import VueRouter from "vue-router";
    import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";

    @Component
    export default class EventCsvForm extends Vue {

        $opensilex: OpenSilexVuePlugin;
        $store: any;
        $router: VueRouter;
        users: any[] = [];

        @Ref("templateGenerator") readonly templateGenerator!: any;
        @Ref("importForm") readonly importForm!: any;
        nbLinesImported = 0;

        @Prop({default: () => []})
        targets: Array<string>

        @Prop({default : false})
        isMove: boolean;

        get user() {
            return this.$store.state.user;
        }

        get lang() {
            return this.$store.state.lang;
        }

        show() {
            this.importForm.show();
        }

        validateCSV(csvFile){
            let path = this.isMove ? "/core/events/moves/import_validation" : "/core/events/import_validation";

            return this.$opensilex.uploadFileToService(
                path, {description: {}, file: csvFile}, null, false
            );
        }

        uploadCSV(validationToken,csvFile){
            let path = this.isMove ? "/core/events/moves/import" : "/core/events/import";

            return this.$opensilex.uploadFileToService(
                path, {description: {}, file: csvFile}, null, false
            );
        }

        onCsvImported(response){
            this.nbLinesImported = response.result.nb_lines_imported;
            this.$emit("csvImported", response);
        }

    }
</script>
<style scoped lang="scss">
</style>

<i18n>
en:
    EventCsvForm:
        invalidDate: Start date cannot be later than End date
fr:
    EventCsvForm:
        invalidDate: La date de début ne peut être ultérieure à celle de fin
</i18n>