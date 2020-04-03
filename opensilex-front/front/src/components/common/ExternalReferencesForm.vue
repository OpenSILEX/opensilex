<template>
  <div>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <b-card bg-variant="light">
          <div class="row">
            <div class="col">
              <b-form-group
                label="component.skos.ontologies-references-label"
                label-size="lg"
                label-class="font-weight-bold pt-0"
                class="mb-0"
              >
                <template
                  v-slot:label
                >{{$t('component.skos.ontologies-references-label') }}</template>
              </b-form-group>
              <b-card-text>
                <ul>
                  <li
                    v-for="externalOntologyRef in externalOntologiesRefs"
                    :key="externalOntologyRef.label"
                  >
                    <a
                      v-bind:title="externalOntologyRef.label"
                      v-bind:href="externalOntologyRef.link"
                      v-b-tooltip.v-info.hover.left="externalOntologyRef.description"
                    >{{ externalOntologyRef.label }}</a>
                  </li>
                </ul>
              </b-card-text>
            </div>
            <div class="col">
              <b-form-group>
                <opensilex-FormInputLabelHelper
                  label="component.skos.relation"
                  helpMessage="component.skos.relation-help"
                ></opensilex-FormInputLabelHelper>
                <ValidationProvider
                  :name="$t('component.skos.relation')"
                  :rules="{ 
                  required: true
                }"
                  v-slot="{ errors }"
                >
                  <b-form-select
                    required
                    v-model="currentRelation"
                    :options="options"
                    :placeholder="$t('component.skos.relation-placeholder')"
                  ></b-form-select>
                  <div class="mt-3">
                    current selected relation:
                    <strong>{{ (currentRelation == "") ? 'component.skos.norelation' : currentRelation }}</strong>
                  </div>
                  <div class="error-message alert alert-danger">{{ errors[0] }}</div>
                </ValidationProvider>
              </b-form-group>
              <!-- URI -->
              <b-form-group>
                <opensilex-FormInputLabelHelper
                  label="component.skos.uri"
                  helpMessage="component.skos.-help"
                ></opensilex-FormInputLabelHelper>
                <ValidationProvider
                  :name="$t('component.skos.uri')"
                  :rules="{ 
                  required: true, 
                  regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/ 
                }"
                  v-slot="{ errors }"
                >
                  <b-form-input
                    id="externalUri"
                    v-model.trim="currentExternalUri"
                    type="text"
                    required
                    :placeholder="$t('component.skos.uri-placeholder')"
                  ></b-form-input>
                  <div class="error-message alert alert-danger">{{ errors[0] }}</div>
                </ValidationProvider>
              </b-form-group>
              <b-form-group label-align-sm="right">
                <b-button
                  @click="addRelationsToSkosReferences"
                  variant="info"
                >{{$t('component.skos.add')}}</b-button>
              </b-form-group>
            </div>
          </div>
        </b-card>
      </b-form>
    </ValidationObserver>
    <div>
      <b-table striped hover :items="relations" :fields="fields">
        <template v-slot:head(relation)="data">{{$t(data.label)}}</template>
        <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
        <template v-slot:cell(uri)="data">{{$t(data.value)}}</template>
        <template v-slot:cell(actions)="data">
          <b-button-group size="sm">
            <b-button
              size="sm"
              @click="removeRelationsToSkosReferences(data.item)"
              variant="danger"
            >
              <font-awesome-icon icon="trash-alt" size="sm" />
            </b-button>
          </b-button-group>
        </template>
      </b-table>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Multiselect from "vue-multiselect";
// import { OntologyReferenceDTO } from "opensilex-core/index";
import Vue from "vue";

@Component
export default class ExternalReferencesForm extends Vue {
  $opensilex: any;
  $store: any;

  currentRelation: string = "";
  currentExternalUri: string = "";

  @Prop()
  skosReferences: any;

  relationsInternal: any[] = [];

