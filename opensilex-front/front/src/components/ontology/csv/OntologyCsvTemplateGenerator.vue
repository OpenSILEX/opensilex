<template>
    <b-modal
        ref="soModalRef"
        size="lg"
        ok-only
        :static="true"
        @hide="requiredField = false"
        @show="requiredField = true"
    >

        <template v-slot:modal-title>{{ $t("OntologyCsvTemplateGenerator.title") }}</template>

        <div>
            <ValidationObserver ref="validatorRefTemplate">
                <b-row>
                    <b-col cols="8">
                        <opensilex-TypeForm
                            :type.sync="types"
                            :multiple="true"
                            :required="false"
                            :baseType="baseType"
                            :ignoreRoot="false"
                            :placeholder="typePlaceholder"
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
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {VueDataTypeDTO, VueJsOntologyExtensionService, VueObjectTypeDTO, VueRDFTypeDTO, VueRDFTypePropertyDTO} from "../../../lib";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {Store} from "vuex";
import {OntologyService} from "opensilex-core/api/ontology.service";
import { PropertiesByDomainDTO } from 'opensilex-core/index';
import Rdfs from "../../../ontologies/Rdfs";
import { createUriListFromGetPropertiesResult, sortProperties } from '../OntologyTools';

interface GetTypesPromisesReturnType{
  uri: string,
  dataProperties: Map<string, VueRDFTypePropertyDTO>,
  objectProperties: Map<string, VueRDFTypePropertyDTO>,
  propertiesOrder: Array<string> | undefined,
  fromTypeModel: VueRDFTypeDTO
}

interface HeadersAndDescriptions {
  headers: Array<string>,
  headersDescriptions: Array<string>
}

