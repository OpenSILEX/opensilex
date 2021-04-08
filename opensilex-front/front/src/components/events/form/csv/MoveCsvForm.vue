<template>
    <div>
        <opensilex-OntologyCsvImporter
                ref="importForm"
                :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
                :validateCSV="validateCSV"
                :uploadCSV="uploadCSV"
                @csvImported="onCsvImported"
        >
            <template v-slot:icon>
                <opensilex-Icon icon="ik#ik-target" class="icon-title" />
            </template>

            <template v-slot:help>
                <opensilex-MoveHelpTableView></opensilex-MoveHelpTableView>
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
                    <opensilex-GenerateMoveTemplate
                            ref="templateGenerator"
                            :targets="targets"
                    ></opensilex-GenerateMoveTemplate>
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
    export default class MoveCsvForm extends Vue {

        $opensilex: any;
        $store: any;
        $router: VueRouter;

        @Ref("templateGenerator") readonly templateGenerator!: any;
        @Ref("importForm") readonly importForm!: any;

        nbLinesImported = 0;

        @Prop({default: () => []})
        targets: Array<string>

        users: any[] = [];

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
            return this.$opensilex.uploadFileToService(
                "/core/events/moves/import_validation",
                {description: {}, file: csvFile}
            );
        }

        uploadCSV(validationToken,csvFile){
            return this.$opensilex.uploadFileToService(
                "/core/events/moves/import",
                {description: {}, file: csvFile}
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