  externalOntologiesRefs: any[] = [
    {
      link: "http://agroportal.lirmm.fr/",
      label: "AGROPORTAL",
      description: `AgroPortal project is based on five driving agronomic use cases which 
      participate in the design  and orientation of the platform. AgroPortal already offers a robust 
      and stable reference repository highly valuable for the agronomic domain.`
    },
    {
      link: "http://agrovoc.uniroma2.it/agrovoc/agrovoc/en/",
      label: "AGROVOC",
      description: `AGROVOC is a controlled vocabulary covering all areas of interest of the Food and 
        Agriculture Organization (FAO) of the United Nations, including food, nutrition, agriculture, forestry,
        fisheries, scientific and common names of animals and plants, environment, biological notions, techniques
        of plant cultivation and more.. It is published by FAO and edited by a community of experts.`
    },
    {
      link: "https://ncbo.bioontology.org/",
      label: "BioPortal",
      description: `The goal of the National Center for Biomedical Ontology is to
        support biomedical researchers in their knowledge-intensive work, by providing
        online tools and a Web portal enabling them to access, review, and integrate
        disparate ontological resources in all aspects of biomedical investigation
        and clinical practice. A major focus of our work involves the use of 
        biomedical ontologies to aid in the management and analysis of data derived 
        from complex experiments.`
    },
    {
      link: "https://www.cropontology.org/",
      label: "Crop Ontology",
      description: `The Crop Ontology (CO) current objective is to compile validated concepts along with 
      their inter-relationships on anatomy, structure and phenotype of Crops, on trait measurement and 
      methods as well as on Germplasm with the multi-crop passport terms. The concepts of the CO are being
      used to curate agronomic databases and describe the data.`
    },
    {
      link: "https://www.ebi.ac.uk/ols/ontologies/po",
      label: "Plant Ontology",
      description: `The Plant Ontology is a structured vocabulary and database 
      resource that links plant anatomy, morphology and growth and development to plant
       genomics data.`
    },
    {
      link: "http://planteome.org/",
      label: "Planteome",
      description: `Research engine project brings an integrated approach of adopting 
      common annotation standards and a set of reference ontologies for Plants.`
    },
    {
      link: "http://www.ontobee.org/ontology/UO",
      label: "Units of measurement ontology (UO)",
      description: `Metrical units for use in conjunction with the Phenotype And Trait
      Ontology (PATO)`
    },
    {
      link: "http://www.ontology-of-units-of-measure.org/page/om-2",
      label: "Units of Measure (OM)",
      description: `The Ontology of units of Measure (OM) 2.0 models concepts and 
      relations important to scientific research. It has a strong focus on units,
      quantities, measurements, and dimensions.`
    },
    {
      link: "http://www.qudt.org/release2/qudt-catalog.html#vocabs",
      label: "QUDT Ontologies (QUDT)",
      description: `Quantities, Units, Dimensions and Data Types models are based on dimensional analysis expressed in the 
      OWL Web Ontology Language (OWL). The dimensional approach relates each unit 
      to a system of base units using numeric factors and a vector of exponents 
      defined over a set of fundamental dimensions.`
    },
    {
      link: "http://books.xmlschemata.org/relaxng/relax-CHP-19.html",
      label: "XML/XSD Datatype Schemas",
      description: `Discover XML schema languages.`
    }
  ];

  options: any[] = [
    {
      value: "",
      text: "Please select an relation",
      disabled: true
    },
    { value: "exactMatch", text: "exactMatch" },
    { value: "closeMatch", text: "closeMatch" },
    { value: "narrower", text: "narrower" },
    { value: "broader", text: "broader" }
  ];

  fields = [
    {
      key: "relation",
      label: "component.skos.relation",
      sortable: true
    },
    {
      key: "relationURI",
      label: "component.skos.uri",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  get relations() {
    this.relationsInternal = [];
    this.updateRelations("narrower", this.skosReferences.narrower);
    this.updateRelations("broader", this.skosReferences.broader);
    this.updateRelations("closeMatch", this.skosReferences.closeMatch);
    this.updateRelations("exactMatch", this.skosReferences.exactMatch);
    console.log(this.relationsInternal);
    return this.relationsInternal;
  }

  updateRelations(relation: string, references: string[]) {
    for (let index = 0; index < references.length; index++) {
      const element = references[index];
      this.addRelation(relation, element);
    }
  }

  addRelation(currentRelation: string, currentExternalUri: string) {
    this.$set(this.relationsInternal, this.relationsInternal.length, {
      relation: currentRelation,
      relationURI: currentExternalUri
    });
  }

  validateForm() {
    let validatorRef: any = this.$refs.validatorRef;
    return validatorRef.validate();
  }

  addRelationsToSkosReferences() {
    this.validateForm().then(isValid => {
      if (isValid) {
        this.addRelationToSkosReferences(
          this.currentRelation,
          this.currentExternalUri
        );
      }
    });
  }
  addRelationToSkosReferences(
    currentRelation: string,
    currentExternalUri: string
  ) {
    console.log(this.skosReferences, this.skosReferences[currentRelation]);

    this.$nextTick(function() {
      console.log(this.skosReferences, this.skosReferences[currentRelation]);
      if (!this.skosReferences[currentRelation].includes(currentExternalUri)) {
        this.skosReferences[currentRelation].push(currentExternalUri);
      }
    });
  }

  removeRelationsToSkosReferences(row: any) {
    this.skosReferences[row.relation] = this.skosReferences[
      row.relation
    ].filter(function(value, index, arr) {
      return value != row.relationURI;
    });
  }
}
</script>

<style scoped lang="scss">
</style>