@Component
export default class OntologyCsvTemplateGenerator extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: Store<any>;
    $t: any;
    $i18n: any;
    $papa: any;

    /**
     * URI of base type : used for type+form and vocabulary search
     */
    @Prop()
    baseType: string;

    @Prop()
    typePlaceholder: string;

    /**
     * Template file prefix. The final file name will be : templatePrefix + _csv_template_  + <current_datetime> + .csv
     */
    @Prop({default: "csv_import_template"})
    templatePrefix: string;

    @Prop()
    uriHelp: string;

    @Prop()
    uriExample: string;

    @Prop()
    typeHelp: string;

    @Prop()
    typeExample: string;

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
            this.$t("OntologyCsvTemplateGenerator.property-name"),
            " : ",
            property.name,

            // data/object type
            "\n",
            this.$t("OntologyCsvTemplateGenerator.data-type"),
            " : ",
            this.getPropertyLabel(property.target_property, isDataProperty),

            // property description
            "\n",
            this.$t("OntologyCsvTemplateGenerator.property-description"),
            " : ",
            property.comment,

            // required
            "\n",
            this.$t("OntologyCsvTemplateGenerator.required"),
            " : ",
            property.is_required ? this.$t("component.common.yes") : this.$t("component.common.no")
        );

        if (property.is_list) {
            parts.push("\n", this.$t("OntologyCsvTemplateGenerator.property-list"));
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
            this.$t("OntologyCsvTemplateGenerator.required"),
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

    getTypesPromises() : Array<Promise<GetTypesPromisesReturnType>>{
        let ontoService: VueJsOntologyExtensionService = this.$opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
        let promises = [];

        for (let type of this.types) {
          let typePromise = ontoService.getRDFTypeProperties(type, this.baseType)
                .then(http => {
                    let result = {
                        uri: type,
                        dataProperties: new Map<string, VueRDFTypePropertyDTO>(),
                        objectProperties: new Map<string, VueRDFTypePropertyDTO>(),
                        propertiesOrder: http.response.result.properties_order,
                        fromTypeModel: http.response.result
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

  /**
   * Function that is used to create headers and header desriptions when no order has ever been defined in Vocabulary.
   * Places the columns that come from higher up in the hierarchy first (baseType -> betweenTypes -> type)
   * Returns an object containing a headers array and a headersDescription
   */
    async generateHeadersNoOrderFound(typeModels: GetTypesPromisesReturnType[], headers: string[], headersDescription: string[]): Promise<HeadersAndDescriptions>{
      let visitedProperties = new Set<string>();

      let normalOntologyService : OntologyService = this.$opensilex.getService("opensilex.OntologyService");

      //Call service to work out which properties come from where
      let propertiesByDomainHttpResponse = await normalOntologyService.getPropertiesByDomainHierarchyUsingRestrictions(this.baseType, this.types);
      let propertiesByDomain: PropertiesByDomainDTO[] = propertiesByDomainHttpResponse.response.result;

      //Create propertyUri-isDataProperty Map and propertyUri-VueRdfTypePropertyDtoMap
      let propertyIsDataPropMap = new Map<string, boolean>();
      let propertyVueRdfTypePropertyDtoMap = new Map<string, VueRDFTypePropertyDTO>();
      for(let typeResult of typeModels){
        for(let typeResultPropertyUri of typeResult.dataProperties.keys()){
          propertyIsDataPropMap.set(typeResultPropertyUri, true);
          propertyVueRdfTypePropertyDtoMap.set(typeResultPropertyUri, typeResult.dataProperties.get(typeResultPropertyUri))
        }
        for(let typeResultPropertyUri of typeResult.objectProperties.keys()){
          propertyIsDataPropMap.set(typeResultPropertyUri, false);
          propertyVueRdfTypePropertyDtoMap.set(typeResultPropertyUri, typeResult.objectProperties.get(typeResultPropertyUri))
        }
      }

      //Follow correct order
      for(let propertiesByDomainDTO of propertiesByDomain){
        let propsOfDomainsUris: Array<string> = createUriListFromGetPropertiesResult(propertiesByDomainDTO.properties, this.$opensilex);
        propsOfDomainsUris.forEach(propertyUri => {
          let shortPropertyUri = this.$opensilex.getShortUri(propertyUri);
          if(!visitedProperties.has(shortPropertyUri)){
            visitedProperties.add(shortPropertyUri);
            if(propertyVueRdfTypePropertyDtoMap.has(shortPropertyUri)){
              headers.push(shortPropertyUri);
              headersDescription.push(this.getCustomPropertyDescription(propertyVueRdfTypePropertyDtoMap.get(shortPropertyUri), propertyIsDataPropMap.get(shortPropertyUri)));
            }
          }
        });
      }
      //Add ones that came from higher up in the hierarchy
      for(let typeResult of typeModels){
        for(let typeResultPropertyUri of typeResult.dataProperties.keys()){
          let shortPropertUri = this.$opensilex.getShortUri(typeResultPropertyUri);
          if(visitedProperties.has(shortPropertUri)){
            continue;
          }
          visitedProperties.add(shortPropertUri);
          //If the property is rdfs:label , insert in third position
          if(this.$opensilex.checkURIs(typeResultPropertyUri, Rdfs.LABEL)){
            let copyOfEndHeaders = headers.slice(2, headers.length);
            let copyOfEndDescriptions = headersDescription.slice(2, headersDescription.length);
            headers = headers.slice(0, 2);
            headersDescription = headersDescription.slice(0, 2);
            headers.push(typeResultPropertyUri);
            headersDescription.push(this.getCustomPropertyDescription(typeResult.dataProperties.get(typeResultPropertyUri), true));
            headers.push(...copyOfEndHeaders);
            headersDescription.push(...copyOfEndDescriptions);
            continue;
          }
          headers.push(typeResultPropertyUri);
          headersDescription.push(this.getCustomPropertyDescription(typeResult.dataProperties.get(typeResultPropertyUri), true));
        }
        for(let typeResultPropertyUri of typeResult.objectProperties.keys()){
          let shortPropertUri = this.$opensilex.getShortUri(typeResultPropertyUri);
          if(!visitedProperties.has(shortPropertUri)){
            visitedProperties.add(shortPropertUri);
            headers.push(typeResultPropertyUri);
            headersDescription.push(this.getCustomPropertyDescription(typeResult.objectProperties.get(typeResultPropertyUri), false));
          }
        }
      }
      return {headers: headers, headersDescriptions: headersDescription};
    }

    async generateCSV(typeModels: GetTypesPromisesReturnType[]) {

        let headers = ["uri", "type"];
        let headersDescription = [
            this.getPropertyDescription(this.uriHelp, false, this.uriExample),
            this.getPropertyDescription(this.typeHelp, false, this.typeExample),
        ];

        //If there is more than one type, or if the single type has no order defined then use the default one
        if(typeModels.length > 1 || !typeModels[0].propertiesOrder || typeModels[0].propertiesOrder.length==0){
          let newHeadersAndDesriptions = await this.generateHeadersNoOrderFound(typeModels, headers, headersDescription);
          headers = newHeadersAndDesriptions.headers;
          headersDescription = newHeadersAndDesriptions.headersDescriptions;
        }else{
          let typeModelInfo : GetTypesPromisesReturnType = typeModels[0];
          //Create an array of VueRDFTypePropertyDTO to call the sort function which uses VueRDFTypeDTO#properties_order
          let vueRDFTypePropertyDTOArray : Array<VueRDFTypePropertyDTO> = Array.from(typeModelInfo.objectProperties.values()).concat(Array.from(typeModelInfo.dataProperties.values()));
          vueRDFTypePropertyDTOArray = sortProperties(vueRDFTypePropertyDTOArray, typeModelInfo.fromTypeModel, this.$opensilex);
          vueRDFTypePropertyDTOArray.forEach(e =>{
            headers.push(e.uri);
            if(typeModelInfo.dataProperties.has(e.uri)){
              headersDescription.push(this.getCustomPropertyDescription(typeModelInfo.dataProperties.get(e.uri), true));
            }else{
              headersDescription.push(this.getCustomPropertyDescription(typeModelInfo.objectProperties.get(e.uri), false));
            }
          })
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
                  this.generateCSV(results).then(data => {
                    let templateName = this.templatePrefix + "_csv_template_" + new Date().getTime();
                    this.$papa.download(
                        this.$papa.unparse(data, {delimiter: this.separator}), templateName
                    );
                  })
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
    OntologyCsvTemplateGenerator:
        title: Generate CSV template
        property-name: Property name
        property-description: Property description
        property-list: This column can be present multiple time to define multiple values
        data-type: Data type
        required: Required
fr:
    OntologyCsvTemplateGenerator:
        title: Générer un modèle de CSV
        property-name: Nom de la propriété
        property-description: Description de la propriété
        property-list: Cette colonne peut être présente plusieurs fois pour définir plusieurs valeurs
        data-type: Type de donnée
        required: Obligatoire
</i18n>



