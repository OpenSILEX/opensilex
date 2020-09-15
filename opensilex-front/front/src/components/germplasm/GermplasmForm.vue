<template>
  <ValidationObserver ref="validatorRef" v-if="form.rdfType">
    
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="GermplasmForm.uri"
      helpMessage="GermplasmForm.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- rdfType -->
    <opensilex-TypeForm
      :type.sync="form.rdfType"
      :baseType="$opensilex.Oeso.GERMPLASM_TYPE_URI"
      :required="true"
      helpMessage="GermplasmForm.rdfType-help"
      disabled
    ></opensilex-TypeForm>
    
    <!-- label -->
    <opensilex-InputForm
      :value.sync="form.label"
      label="GermplasmForm.label"
      type="text"
      :required="true"
      helpMessage="GermplasmForm.label-help"
    ></opensilex-InputForm>

    <!-- synonyms -->
    <opensilex-TagInputForm
      :value.sync="form.synonyms"
      label="GermplasmForm.synonyms"
      helpMessage="GermplasmForm.synonyms-help"
      variant="primary"
    ></opensilex-TagInputForm>
    <!-- <label for="tags-basic">Type a new tag and press enter</label>
    <b-form-tags
        v-model="value"        
    ></b-form-tags> -->
    
    <!-- fromSpecies -->
    <opensilex-InputForm
      v-if= '!form.rdfType.endsWith("Species")'
      :value.sync="form.fromSpecies"
      label="GermplasmForm.fromSpecies"
      type="text"
      helpMessage="GermplasmForm.species-help"
    ></opensilex-InputForm>
 
    <!-- fromVariety -->
    <opensilex-InputForm
      v-if= '! (form.rdfType.endsWith("Species") || form.rdfType.endsWith("Variety"))'
      :value.sync="form.fromVariety"
      label="GermplasmForm.fromVariety"
      type="text"
      helpMessage="GermplasmForm.variety-help"
    ></opensilex-InputForm>
    
    <!-- fromAccession -->
    <opensilex-InputForm
      v-if= '! (form.rdfType.endsWith("Species") || form.rdfType.endsWith("Variety") || form.rdfType.endsWith("Accession"))'
      :value.sync="form.fromAccession"
      label="GermplasmForm.fromAccession"
      type="text"
      helpMessage="GermplasmForm.accession-help"
    ></opensilex-InputForm>
    
    <!-- institute -->
    <opensilex-InputForm
      v-if= '!form.rdfType.endsWith("Species")'
      :value.sync="form.institute"
      label="GermplasmForm.institute"
      type="text"
      helpMessage="GermplasmForm.institute-help"
    ></opensilex-InputForm>
    
    <!-- year -->
    <opensilex-InputForm
      v-if= '!form.rdfType.endsWith("Species")'
      :value.sync="form.productionYear"
      label="GermplasmForm.year"
      type="text"
      helpMessage="GermplasmForm.year-help"
    ></opensilex-InputForm>
    
    <!-- comment -->
    <opensilex-InputForm
      :value.sync="form.comment"
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
import { GermplasmCreationDTO, GermplasmGetSingleDTO, GermplasmSearchDTO, GermplasmService, OntologyService, ResourceTreeDTO } from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class GermplasmForm extends Vue {
  $opensilex: any;
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
        rdfType: null,
        label: null,
        code: null,
        fromSpecies: null,
        fromVariety: null,
        fromAccession: null,
        institute: null,
        productionYear: null,
        comment: null,
        synonyms:[],
        attributes: null
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
      rdfType: null,
      label: null,
      code: null,
      fromSpecies: null,
      fromVariety: null,
      fromAccession: null,
      institute: null,
      productionYear: null,
      comment: null,
      synonyms:[],
      attributes: null
    };
  }
  @Ref("germplasmAttributesTable") readonly table!: any;

  update(form) {
    form.attributes = this.table.pushAttributes();
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
    if (form.attributes != null) {   
      for (const property in form.attributes) {
        let att = {
          attribute: property,
          value: form.attributes[property]
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
    label: Name
    label-help: Name used to define this germplasm
    uri: URI
    uri-help: Unique germplasm identifier
    rdfType: Type
    rdfType-help: Germplasm Type
    fromSpecies : Species URI
    species-help: Species URI of the germplasm
    fromVariety : Variety URI
    variety-help: Variety URI of the germplasm
    fromAccession: Accession URI
    accession-help: Accession URI of the germplasm
    institute: Institute
    institute-help: The code of the institute which the sgermplasm comes from
    comment: Comment
    comment-help: Description associated to the germplasm 
    year: Production Year
    year-help: Year when the ressource has been produced
    synonyms: Synonyms
    synonyms-help: Fill with a synonym and press Enter

fr:
  GermplasmForm:
    label: Nom
    label-help: Nom du germplasm
    uri: URI
    uri-help: Identifiant unique du germplasm
    rdfType: Type
    rdfType-help: Type du germplasm
    fromSpecies : URI de l'espèce
    species-help: URI de l'espèce
    fromVariety : URI de variété
    variety-help: URI de la variété
    fromAccession: URI d'accession
    accession-help: Accession URI of the germplasm
    institute: institut
    institute-help: Code de l'institut dont provient le germplasm
    comment: Commentaire
    comment-help: Description associée au germplasm
    year: Année de production
    year-help: Year when the ressource has been produced
    synonyms: Synonymes
    synonyms-help: Entrer un synonyme et appuyer sur Entrée
</i18n>

