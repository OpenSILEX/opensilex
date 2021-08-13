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
                            :baseType="isMove ? this.$opensilex.Oeev.MOVE_TYPE_URI : this.$opensilex.Oeev.EVENT_TYPE_URI"
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

@Component
export default class GenerateEventTemplate extends Vue {
    $opensilex: any;
    $store: any;
    $t: any;
    $i18n: any;
    $papa: any;

    requiredField: boolean = false;
    separator = ",";
    types: any[] = [];

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

    private getCustomPropertyDescription(property) {

        let parts = Array.of(
            property.name,

            // datatype
            "\n",
            this.$t("ScientificObjectCSVTemplateGenerator.data-type"),
            " : ",
            this.getDataTypeLabel(property.target_property),

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
        let ontoService = this.$opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
        let promises = [];

        for (let type of this.types) {
            let typePromise = ontoService.getRDFTypeProperties(type, this.$opensilex.Oeev.EVENT_TYPE_URI)
                .then(http => {
                    let properties = {};
                    for (let dataProp of http.response.result.data_properties) {
                        let propURI = dataProp.property;
                        properties[propURI] = dataProp;
                    }

                    for (let objProp of http.response.result.object_properties) {
                        let propURI = objProp.property;
                        properties[propURI] = objProp;
                    }

                    return {
                        uri: type,
                        properties: properties,
                    };
                });

            promises.push(typePromise);
        }

        return promises;
    }

    generateCSV(typeModels) {

        let headers = ["uri", "rdfType", "isInstant", "start", "end", "targets", "description"];
        let headersDescription = [
            this.getPropertyDescription("Event.uri-help", false, "Event.uri-example"),
            this.getPropertyDescription("Event.type-help", false, "Event.type-example"),
            this.getPropertyDescription("Event.is-instant-help", true, "Event.is-instant-example"),
            this.getPropertyDescription("Event.start-help", false, "Event.start-example"),
            this.getPropertyDescription("Event.end-help", false, "Event.start-example"),
            this.getPropertyDescription("Event.target-help", true, "Event.targets-example"),
            this.getPropertyDescription("component.common.description", false),
        ];

        if (this.isMove) {

            headers.push("from", "to", "coordinates", "x", "y", "z", "textualPosition");
            headersDescription.push(
                this.getPropertyDescription("Position.from-help", false, "Position.from-placeholder"),
                this.getPropertyDescription("Position.to-help", false, "Position.to-placeholder"),
                this.getPropertyDescription("Position.coordinates-help", true, "Position.coordinates-placeholder"),
                this.getPropertyDescription("Position.x-help", false, "Position.x-placeholder"),
                this.getPropertyDescription("Position.y-help", false, "Position.y-placeholder"),
                this.getPropertyDescription("Position.z-help", false, "Position.z-placeholder"),
                this.getPropertyDescription("Position.textual-position-help", false, "Position.textual-position-placeholder"),
            );
        }


        let visitedProperties = new Set();

        // for each type, add all non visited property header column and description
        for (let typeResult of typeModels) {
            for (let propURI in typeResult.properties) {
                if (!visitedProperties.has(propURI)) {
                    visitedProperties.add(propURI);

                    headers.push(propURI);

                    let property = typeResult.properties[propURI];
                    headersDescription.push(this.getCustomPropertyDescription(property));
                }
            }
        }

        let data = [headers, headersDescription];

        let typeIndex = headers.indexOf("rdfType");
        let isInstantIndex = headers.indexOf("isInstant");
        let endIndex = headers.indexOf("end");
        let targetIndex = headers.indexOf("targets");

        if (this.targets.length > 0) {

            // Set a type if a type and only one has been selected.
            let generatedType = typeModels && typeModels.length == 1 ? typeModels[0].uri : undefined;

            // append row with pre-filled target column
            this.targets.forEach(target => {

                if (target && target.length > 0) {
                    let row = new Array(headers.length).fill('');

                    row[typeIndex] = generatedType;
                    row[isInstantIndex] = "true";
                    row[endIndex] = new Date().toISOString();
                    row[targetIndex] = target;

                    data.push(row);
                }
            });

        // no target, append a row by type
        } else if (typeModels && typeModels.length > 0) {
            typeModels.forEach(typeModel => {
                let row = new Array(headers.length).fill('');

                row[typeIndex] = typeModel.uri;
                row[isInstantIndex] = "true";
                row[endIndex] = new Date().toISOString();

                data.push(row);
            });
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

                    let templateName = this.isMove ? "move" : "event";
                    templateName += "_template_" + new Date().getTime();

                    this.$papa.download(this.$papa.unparse(data, {delimiter: this.separator}), templateName);
                }));

            }
        });
    }

    getDataTypeLabel(dataTypeUri: string): string {
        if (!dataTypeUri) {
            return undefined;
        }
        let type = this.$opensilex.getDatatype(dataTypeUri);
        let label = "URI";
        if (type) {
            label = this.$t(type.label_key);
        }
        return label.charAt(0).toUpperCase() + label.slice(1);
    }
}
</script>