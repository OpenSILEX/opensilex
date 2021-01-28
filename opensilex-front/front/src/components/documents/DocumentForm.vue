<template>
  <b-form>
    <!-- URI or URL-->
    <opensilex-UriForm
      :uri.sync="form.description.uri" 
      label="DocumentForm.uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
      helpMessage="DocumentForm.uri-help"
    ></opensilex-UriForm>

    <!-- rdfType -->
    <opensilex-TypeForm
      :type.sync="form.description.type"
      :baseType="$opensilex.Oeso.DOCUMENT_TYPE_URI"
      helpMessage="DocumentForm.type-help"    
      :required="true"
    ></opensilex-TypeForm>

    <!-- concerns -->
    <opensilex-InputForm
      label="DocumentForm.concerns"
      :value.sync="form.description.concerns"
      :baseType="$opensilex.Oeso.CONCERNS"
      helpMessage="DocumentForm.concerns-help"
    ></opensilex-InputForm>

    <!-- title -->
    <opensilex-InputForm
      :value.sync="form.description.name"
      label="DocumentForm.name"
      type="text"
      helpMessage="DocumentForm.name-help"
      :required="true"
    ></opensilex-InputForm>

    <!--Creator -->
    <opensilex-UserSelector
      :users.sync="form.description.creator"
      label="DocumentForm.creator"
      helpMessage="DocumentForm.creator-help"
    ></opensilex-UserSelector>

    <!-- language -->
    <opensilex-InputForm
      :value.sync="form.description.language"
      label="DocumentForm.language"
      type="text"
      helpMessage="DocumentForm.language-help"
    ></opensilex-InputForm>

    <!-- date -->
    <opensilex-InputForm
      :value.sync="form.description.date"
      label="DocumentForm.date"
      type="date"
      helpMessage="DocumentForm.date-help"
      :required="true"
    ></opensilex-InputForm>

    <!-- format -->
    <!-- <opensilex-InputForm
      :value.sync="form.description.format"
      label="DocumentForm.format"
      type="text"
      helpMessage="DocumentForm.format-help"
    ></opensilex-InputForm> -->

    <!-- comment -->
    <opensilex-TextAreaForm
      :value.sync="form.description.comment"
      label="DocumentForm.comment"
      type="text"
      helpMessage="DocumentForm.comment-help"
    ></opensilex-TextAreaForm>

    <!-- subject -->
    <opensilex-TagInputForm
      :value.sync="form.description.subject"
      label="DocumentForm.subject"
      type="text"
      helpMessage="DocumentForm.subject-help"
    ></opensilex-TagInputForm>

    <!-- deprecated -->
    <opensilex-CheckboxForm
      v-if="editMode"
      :value.sync="form.description.deprecated"
      label="DocumentForm.deprecated"
      title="DocumentForm.deprecated-title"
      helpMessage="DocumentForm.deprecated-help"
    ></opensilex-CheckboxForm>

    <!-- File -->
    <opensilex-FileInputForm
      v-if="!editMode"
      :file.sync="form.file"
      label="DocumentForm.file"
      type="file"
      helpMessage="DocumentForm.file-help"
    ></opensilex-FileInputForm>

  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { DocumentsService,DocumentGetDTO } from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DocumentForm extends Vue {
  $opensilex: any;
  service: DocumentsService;
  $store: any;
  $t: any;

  uriGenerated = true;

  get user() {
    return this.$store.state.user;
  } 

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        description: {
          uri: undefined,
          type: undefined,
          concerns: undefined,
          creator: undefined,
          language: undefined,
          name: undefined,
          date: undefined,
          format: undefined,
          comment: undefined,
          subject: undefined,
          deprecated: undefined
        },
        file: undefined
      };
    }
  })
  form: any;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      description: {
          uri: undefined,
          type: undefined,
          concerns: undefined,
          creator: undefined,
          language: undefined,
          name: undefined,
          date: undefined,
          format: undefined,
          comment: undefined,
          subject: undefined,
          deprecated: undefined
        },
        file: undefined
      };
  }

  create(form) {
    return this.$opensilex
     .uploadFileToService("/core/document/create", this.form, null, false)
     .then((http: OpenSilexResponse<any>) => {
        let uri = http.result;
        console.debug("document created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Document already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("DoucmentForm.document-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
     .uploadFileToService("/core/document/update", this.form, null, true)
     .then((http: OpenSilexResponse<any>) => {
        let uri = http.result;
        console.debug("Document updated", uri);
        this.$emit("onUpdate", form);
      })
      .catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DocumentForm:
    uri: URI or URL
    uri-help: Unique document identifier autogenerated OR uncheck to insert a URL to an external file
    type: Type
    type-help: Document Type
    name: Title
    name-help: A name given to the resource
    creator: Creator
    creator-help: An entity primarily responsible for making the resource
    language : Language 
    language-help: A language of the resource
    date: Date
    date-help: Creation Date
    format: Format 
    format-help: The file format, physical medium, or dimensions of the resource.
    comment: Comment
    comment-help: Description associated to the document metadata
    subject: Subject
    subject-help: A topic of the resource
    concerns: Concerns
    concerns-help: Resource's URI concerned by the document
    deprecated: Deprecated
    deprecated-help: Deprecated File
    deprecated-title: Select this option to make deprecated document
    file : File
    file-help: File to upload


fr:
  DocumentForm:
    uri: URI ou URL
    uri-help: Identifiant unique du document généré automatiquement OU décochez la case pour lier une URL de fichier externe
    type: Type
    type-help: Type de Document 
    name: Titre
    name-help: Titre de la ressource
    creator: Createur 
    creator-help: Une entité à l'origine de la creation la ressource
    language : Langue 
    language-help: Langue de la ressource
    date: Date
    date-help: Date de création
    format: Format 
    format-help: Le format de fichier, le support physique ou les dimensions de la ressource
    comment: Commentaire
    comment-help: Description associée aux métadonnées du document
    subject: Sujet
    subject-help: Le(s) sujet(s) de la ressource
    concerns: Concerne
    concerns-help: URI de la ressource concernée par le document
    deprecated: Obsolète
    deprecated-help: Fichier obsolète
    deprecated-title: Sélectionnez cette option pour rendre le document obsolète
    file : Fichier
    file-help: Fichier à insérer
</i18n>
