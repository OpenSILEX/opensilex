<template>
  <b-form v-if="form.description">
    <!-- URI or URL-->
    <opensilex-UriForm
      :uri.sync="form.description.uri" 
      label="DocumentForm.uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
      helpMessage="DocumentForm.uri-help"
    ></opensilex-UriForm>

    <!-- identifier -->
    <opensilex-InputForm
      :value.sync="form.description.identifier"
      label="DocumentForm.identifier"
      type="text"
      helpMessage="DocumentForm.identifier-help"
      placeholder= "DocumentForm.placeholder-identifier"
    ></opensilex-InputForm>

    <!-- type -->
    <opensilex-TypeForm
      :type.sync="form.description.rdf_type"
      :baseType="$opensilex.Oeso.DOCUMENT_TYPE_URI"
      helpMessage="DocumentForm.type-help"    
      :required="true"
    ></opensilex-TypeForm>

    <!-- title -->
    <opensilex-InputForm
      :value.sync="form.description.title"
      label="DocumentForm.title"
      type="text"
      helpMessage="DocumentForm.title-help"
      :required="true"
    ></opensilex-InputForm>

    <!-- date -->
    <opensilex-DateForm
        :value.sync="form.description.date"
        helpMessage="DocumentForm.date-help"
        label="DocumentForm.date"
    ></opensilex-DateForm>

    <!-- description -->
    <opensilex-TextAreaForm
      :value.sync="form.description.description"
      label="DocumentForm.description"
      type="text"
      helpMessage="DocumentForm.description-help"
      @keydown.native.enter.stop
    ></opensilex-TextAreaForm>

    <!-- targets -->
    <!-- {regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$|(.)+:(.)+/} -->
    <ValidationProvider
      name="Target"     
      rules="url"         
      :skipIfEmpty="true"
      v-slot="{ errors }"
    >
      <div class="error-message alert alert-danger">{{ errors[0] }}</div>
      <opensilex-TagInputForm
        class="overflow-auto"
        style="height: 90px"
        :value.sync="form.description.targets"
        :baseType="$opensilex.Oeso.targets"
        label="DocumentForm.targets"
        helpMessage="DocumentForm.targets-help"
        type="text"
      ></opensilex-TagInputForm>
    </ValidationProvider>

    <!--hasAuthors -->
    <opensilex-TagInputForm
      :value.sync="form.description.authors"
      :baseType="$opensilex.Oeso.hasAuthors"
      placeholder= "DocumentForm.placeholder-authors"
      label="DocumentForm.authors"
      helpMessage="DocumentForm.authors-help"
      type="text"
    ></opensilex-TagInputForm>

    <!-- language -->
    <opensilex-InputForm
      :value.sync="form.description.language"
      label="DocumentForm.language"
      type="text"
      helpMessage="DocumentForm.language-help"
      placeholder= "DocumentForm.placeholder-language"
    ></opensilex-InputForm>

    <!-- keywords -->
    <opensilex-TagInputForm
      :value.sync="form.description.keywords"
      label="DocumentForm.keywords"
      type="text"
      helpMessage="DocumentForm.keywords-help"
    ></opensilex-TagInputForm>

    <!-- deprecated -->
    <opensilex-CheckboxForm
      v-if="editMode"
      :value.sync="form.description.deprecated"
      label="DocumentForm.deprecated"
      title="DocumentForm.deprecated-title"
      helpMessage="DocumentForm.deprecated-help"
    ></opensilex-CheckboxForm>

    <!-- switch -->
    <b-form-radio-group
        v-model="documentContentType"
        v-if="!editMode"
    >
      <b-form-radio :value="DOCUMENT_CONTENT_TYPE_FILE">{{ $t("DocumentForm.upload-file") }}</b-form-radio>
      <b-form-radio :value="DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE">{{ $t("DocumentForm.link-external-source") }}</b-form-radio>
    </b-form-radio-group>
    <!-- File -->
    <opensilex-FileInputForm
      v-if="!editMode && documentContentType === DOCUMENT_CONTENT_TYPE_FILE"
      :file.sync="form.file"
      label="DocumentForm.file"
      type="file"
      helpMessage="DocumentForm.file-help"
      browse-text="DocumentForm.browse"
      :required="true"
      rules="size:100000"
    ></opensilex-FileInputForm>

    <!-- Source -->
    <opensilex-InputForm
      v-if="!editMode && documentContentType === DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE"
      label="DocumentForm.external-source"
      type="text"
      :value.sync="form.description.source"
      :required="true"
      rules="url"
    >
    </opensilex-InputForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { DocumentsService } from "opensilex-core/index"; 
import { OpenSilexResponse } from "../../lib/HttpResponse";

type DocumentContentTypeFile = 'file';
type DocumentContentTypeExternalSource = 'external-source';
type DocumentContentType = DocumentContentTypeFile | DocumentContentTypeExternalSource;

@Component
export default class DocumentForm extends Vue {
  $opensilex: any;
  service: DocumentsService;
  $store: any;
  $t: any;
  file;
  uriGenerated = true;

