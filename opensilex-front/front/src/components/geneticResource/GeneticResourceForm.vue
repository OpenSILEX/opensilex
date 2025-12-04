<template>
  <ValidationObserver ref="validatorRef" v-if="form.rdf_type">
    
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="GeneticResourceForm.uri"
      helpMessage="GeneticResourceForm.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- type -->
    <opensilex-TypeForm
      :type.sync="form.rdf_type"
      :baseType="$opensilex.Oeso.GENETIC_RESOURCE_TYPE_URI"
      :required="true"
      helpMessage="GeneticResourceForm.type-help"
      disabled
    ></opensilex-TypeForm>
    
    <!-- label -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="GeneticResourceForm.name"
      type="text"
      :required="true"
      helpMessage="GeneticResourceForm.name-help"
    ></opensilex-InputForm>

    <!-- synonyms -->
    <opensilex-TagInputForm
      v-if="$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.VARIETY_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.ACCESSION_TYPE_URI)"
      :value.sync="form.synonyms"
      label="GeneticResourceForm.subtaxa"
      helpMessage="GeneticResourceForm.subtaxa-help"
      variant="primary"
      placeholder = "GeneticResourceForm.synonyms"
    ></opensilex-TagInputForm>

    <!-- synonyms -->
    <opensilex-TagInputForm
      v-else
      :value.sync="form.synonyms"
      label="GeneticResourceForm.synonyms"
      helpMessage="GeneticResourceForm.synonyms-help"
      variant="primary"
      placeholder = "GeneticResourceForm.synonyms"
    ></opensilex-TagInputForm>
    <!-- <label for="tags-basic">Type a new tag and press enter</label>
    <b-form-tags
        v-model="value"        
    ></b-form-tags> -->

    <!-- code -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.code"
      label="GeneticResourceForm.code"
      type="text"
      helpMessage="GeneticResourceForm.code-help"
    ></opensilex-InputForm>
    
    <!-- species -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.species"
      label="GeneticResourceForm.species"
      type="text"
      helpMessage="GeneticResourceForm.species-help"
    ></opensilex-InputForm>

    <!-- variety -->
    <opensilex-InputForm
      v-if=" !($opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.VARIETY_TYPE_URI))"
      :value.sync="form.variety"
      label="GeneticResourceForm.variety"
      type="text"
      helpMessage="GeneticResourceForm.variety-help"
    ></opensilex-InputForm>

    <!-- accession -->
    <opensilex-InputForm
      v-if=" !($opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.VARIETY_TYPE_URI) || $opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.ACCESSION_TYPE_URI))"
      :value.sync="form.accession"
      label="GeneticResourceForm.accession"
      type="text"
      helpMessage="GeneticResourceForm.accession-help"
    ></opensilex-InputForm>

    <!-- public geneticResource -->
    <opensilex-FormSelector
      :options="isPublicOptions"
      :selected.sync="form.is_public"
      :required="true"
      label="GeneticResourceForm.isPublic_label"
    ></opensilex-FormSelector>    

    <!-- institute -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.institute"
      label="GeneticResourceForm.institute"
      type="text"
      helpMessage="GeneticResourceForm.institute-help"
    ></opensilex-InputForm>

    <!-- website -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.website"
      label="GeneticResourceForm.website"
      type="url"
      rules="url"
      helpMessage="GeneticResourceForm.website-help"
    ></opensilex-InputForm>

    <!-- group -->
    <opensilex-GroupSelector
      label="GeneticResourceForm.groups"
      :groups.sync="form.groups"
      :disabled="form.is_public"
      :multiple="true"
    ></opensilex-GroupSelector>
    
    <!-- year -->
    <opensilex-InputForm
      v-if="!$opensilex.Oeso.checkURIs(form.rdf_type, $opensilex.Oeso.SPECIES_TYPE_URI)"
      :value.sync="form.production_year"
      label="GeneticResourceForm.year"
      type="text"
      helpMessage="GeneticResourceForm.year-help"
    ></opensilex-InputForm>
    
    <!-- comment -->
    <opensilex-InputForm
      :value.sync="form.description"
      label="GeneticResourceForm.comment"
      type="text"
      helpMessage="GeneticResourceForm.comment-help"
    ></opensilex-InputForm>

    <!-- parents -->
    <opensilex-GeneticResourceParentsModalFormField
        ref="parentsFieldRef"
        :relation_dtos.sync="form.relations"
        :existingParentUrisToLabelsMap="existingParentUrisToLabelsMap"
        :required="false"
        :requiredBlue="false"
    ></opensilex-GeneticResourceParentsModalFormField>

    <opensilex-AttributesTable
      ref="geneticResourceAttributesTable"
      :editMode="editMode"
      :attributesArray='attributesArray'
    ></opensilex-AttributesTable>

  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch  } from "vue-property-decorator";
