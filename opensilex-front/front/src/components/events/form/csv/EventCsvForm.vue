<template>
    <div>
        <opensilex-OntologyCsvImporter
                ref="importForm"
                :baseType="$opensilex.Oeev.EVENT_TYPE_URI"
                :validateCSV="validateCSV"
                :uploadCSV="uploadCSV"
                @csvImported="onCsvImported"
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
                            variant="secondary"
                            class="mr-2"
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

    @Component
    export default class EventCsvForm extends Vue {

        $opensilex: any;
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
                path, {description: {}, file: csvFile}
            );
        }

        uploadCSV(validationToken,csvFile){
            let path = this.isMove ? "/core/events/moves/import" : "/core/events/import";

            return this.$opensilex.uploadFileToService(
                path, {description: {}, file: csvFile}
            );
        }

        onCsvImported(response){
            this.nbLinesImported = response.result.nb_lines_imported;
            this.$opensilex.showSuccessToast(this.nbLinesImported+ " "+this.$i18n.t("Event.move-multiple-insert"));
            this.$emit("csvImported", response);
        }

    }
</script>
<style scoped lang="scss">
</style>
