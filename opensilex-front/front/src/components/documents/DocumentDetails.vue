<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-file-text"
      title="DocumentDetails.title"
      :description="document.name"
    ></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" >   
        <b-button
          v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"           
          @click="update"
          label="update"
          variant="primary"
        >update</b-button>      
    </opensilex-PageActions>

    <opensilex-PageContent>
      <b-row>
        <b-col sm="5">
          <opensilex-Card label="DocumentDetails.description" icon="ik#ik-clipboard">
          <template v-slot:rightHeader>
            <span
              v-if="document.deprecated"
              class="badge badge-pill badge-warning"
              :title="$t('DocumentDetails.deprecated')"
            >
              <i class="ik ik-alert-triangle mr-1"></i>
              {{ $t("DocumentDetails.deprecated") }}
            </span>
          </template>
            <template v-slot:body>
              <opensilex-StringView label="DocumentDetails.uri" :value="document.uri"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.rdfType" :value="document.type"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.concerns" :value="document.concerns"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.language" :value="document.language"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.name" :value="document.name"></opensilex-StringView>
              <opensilex-TextView label="DocumentDetails.comment" :value="document.comment"></opensilex-TextView>
              <opensilex-StringView label="DocumentDetails.creator" :value="document.creator"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.date" :value="document.date"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.format" :value="document.format"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.subject" :value="document.subject">
                <span :key="subject" v-for="(subject) in document.subject">{{ subject }} </span>
              </opensilex-StringView>
            </template>
          </opensilex-Card>
      </b-col>

      <b-col sm="5">
        <opensilex-Card label="DocumentDetails.file" icon="ik#ik-upload">
            <template v-slot:body>
              <b-button 
                v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_READ_ID)"           
                @click="loadFile(document.uri, document.name, document.format)"
                label="DocumentDetails.upload"
              >File to upload</b-button> 
            </template>
        </opensilex-Card>
      </b-col>
      </b-row>
    </opensilex-PageContent>
    <opensilex-ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      editTitle="udpate"
      icon="ik#ik-user"
      modalSize="lg"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  DocumentGetDTO,
  DocumentsService
} from "opensilex-core/index";
import {
  SecurityService,
  UserGetDTO
} from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { faFileExport, faFilePdf } from "@fortawesome/free-solid-svg-icons";

@Component
export default class DocumentDetails extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $t: any;
  $i18n: any;
  service: DocumentsService;
  uri: string = null;

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  
  refresh() {
    this.modalRef.refresh();
  }

  document: DocumentGetDTO = { 
          uri: null,
          type: null,
          concerns: null,
          creator: null,
          language: null,
          name: null,
          date: null,
          format: null,
          comment: null,
          subject: null,
          deprecated: null,
      };
  
  created() {
    this.service = this.$opensilex.getService("opensilex.DocumentsService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadDocument(this.uri);
  }

  loadDocument(uri: string) {
    this.service
      .getDocumentMetadata(uri)
      .then((http: HttpResponse<OpenSilexResponse<DocumentGetDTO>>) => {
        this.document = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadFile(uri: string, name: string, format: string) {
    let path = "/core/document/getFile/" + encodeURIComponent(uri);
    this.$opensilex
     .downloadFilefromService(path, name, format, null);
  }

  @Ref("documentForm") readonly documentForm!: any;

  update() {
    let document = {
          description: {
            uri: this.document.uri,
            type: this.document.type,
            concerns: this.document.concerns,
            creator: this.document.creator,
            language: this.document.language,
            name: this.document.name,
            date: this.document.date,
            format: this.document.format,
            comment: this.document.comment,
            subject: this.document.subject,
            deprecated: this.document.deprecated

      }
    }
    this.documentForm.showEditForm(document);
  }
  
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DocumentDetails:
    title: Document
    description: Detailed Information
    uri: URI
    name: Title
    rdfType: Type
    date: Date
    comment: Comment
    creator: Creator
    concerns: Concerns
    language: Language
    format: Format
    subject: Subject
    deprecated: Deprecated File
    backToList: Go back to Document list
    file: File
    no-such-file: No file provided
    upload: File to upload

fr:
  DocumentDetails:
    title: Document
    description: Information détaillées
    uri: URI
    name: Titre
    rdfType: Type
    date: Date
    comment: Commentaire
    creator: Auteur
    concerns: Concerne
    language: Language
    format: Format
    subject: Sujet
    deprecated: Fichier Obsolète
    backToList: Retourner à la liste des documents
    file: Fichier 
    no-such-file: Aucun fichier associé
    upload: Télécharger le fichier

</i18n>
