<template>
    <b-modal
        ref="soModalRef"
        size="lg"
        ok-only
        :static="true"
        @hide="requiredField = false"
        @show="requiredField = true"
    >

        <template v-slot:modal-footer>
      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="hide(false)"
      >{{ $t('component.common.close') }}</button>
        </template>

        <!-- <template v-slot:modal-ok>{{ $t("component.common.close") }}</template> -->
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

                <b-button @click="csvExport" class="greenThemeColor">
                    {{ $t("OntologyCsvImporter.downloadTemplate") }}
                </b-button>
            </ValidationObserver>
        </div>
    </b-modal>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {VueJsOntologyExtensionService, VueRDFTypePropertyDTO} from "../../../../lib";
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";

@Component
export default class GenerateEventTemplate extends Vue {
    $opensilex: OpenSilexVuePlugin;
    vueOntologyService: VueJsOntologyExtensionService;

    $store: any;
    $t: any;
    $i18n: any;
    $papa: any;

    requiredField: boolean = false;
    separator = ",";
    types: Array<string> = [];

    @Ref("validatorRefTemplate") readonly validatorRefTemplate!: any;
    @Ref("soModalRef") readonly soModalRef!: any;

    @Prop({default: false})
    isMove: boolean;

    created(){
       this.vueOntologyService = this.$opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
    }

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

    hide() {
        this.soModalRef.hide();
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
        let promises = [];

        for (let type of this.types) {
            let typePromise = this.vueOntologyService.getRDFTypeProperties(type, this.$opensilex.Oeev.EVENT_TYPE_URI)
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

      let headers = ["uri", "rdfType", "isInstant", "start", "end", "targets", "description"];

      // list of properties URI to exclude from custom properties
      let managedProperties = [
          this.$opensilex.Oeev.IS_INSTANT,
          this.$opensilex.Time.HAS_BEGINNING,
          this.$opensilex.Time.HAS_END,
          this.$opensilex.Oeev.CONCERNS,
          this.$opensilex.Rdfs.COMMENT
      ];

      let headersDescription = [
            this.getPropertyDescription("Event.uri-help", false, "Event.uri-example"),
            this.getPropertyDescription("Event.type-help", false, "Event.type-example"),
            this.getPropertyDescription("Event.is-instant-help", true, "Event.is-instant-example"),
            this.getPropertyDescription("Event.start-help", false, "Event.start-example"),
            this.getPropertyDescription("Event.end-help", false, "Event.start-example"),
            this.getPropertyDescription("Event.target-help", true, "Event.targets-example"),
            this.getPropertyDescription("component.common.description", false),
        ];

        if (this.generateMoveTemplate()) {
            managedProperties.push(this.$opensilex.Oeev.FROM, this.$opensilex.Oeev.TO);

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

        // exclude managed properties -> ensure that no property is present twice into header
        let visitedProperties = new Set(managedProperties.map(property => this.$opensilex.getShortUri(property)));

        // for each type, add all non visited property header column and description
        for (let typeResult of typeModels) {

            let propertyFunction = (propURI: string, property: VueRDFTypePropertyDTO) => {
                if (!visitedProperties.has(this.$opensilex.getShortUri(propURI))) {
                    visitedProperties.add(propURI);

                    headers.push(propURI);
                    headersDescription.push(this.getCustomPropertyDescription(property));
                }
            }

            typeResult.dataProperties.forEach((property, propURI) => {
                propertyFunction(propURI, property);
            });
            typeResult.objectProperties.forEach((property, propURI) => {
                propertyFunction(propURI, property);
            });
        }

        let data = [headers, headersDescription];

        let typeIndex = headers.indexOf("rdfType");
        let isInstantIndex = headers.indexOf("isInstant");
        let endIndex = headers.indexOf("end");
        let targetIndex = headers.indexOf("targets");

        // generate a row per target and per type

        // no target -> empty column
        let generatedTargets = this.targets ? this.targets : [undefined];

        // no type -> empty column
        let generatedTypes = typeModels ? typeModels.map(type => type.uri) : [undefined];

        generatedTargets.forEach(generatedTarget => {
            generatedTypes.forEach(generatedType => {
                let row = new Array(headers.length).fill('');

                row[typeIndex] = generatedType;
                row[isInstantIndex] = "true";
                row[endIndex] = new Date().toISOString();
                row[targetIndex] = generatedTarget;

                data.push(row);
            });
        });

        return data;
    }

    csvExport() {
        this.validateTemplate().then((isValid) => {
            // fill in large
            if (isValid) {

                let typePromises = this.getTypesPromises();

                Promise.all(typePromises).then((results => {

                    let data = this.generateCSV(results)

                    let templateName = this.generateMoveTemplate() ? "move" : "event";
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

    generateMoveTemplate() : boolean {
        // parent component explicit use this component for move template
        if (this.isMove) {
            return true;
        }

        if (this.types.length == 0) {
            return false;
        }

        for (const type of this.types) {
            if (this.$opensilex.Oeev.checkURIs(type, this.$opensilex.Oeev.MOVE_TYPE_URI)) {
                return true;
            }
        }
        return false;
    }
}
</script>