<!--
  -                         DeviceCsvForm.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2022.
  - Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  -
  -->

<template>
    <div>
        <opensilex-OntologyCsvImporter
            ref="importForm"
            :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
            :validateCSV="validateCSV"
            :uploadCSV="uploadCSV"
            @csvImported="onCsvImported"
            title="DeviceCsvForm.import-title"
            successImportMsg="DeviceCsvForm.multiple-insert"
        >
            <template v-slot:icon>
                <opensilex-Icon icon="ik#ik-target" class="icon-title"/>
            </template>

          <template v-slot:help>
            <opensilex-DeviceImportHelp></opensilex-DeviceImportHelp>
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

                    <opensilex-OntologyCsvTemplateGenerator
                        ref="templateGenerator"
                        :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
                        templatePrefix="device"
                        typePlaceholder="DeviceList.filter.rdfTypes-placeholder"
                        uriHelp="DeviceImportHelp.uri-help"
                        uriExample="DeviceCsvForm.uri-example"
                        typeHelp="DeviceImportHelp.type-help"
                        typeExample="DeviceCsvForm.type-example"
                    ></opensilex-OntologyCsvTemplateGenerator>
                </b-col>
            </template>
        </opensilex-OntologyCsvImporter>
    </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import OntologyCsvTemplateGenerator from 'src/components/ontology/csv/OntologyCsvImporter.vue';
import OntologyCsvImporter from 'src/components/ontology/csv/OntologyCsvImporter.vue';

@Component
export default class DeviceCsvForm extends Vue {

    $opensilex: OpenSilexVuePlugin;
    $store: any;
    users: any[] = [];

    @Ref("templateGenerator") readonly templateGenerator!: OntologyCsvTemplateGenerator;
    @Ref("importForm") readonly importForm!: OntologyCsvImporter;
    nbLinesImported = 0;

    get user() {
        return this.$store.state.user;
    }

    get lang() {
        return this.$store.state.lang;
    }

    show() {
        this.importForm.show();
    }

    validateCSV(csvFile) {
        let path = "/core/devices/import_validation";
        return this.$opensilex.uploadFileToService(
            path, {file: csvFile}, null, false
        );
    }

    uploadCSV(validationToken, csvFile) {
        let path = "/core/devices/import";
        return this.$opensilex.uploadFileToService(
            path, {file: csvFile}, null, false
        );
    }

    onCsvImported(response) {
        this.$emit("csvImported", response);
    }

}
</script>
<style scoped lang="scss">
</style>

<i18n>
en:
    DeviceCsvForm:
        import-title: Import device(s)
        multiple-insert: device(s) imported
        uri-example: http://opensilex.org/id/device/rasperry_pi_4B
        type-example: vocabulary:SensingDevice
fr:
    DeviceCsvForm:
        import-title: Importer des dispositifs
        multiple-insert: dispositifs(s) importé(s)
        uri-example: http://opensilex.org/id/device/rasperry_pi_4B
        type-example: vocabulary:SensingDevice
</i18n>
