<template>
    <b-modal
        ref="soModalRef"
        size="lg"
        ok-only
        :static="true"
        @hide="requiredField = false"
        @show="requiredField = true"
    >
        <template v-slot:modal-ok>{{ $t("component.common.close") }}</template>
        <template v-slot:modal-title>{{ $t("ScientificObjectCSVTemplateGenerator.title") }}</template>

        <div>
            <ValidationObserver ref="validatorRefTemplate">

                <b-row>
                    <b-col cols="8">
                        <opensilex-TypeForm
                            :type.sync="types"
                            :multiple="true"
                            :required="false"
                            :baseType="this.$opensilex.Oeso.DEVICE_TYPE_URI"
                            :ignoreRoot="false"
                            placeholder="Event.type-placeholder"
                        ></opensilex-TypeForm>
                    </b-col>
                    <b-col>
                        <opensilex-CSVSelectorInputForm :separator.sync="separator"></opensilex-CSVSelectorInputForm>
                    </b-col>
                </b-row>

                <b-button @click="csvExport" variant="outline-primary">
                    {{ $t("OntologyCsvImporter.downloadTemplate") }}
                </b-button>
            </ValidationObserver>
        </div>
    </b-modal>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {VueDataTypeDTO, VueJsOntologyExtensionService, VueObjectTypeDTO, VueRDFTypePropertyDTO} from "../../../lib";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class DeviceCsvTemplateGenerator extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    $t: any;
    $i18n: any;
    $papa: any;

    requiredField: boolean = false;
    separator = ",";
    types: any[] = [];

    @Ref("validatorRefTemplate") readonly validatorRefTemplate!: any;
    @Ref("soModalRef") readonly soModalRef!: any;


    get user() {
        return this.$store.state.user;
    }

    reset() {
        this.validatorRefTemplate.reset();
    }

    show() {
        this.soModalRef.show();
    }

    validateTemplate() {
        return this.validatorRefTemplate.validate();
    }

    private getCustomPropertyDescription(property: VueRDFTypePropertyDTO, isDataProperty: boolean) {

        let parts = Array.of(
            this.$t("component.common.name"),
            " : ",
            property.name,

            // data/object type
            "\n",
            this.$t("ScientificObjectCSVTemplateGenerator.data-type"),
            " : ",
            this.getDataTypeLabel(property.target_property, isDataProperty),

            // description
            "\n",
            this.$t("component.common.description"),
            " : ",
            property.comment,

            // required
            "\n",
            this.$t("ScientificObjectCSVTemplateGenerator.required"),
            " : ",
            property.is_required ? this.$t("component.common.yes") : this.$t("component.common.no")
        );

        if (property.is_list) {
            parts.push("\n", this.$t("ScientificObjectCSVTemplateGenerator.property-list"));
        }

        return parts.join('');
    }

    private getPropertyDescription(propertyTranslationKey: string, required: boolean, example?: string): string {

        let parts = Array.of(
            this.$t(propertyTranslationKey),
            "\n",
            this.$t("ScientificObjectCSVTemplateGenerator.required"),
            " : ",
            (required) ? this.$t("component.common.yes") : this.$t("component.common.no"),
            ". ",
            (example && example.length > 0) ? "Ex : " + this.$t(example) : ""
        )

        return parts.join('');
    }

    getTypesPromises() {
        let ontoService: VueJsOntologyExtensionService = this.$opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
        let promises = [];

        for (let type of this.types) {
            let typePromise = ontoService.getRDFTypeProperties(type, this.$opensilex.Oeso.DEVICE_TYPE_URI)
                .then(http => {

                    let result = {
                        uri: type,
                        dataProperties: new Map<string, VueRDFTypePropertyDTO>(),
                        objectProperties: new Map<string, VueRDFTypePropertyDTO>(),
                    }

                    for (let property of http.response.result.data_properties) {
                        let propURI = property.property;
                        result.dataProperties.set(propURI,property);
                    }
                    for (let property of http.response.result.object_properties) {
                        let propURI = property.property;
                        result.objectProperties.set(propURI,property);
                    }

                    return result;
                });

            promises.push(typePromise);
        }

        return promises;
    }

    generateCSV(typeModels) {

        let headers = ["uri", "type"];
        let headersDescription = [
            this.getPropertyDescription("Event.uri-help", false, "Event.uri-example"),
            this.getPropertyDescription("Event.type-help", false, "Event.type-example"),
        ];

        let visitedProperties = new Set<string>();

        // for each type, add all non visited property header column and description
        for (let typeResult of typeModels) {

            let propertyFunction = (propURI: string, property: VueRDFTypePropertyDTO, isDataProperty: boolean) => {
                if (!visitedProperties.has(propURI)) {
                    visitedProperties.add(propURI);

                    headers.push(propURI);
                    headersDescription.push(this.getCustomPropertyDescription(property, isDataProperty));
                }
            }

            typeResult.dataProperties.forEach((property, propURI) => {
                propertyFunction(propURI, property, true);
            });
            typeResult.objectProperties.forEach((property, propURI) => {
                propertyFunction(propURI, property, false);
            });

        }

        let data = [headers, headersDescription];

        //append a row by type
        if (typeModels && typeModels.length > 0) {
            let typeIdx = headers.indexOf("type");

            typeModels.forEach(typeModel => {
                let row = new Array(headers.length).fill('');
                row[typeIdx] = typeModel.uri;
                data.push(row);
            });
        } else { // just one empty row
            let row = new Array(headers.length).fill('');
            data.push(row);
        }

        return data;
    }

    csvExport() {
        this.validateTemplate().then((isValid) => {
            // fill in large
            if (isValid) {
                let typePromises = this.getTypesPromises();

                Promise.all(typePromises).then((results => {
                    let data = this.generateCSV(results)
                    let templateName = "device_csv_template_" + new Date().getTime();
                    this.$papa.download(
                        this.$papa.unparse(data, {delimiter: this.separator}), templateName
                    );
                }));

            }
        });
    }

    getDataTypeLabel(dataTypeUri: string, isDataProperty: boolean): string {
        if (!dataTypeUri) {
            return undefined;
        }

        let label;
        if (isDataProperty) {
            let type: VueDataTypeDTO = this.$opensilex.getDatatype(dataTypeUri);
            if (type) {
                label = this.$t(type.label_key);
            }
        } else {
            let type: VueObjectTypeDTO = this.$opensilex.getObjectType(dataTypeUri);
            if (type) {
                label = type.name + " (URI)"
            }
        }

        if (!label || label.length == 0) {
            return "";
        }

        return label.charAt(0).toUpperCase() + label.slice(1);
    }
}
</script>