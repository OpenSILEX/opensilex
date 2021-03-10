<template>
  <ValidationObserver ref="validatorRef" v-if="form.rdf_type">
    
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="GermplasmForm.uri"
      helpMessage="GermplasmForm.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- type -->
    <opensilex-TypeForm
      :type.sync="form.rdf_type"
      :baseType="$opensilex.Oeso.GERMPLASM_TYPE_URI"
      :required="true"
      helpMessage="GermplasmForm.type-help"
      disabled
    ></opensilex-TypeForm>
    
    <!-- label -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="GermplasmForm.name"
      type="text"
      :required="true"
      helpMessage="GermplasmForm.name-help"
    ></opensilex-InputForm>

    <!-- synonyms -->
    <opensilex-TagInputForm
      v-if="$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.VARIETY_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.ACCESSION_TYPE_URI)"
      :value.sync="form.synonyms"
      label="GermplasmForm.subtaxa"
      helpMessage="GermplasmForm.subtaxa-help"
      variant="primary"
    ></opensilex-TagInputForm>

    <!-- synonyms -->
    <opensilex-TagInputForm
      v-else
      :value.sync="form.synonyms"
      label="GermplasmForm.synonyms"
      helpMessage="GermplasmForm.synonyms-help"
      variant="primary"
    ></opensilex-TagInputForm>
    <!-- <label for="tags-basic">Type a new tag and press enter</label>
    <b-form-tags
        v-model="value"        
    ></b-form-tags> -->

    <!-- code -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.code"
      label="GermplasmForm.code"
      type="text"
      helpMessage="GermplasmForm.code-help"
    ></opensilex-InputForm>
    
    <!-- species -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      label="GermplasmForm.species"
      type="text"
      helpMessage="GermplasmForm.species-help"
    ></opensilex-InputForm>
 
    <!-- variety -->
    <opensilex-InputForm
      v-if=" !($opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.VARIETY_TYPE_URI))"
      :value.sync="form.variety"
      label="GermplasmForm.variety"
      type="text"
      helpMessage="GermplasmForm.variety-help"
    ></opensilex-InputForm>
    

    <!-- accession -->
    <opensilex-InputForm
      v-if=" !($opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.VARIETY_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.ACCESSION_TYPE_URI))"
      :value.sync="form.accession"
      label="GermplasmForm.accession"
      type="text"
      helpMessage="GermplasmForm.accession-help"
    ></opensilex-InputForm>
    
    <!-- institute -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.institute"
      label="GermplasmForm.institute"
      type="text"
      helpMessage="GermplasmForm.institute-help"
    ></opensilex-InputForm>

    <!-- website -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.website"
      label="GermplasmForm.website"
      type="url"
      rules="url"
      helpMessage="GermplasmForm.website-help"
    ></opensilex-InputForm>
    
    <!-- year -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.production_year"
      label="GermplasmForm.year"
      type="text"
      helpMessage="GermplasmForm.year-help"
    ></opensilex-InputForm>
    
    <!-- comment -->
    <opensilex-InputForm
      :value.sync="form.description"
      label="GermplasmForm.comment"
      type="text"
      helpMessage="GermplasmForm.comment-help"
    ></opensilex-InputForm>

    <opensilex-GermplasmAttributesTable
      ref="germplasmAttributesTable"
      :editMode="editMode"
      :attributesArray='attributesArray'
    ></opensilex-GermplasmAttributesTable>

  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, Ref  } from "vue-property-decorator";
import Vue from "vue";
import { GermplasmCreationDTO, GermplasmGetSingleDTO, GermplasmService, OntologyService, ResourceTreeDTO } from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class GermplasmForm extends Vue {
  $opensilex: any;
  $store: any;
  service: GermplasmService;


  get user() {
    return this.$store.state.user;
  }

  get languages() {
    let langs = [];
    Object.keys(this.$i18n.messages).forEach(key => {
      langs.push({
        id: key,
        label: this.$i18n.t("component.header.language." + key)
      });
    });
    return langs;
  }

  uriGenerated = true;

  attributesArray = [];

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        rdf_type: null,
        name: null,
        code: null,
        species: null,
        variety: null,
        accession: null,
        institute: null,
        production_year: null,
        description: null,
        synonyms:[],
        metadata: null
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      uri: null,
      rdf_type: null,
      name: null,
      code: null,
      species: null,
      variety: null,
      accession: null,
      institute: null,
      production_year: null,
      description: null,
      synonyms:[],
      metadata: null
    };
  }
  @Ref("germplasmAttributesTable") readonly table!: any;

  update(form) {
    form.metadata = this.table.pushAttributes();
    return this.$opensilex
      .getService("opensilex.GermplasmService")
      .updateGermplasm(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Germplasm updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
  
  getAttributes(form) {
    this.attributesArray = [];
    if (form.metadata != null) {   
      for (const property in form.metadata) {
        let att = {
          attribute: property,
          value: form.metadata[property]
        }
        this.attributesArray.push(att);
      } 
    }
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  GermplasmForm:
    name: Name
    name-help: Name used to define this germplasm
    uri: URI
    uri-help: Unique germplasm identifier
    type: Type
    type-help: Germplasm Type
    species : Species URI
    species-help: Species URI of the germplasm
    variety : Variety URI
    variety-help: Variety URI of the germplasm
    accession: Accession URI
    accession-help: Accession URI of the germplasm
    institute: Institute
    institute-help: The code of the institute which the germplasm comes from
    comment: Description
    comment-help: Description associated to the germplasm 
    year: Production Year
    year-help: Year when the ressource has been produced
    synonyms: Synonyms
    synonyms-help: Fill with a synonym and press Enter
    subtaxa: Subtaxa
    subtaxa-help: Fill with a subtaxa and press Enter
    code: Code
    code-help: The code of the germplasm
    website: Web site
    website-help: the web page of the institute or the germplasm

fr:
  GermplasmForm:
    name: Nom
    name-help: Nom du germplasm
    uri: URI
    uri-help: Identifiant unique du germplasm
    type: Type
    type-help: Type du germplasm
    species : URI de l'espèce
    species-help: URI de l'espèce
    variety : URI de variété
    variety-help: URI de la variété
    accession: URI d'accession
    accession-help: Accession URI of the germplasm
    institute: institut
    institute-help: Code de l'institut dont provient le germplasm
    comment: Description
    comment-help: Description associée au germplasm
    year: Année de production
    year-help: Year when the ressource has been produced
    synonyms: Synonymes
    synonyms-help: Entrer un synonyme et appuyer sur Entrée
    subtaxa: Subtaxa
    subtaxa-help: Entrer un subtaxa et appuyer sur Entrée
    code: Code
    code-help: Code de la ressource génétique
    website: Site web
    website-help: page web de l'institut ou de la ressource plus spécifique
</i18n>