import Vue from "vue";
import {GeneticResourceGetAllDTO, GeneticResourceService, GeneticResourceUpdateDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import AttributesTable from "../common/forms/AttributesTable.vue";
import GeneticResourceParentsModalFormField from './GeneticResourceParentsModalFormField.vue';
import Oeso from "../../ontologies/Oeso";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class GeneticResourceForm extends Vue {
  @Ref("parentsFieldRef") readonly parentsFieldRef!: GeneticResourceParentsModalFormField;
  @Ref("geneticResourceAttributesTable") readonly table!: any;

  $opensilex;
  $store: any;
  service: GeneticResourceService;
  existingParentUrisToLabelsMap : Map<string, string> = new Map<string, string>();

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
  isPublicOptions = [
    {
      id: true, 
      label: this.$i18n.t("GeneticResourceForm.isPublic")
    },
    {
      id: false, 
      label: this.$i18n.t("GeneticResourceForm.isPrivate")
    },
  ];

  @Prop()
  editMode: boolean;

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
        is_public: null,
        groups: [],
        synonyms:[],
        relations:[],
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
      is_public: null,
      groups: [],
      synonyms:[],
      relations:[],
      metadata: null
    };
  }

  /**
   * Translates the duplicatable relations from individual attributes to a single "relations" attribute
   */
  static readDuplicatableRelations(form: any) : GeneticResourceUpdateDTO{
    let relations = [];
    if(Array.isArray(form.has_parent_geneticResource) && form.has_parent_geneticResource.length !== 0){
      this._pushRelationsForProperty(relations, Oeso.HAS_PARENT_GENETIC_RESOURCE, form.has_parent_geneticResource);
    }
    if(Array.isArray(form.has_parent_geneticResource_m) && form.has_parent_geneticResource_m.length !== 0){
      this._pushRelationsForProperty(relations, Oeso.HAS_PARENT_GENETIC_RESOURCE_M, form.has_parent_geneticResource_m);
    }
    if(Array.isArray(form.has_parent_geneticResource_f) && form.has_parent_geneticResource_f.length !== 0){
      this._pushRelationsForProperty(relations, Oeso.HAS_PARENT_GENETIC_RESOURCE_F, form.has_parent_geneticResource_f);
    }
    let newForm = JSON.parse(JSON.stringify(form));
    newForm.relations = relations;
    return newForm;
  }

  /**
   * Sets relations in advance so that the starting form is correct, and sets the lines of GeneticResourceParentsModalFormField
   */
  private static _pushRelationsForProperty(relations: Array<any>, propertyUri : string, subjects : Array<GeneticResourceGetAllDTO>){
    for(let germ of subjects){
      relations.push(
          {
            inverse: false,
            value: germ.uri,
            property: propertyUri,
          }
      );
    }
  }

  //Ide said this isn't used, it is, by ModalForm in showEditForm()
  onShowEditForm(){
    this.$nextTick(() => this.parentsFieldRef.resetLineListWithInitialLabels()) ;
  }

  update(form) {
    form.metadata = this.table.pushAttributes();
    return this.$opensilex
      .getService("opensilex.GeneticResourceService")
      .updateGeneticResource(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("GeneticResource updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
  
  readAttributes(metadata: { [key: string]: string; }) {
    this.attributesArray = AttributesTable.readAttributes(metadata);
  }

  @Watch("form.is_public")
  resetGroupsOnPublicGeneticResource(){
    if(this.form.is_public) {
      this.form.groups = []
    }
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  GeneticResourceForm:
    name: Name
    name-help: Name used to define this genetic resource
    uri: URI
    uri-help: Unique genetic resource identifier
    type: Type
    type-help: Genetic Resource Type
    species: Species URI
    species-help: Species URI of the genetic resource
    variety: Variety URI
    variety-help: Variety URI of the genetic resource
    accession: Accession URI
    accession-help: Accession URI of the genetic resource
    institute: Institute
    institute-help: The code of the institute which the genetic resource comes from
    comment: Description
    comment-help: Description associated to the genetic resource
    year: Production Year
    year-help: Year when the ressource has been produced
    synonyms: Synonyms
    synonyms-help: Fill with a synonym and press Enter
    subtaxa: Subtaxa
    subtaxa-help: Fill with a subtaxa and press Enter
    code: Code
    code-help: The code of the genetic resource
    website: Web site
    website-help: the web page of the institute or the genetic resource
    groups: Groups
    isPublic: Public
    isPrivate: Private
    isPublic_label: Define status


fr:
  GeneticResourceForm:
    name: Nom
    name-help: Nom de la ressource génétique
    uri: URI
    uri-help: Identifiant unique de ressource génétique
    type: Type
    type-help: Type de ressource génétique
    species: URI de l'espèce
    species-help: URI de l'espèce
    variety: URI de variété
    variety-help: URI de la variété
    accession: URI d'accession
    accession-help: Accession URI of the genetic resource
    institute: institut
    institute-help: Code de l'institut dont provient la ressource génétique
    comment: Description
    comment-help: Description associée à la ressource génétique
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
    groups: Groupes
    isPublic: Public
    isPrivate: Privé
    isPublic_label: Définir le statut

</i18n>

