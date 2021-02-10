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
        <b-row>
          <b-col cols="9">
            <opensilex-ScientificObjectTypeSelector
              label="ScientificObjectCSVTemplateGenerator.type-label"
              :multiple="true"
              :types.sync="types"
              :required="requiredField"
            >
            </opensilex-ScientificObjectTypeSelector>
          </b-col>
          <b-col> </b-col>
        </b-row>
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

@Component
export default class ScientificObjectCSVTemplateGenerator extends Vue {
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

  @Prop({ default: null })
  experiment: string;

  types: any[] = [];

  reset() {
    this.validatorRefTemplate.reset();
    this.experiment = null;
  }

  show() {
    this.soModalRef.show();
  }

  validateTemplate() {
    return this.validatorRefTemplate.validate();
  }

  csvExport() {
    this.validateTemplate().then((isValid) => {
      // fill in large
      if (isValid) {
        let ontoService = this.$opensilex.getService(
          "opensilex-front.VueJsOntologyExtensionService"
        );

        let promises = [];

        for (let type of this.types) {
          promises.push(
            ontoService
              .getClassProperties(
                type,
                this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
              )
              .then((http) => {
                let properties = {};
                for (let dataProp of http.response.result.dataProperties) {
                  let propURI = dataProp.property;
                  properties[propURI] = dataProp;
                }

                for (let objProp of http.response.result.objectProperties) {
                  let propURI = objProp.property;
                  properties[propURI] = objProp;
                }

                return {
                  uri: type,
                  properties: properties,
                };
              })
          );
        }

        Promise.all(promises).then((results) => {
          let headers = [
            "URI",
            "type",
            "name",
            "vocabulary:hasCreationDate",
            "vocabulary:hasDestructionDate",
            "vocabulary:hasFacility",
            "vocabulary:isPartOf",
            "rdfs:comment",
            "geometry",
          ];

          let descriptionByHeaders = {
            URI:
              this.$t("ScientificObjectImportHelp.uri-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.yes"),
            type:
              this.$t("ScientificObjectImportHelp.type-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.yes"),
            name:
              this.$t("ScientificObjectImportHelp.name-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.yes"),
            "vocabulary:hasCreationDate":
              this.$t("ScientificObjectImportHelp.hasCreationDate-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.data-type") +
              ": " +
              this.getDataTypeLabel("xsd:date") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.no"),
            "vocabulary:hasDestructionDate":
              this.$t("ScientificObjectImportHelp.hasDestructionDate-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.data-type") +
              ": " +
              this.getDataTypeLabel("xsd:date") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.no"),
            "vocabulary:hasFacility":
              this.$t("ScientificObjectImportHelp.hasFacility-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.data-type") +
              ": URI" +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.no"),
            "vocabulary:isPartOf":
              this.$t("ScientificObjectImportHelp.isPartOf-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.no"),
            "rdfs:comment":
              this.$t("ScientificObjectImportHelp.comment-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.data-type") +
              ": " +
              this.getDataTypeLabel("xsd:string") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.no"),
            geometry:
              this.$t("ScientificObjectImportHelp.geometry-help") +
              "\n" +
              this.$t("ScientificObjectCSVTemplateGenerator.required") +
              ": " +
              this.$t("component.common.no"),
          };

          let datatypesByHeaders = {};

          for (let result of results) {
            for (let propURI in result.properties) {
              if (propURI != "rdfs:label") {
                if (!datatypesByHeaders[propURI]) {
                  datatypesByHeaders[propURI] = [];
                }
                datatypesByHeaders[propURI].push(result.uri);

                if (headers.indexOf(propURI) < 0) {
                  headers.push(propURI);
                }

                if (!descriptionByHeaders[propURI]) {
                  let prop = result.properties[propURI];
                  let description = prop.name;
                  description +=
                    "\n" +
                    this.$t("ScientificObjectCSVTemplateGenerator.data-type") +
                    ": " +
                    this.getDataTypeLabel(prop.targetProperty);
                  description +=
                    "\n" +
                    this.$t("ScientificObjectCSVTemplateGenerator.required") +
                    ": ";

                  if (prop.isRequired) {
                    description += this.$t("component.common.yes");
                  } else {
                    description += this.$t("component.common.no");
                  }

                  if (prop.isList) {
                    description +=
                      "\n" +
                      this.$t(
                        "ScientificObjectCSVTemplateGenerator.property-list"
                      );
                  }
                  descriptionByHeaders[propURI] = description;
                }
              }
            }
          }

          let headersDescription = [];

          for (let header of headers) {
            let description = descriptionByHeaders[header];
            if (
              datatypesByHeaders[header] &&
              datatypesByHeaders[header].length != this.types.length
            ) {
              description +=
                "\n" +
                this.$t(
                  "ScientificObjectCSVTemplateGenerator.property-available-for"
                ) +
                ":\n" +
                datatypesByHeaders[header].join("\n");
            }

            headersDescription.push(description);
          }

          let arrData = [headers, headersDescription];

          console.error(arrData);

          this.$papa.download(
            this.$papa.unparse(arrData),
            "scientificObjectTemplate"
          );
        });
      }
    });
  }

  dtoToSelectNode(dto) {
    return {
      id: dto.uri,
      label: dto.name,
    };
  }

  getDataTypeLabel(dataTypeUri: string): string {
    if (!dataTypeUri) {
      return undefined;
    }
    let type = this.$opensilex.getDatatype(dataTypeUri);
    let label = "URI";
    if (type) {
      label = this.$t(type.labelKey);
    }
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
}
</script>
<i18n>
en :
  ScientificObjectCSVTemplateGenerator:
    title: Generate CSV template
    type-label: Scientific object types
    data-type: Data type
    required: Required
    property-list: This column can be present multiple time to define multiple values
    property-available-for: Property available only for types
    example :
      column-data-type : "Column data type: "
fr :
  ScientificObjectCSVTemplateGenerator:
    title: Générer un modèle de CSV
    data-type: Type de donnée
    type-label: Types d'objets scientifiques
    required: Obligatoire
    property-list: Cette colonne peut être présente plusieurs fois pour définir plusieurs valeurs
    property-available-for: Propriétée valable seulement pour les types
    example :
      column-data-type : "Type de données colonne : " 
 </i18n>