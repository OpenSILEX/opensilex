<template>
    <b-modal
        ref="soModalRef"
        size="lg"
        ok-only
        :static="true"
        @hide="requiredField = false"
        @show="requiredField = true"
    >

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
                            placeholder="DeviceList.filter.rdfTypes-placeholder"
                        ></opensilex-TypeForm>
                    </b-col>
                    <b-col>
                        <opensilex-CSVSelectorInputForm :separator.sync="separator"></opensilex-CSVSelectorInputForm>
                    </b-col>
                </b-row>

                <b-button @click="csvExport" class="greenThemeColor">
                    {{ $t("OntologyCsvImporter.downloadTemplate") }}
                </b-button>
            </ValidationObserver>
        </div>

        <template v-slot:modal-footer>
            <button
                type="button"
                class="btn greenThemeColor"
                v-on:click="hide(false)"
            >
                {{ $t('component.common.close') }}
            </button>
        </template>

    </b-modal>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
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
    dataTypesToExampleTranslateKey: Map<string, string>

    @Ref("validatorRefTemplate") readonly validatorRefTemplate!: any;
    @Ref("soModalRef") readonly soModalRef!: any;

    created() {
        this.dataTypesToExampleTranslateKey = new Map();
        this.dataTypesToExampleTranslateKey.set("xsd:integer", "integer");
        this.dataTypesToExampleTranslateKey.set("xsd:decimal", "decimal");
        this.dataTypesToExampleTranslateKey.set("xsd:string", "string");
        this.dataTypesToExampleTranslateKey.set("xsd:anyURI", "uri");
        this.dataTypesToExampleTranslateKey.set("xsd:date", "date");
        this.dataTypesToExampleTranslateKey.set("xsd:datetime", "datetime");
        this.dataTypesToExampleTranslateKey.set("xsd:boolean", "boolean");
    }

    get user() {
        return this.$store.state.user;
    }

    reset() {
        this.validatorRefTemplate.reset();
    }

    show() {
        this.soModalRef.show();
    }
    hide() {
        this.soModalRef.hide();
    }

    validateTemplate() {
        return this.validatorRefTemplate.validate();
    }

    private getCustomPropertyDescription(property: VueRDFTypePropertyDTO, isDataProperty: boolean) {
        let parts = Array.of(
            // property name
            this.$t("DeviceCsvTemplateGenerator.property-name"),
            " : ",
            property.name,

            // data/object type
            "\n",
            this.$t("ScientificObjectCSVTemplateGenerator.data-type"),
            " : ",
            this.getPropertyLabel(property.target_property, isDataProperty),

            // property description
            "\n",
            this.$t("DeviceCsvTemplateGenerator.property-description"),
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

        // example
        if (isDataProperty) {
            parts.push(
                "\n",
                this.$t("component.common.example"),
                " : ",
                this.getPropertyExample(property.target_property, isDataProperty),
            );
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
            "\n",
            (example && example.length > 0) ?
                (this.$t("component.common.example") + " : " + this.$t(example))
                : ""
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
                        let propURI = property.uri;
                        result.dataProperties.set(propURI, property);
                    }
                    for (let property of http.response.result.object_properties) {
                        let propURI = property.uri;
                        result.objectProperties.set(propURI, property);
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
            this.getPropertyDescription("DeviceImportHelp.uri-help", false, "DeviceCsvTemplateGenerator.uri-example"),
            this.getPropertyDescription("DeviceImportHelp.type-help", false, "DeviceCsvTemplateGenerator.type-example"),
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

    getPropertyLabel(dataTypeUri: string, isDataProperty: boolean): string {
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
            if (type && type.name && type.name.length > 0) {
                label = type.name + " (URI)"
            }
        }

        if (!label || label.length == 0) {
            return "";
        }

        return label.charAt(0).toUpperCase() + label.slice(1);
    }

    getPropertyExample(dataTypeUri: string, isDataProperty: boolean) {
        if (!dataTypeUri) {
            return;
        }

        if(isDataProperty){
            let key = this.dataTypesToExampleTranslateKey.get(dataTypeUri);
            let translateKey = "datatype-example." + key;
            return this.$t(translateKey);
        }else{
             // #TODO get type concerned by property and add skos:example property associated to type
            // need to add those translated example into ontologies,  for each type which are range of an object-property
        }

        // #TODO take care of multi-valued properties
    }
}
</script>

<i18n>
en:
    DeviceCsvTemplateGenerator:
        property-name: Property name
        property-description: Property description
        uri-example: http://opensilex.org/id/device/rasperry_pi_4B
        type-example: vocabulary:SensingDevice
fr:
    DeviceCsvTemplateGenerator:
        property-name: Nom de la propriété
        property-description: Description de la propriété
        uri-example: http://opensilex.org/id/device/rasperry_pi_4B
        type-example: vocabulary:SensingDevice
</i18n>