  DOCUMENT_CONTENT_TYPE_FILE: DocumentContentTypeFile = 'file';
  DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE: DocumentContentTypeExternalSource = 'external-source';
  documentContentType: DocumentContentType = this.DOCUMENT_CONTENT_TYPE_FILE;

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
          identifier: undefined,
          rdf_type: undefined,
          title: undefined,
          date: undefined,
          description: undefined,
          targets: undefined,
          authors: undefined,
          language: undefined,
          deprecated: undefined,
          keywords: undefined,
          source: undefined
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
          identifier: undefined,
          rdf_type: undefined,
          title: undefined,
          date: undefined,
          description: undefined,
          targets: undefined,
          authors: undefined,
          language: undefined,
          deprecated: undefined,
          keywords: undefined
        },
        file: undefined
      };
  }

  create(form) {
    if (this.documentContentType === this.DOCUMENT_CONTENT_TYPE_FILE) {
      delete this.form.description.source;
    } else {
      delete this.form.file;
    }
    return this.$opensilex
     .uploadFileToService("/core/documents", this.form, null, false)
     .then((http: OpenSilexResponse<any>) => {
        let uri = http.result;
        console.debug("document created", uri);
        form.uri = uri;
        return form;
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Document already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("DocumentForm.error.document-already-exists")
          );
        } else if (error.status == 500) {
          console.error("File name is too long", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("DocumentForm.error.file-name-too-long")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
     .uploadFileToService("/core/documents", this.form, null, true)
     .then((http: OpenSilexResponse<any>) => {
        let uri = http.result;
        console.debug("Document updated", uri);
        return form;
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
    title: Title
    title-help: A title given to the resource
    authors: Authors
    placeholder-authors : Last name, First name
    authors-help: An entity primarily responsible for making the resource. Recommended practice is to identify the creator with a URI. If this is not possible or feasible, a literal value that identifies the creator may be provided.
    language : Language 
    language-help: A language of the resource
    placeholder-language: en
    date: Date
    date-help: Creation Date
    format: Format 
    format-help: The file format, physical medium, or dimensions of the resource.
    description: Description
    description-help: Description associated to the document metadata
    keywords: Keywords
    keywords-help: A topic of the resource. Typically, the subject will be represented using keywords, key phrases, or classification codes. Recommended best practice is to use a controlled vocabulary.
    targets: Target
    targets-help: List of resources's URI concerned by the document
    targets-error: Concerned item's URI expected
    deprecated: Deprecated
    deprecated-help: Deprecated File
    deprecated-title: Select this option to make deprecated document
    file: Document
    file-help: Document to upload limit to 100MB
    identifier: Identifier
    identifier-help: Recommended practice is to identify the resource by means of a string conforming to an identification system. Examples include International Standard Book Number (ISBN), Digital Object Identifier (DOI), and Uniform Resource Name (URN). Persistent identifiers should be provided as HTTP URIs.
    placeholder-identifier: doi:10.1340/309registries
    browse: Browse
    upload-file: Upload a file
    link-external-source: Link an external source
    external-source: External source
    error:
      document-already-exists: Document already exists
      file-name-too-long: File name is too long

fr:
  DocumentForm:
    uri: URI ou URL
    uri-help: Identifiant unique du document généré automatiquement OU décochez la case pour lier une URL de fichier externe
    type: Type
    type-help: Type de Document 
    title: Titre
    title-help: Titre de la ressource
    authors: Auteurs 
    placeholder-authors : Nom, Prénom
    authors-help: Une entité à l'origine de la creation la ressource. La pratique recommandée consiste à identifier le créateur avec un URI. Si cela n'est pas possible ou faisable, une valeur littérale identifiant le créateur peut être fournie.
    language : Langue 
    language-help: Langue de la ressource
    placeholder-language: fr
    date: Date
    date-help: Date de création
    format: Format 
    format-help: Le format de fichier, le support physique ou les dimensions de la ressource
    description: Description
    description-help: Description associée aux métadonnées du document
    keywords: Mots-clés
    keywords-help: Le(s) sujet(s) de la ressource. En règle générale, le sujet sera représenté à l'aide de mots-clés, d'expressions clés ou de codes de classification. La meilleure pratique recommandée consiste à utiliser un vocabulaire contrôlé.
    targets: Cible
    targets-help: Liste d'URI des ressources concernées par le document
    targets-error: URI de l'élément concerné attendu
    deprecated: Obsolète
    deprecated-help: Fichier obsolète
    deprecated-title: Sélectionnez cette option pour rendre le document obsolète
    file: Document
    file-help: Document à insérer limité à 100MB
    identifier: Identifiant
    identifier-help: La pratique recommandée est d'identifier la ressource au moyen d'une chaîne conforme à un système d'identification. Les exemples incluent le numéro international normalisé du livre (ISBN), l'identificateur d'objet numérique (DOI) et le nom uniforme de ressource (URN). Les identificateurs persistants doivent être fournis sous forme d'URI HTTP.
    placeholder-identifier: doi:10.1340/309registries
    browse: Parcourir
    upload-file: Importer un fichier
    link-external-source: Lier une source externe
    external-source: Source externe
    error:
      document-already-exists: Le document existe déjà
      file-name-too-long: Le nom du fichier est trop long

</i18n>
