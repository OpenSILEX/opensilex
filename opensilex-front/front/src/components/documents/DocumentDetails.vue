<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-file-text"
      :title="document.title"
      description="DocumentDetails.title"
    ></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" >   
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
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
              @click="update"
              label="DocumentDetails.update"
            ></opensilex-EditButton>
            <!-- <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_DELETE_ID)"
              @click="deleteDocument(document.uri)"
              label="DocumentDetails.delete"
              :small="true"
            ></opensilex-DeleteButton> -->
          </template>
            <template v-slot:body>
              <opensilex-UriView :uri="document.uri"></opensilex-UriView>
              <opensilex-StringView label="DocumentDetails.identifier" :value="document.identifier"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.type" :value="document.rdf_type_name"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.docTitle" :value="document.title"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.date" :value="document.date"></opensilex-StringView>
              <opensilex-TextView label="DocumentDetails.description" :value="document.description"></opensilex-TextView>
              <opensilex-StringView class="overflow-auto" style="height: 100px" label="DocumentDetails.targets" :uri="document.targets">
                <span :key="target" v-for="(target) in targetsTypes"> 
                <opensilex-UriLink 
                  :uri="target.uri"  
                  :value="target.uri"     
                  v-if="target.rdf_types.includes($opensilex.Oeso.EXPERIMENT_TYPE_URI)"           
                  :to="{path: '/experiment/details/'+ encodeURIComponent(target.uri)}"
                ></opensilex-UriLink> 
                <opensilex-UriLink 
                  :uri="target.uri" 
                  :value="target.uri"   
                  v-else-if="target.rdf_types.includes($opensilex.Oeso.DEVICE_TYPE_URI)"       
                  :to="{path: '/device/details/'+ encodeURIComponent(target.uri)}"
                ></opensilex-UriLink> 
                <opensilex-UriLink 
                  :uri="target.uri"   
                  :value="target.uri" 
                  v-else-if="target.rdf_types.includes($opensilex.Oeso.PROJECT_TYPE_URI)"           
                  :to="{path: '/project/details/'+ encodeURIComponent(target.uri)}"
                ></opensilex-UriLink> 
                <opensilex-UriLink 
                  :uri="target.uri"  
                  :value="target.uri"  
                  v-else-if="target.rdf_types.includes($opensilex.Oeso.GERMPLASM_TYPE_URI)"           
                  :to="{path: '/germplasm/details/'+ encodeURIComponent(target.uri)}"
                ></opensilex-UriLink>
                <opensilex-UriLink 
                  :uri="target.uri"  
                  :value="target.uri"  
                  v-else-if="target.rdf_types.includes($opensilex.Oeso.VARIABLESGROUP_TYPE_URI)"           
                  :to="{path: '/variables_group/details/'+ encodeURIComponent(target.uri)}"
                ></opensilex-UriLink>
                <opensilex-UriLink 
                  :uri="target.uri"  
                  :value="target.uri"  
                  v-else  
                ></opensilex-UriLink> 
                </span>
              </opensilex-StringView> 
              <opensilex-StringView label="DocumentDetails.authors" :value="document.authors">
                <span v-if="document.authors"><span :key="authors" v-for="(authors) in document.authors">{{ authors }}<br></span></span>
              </opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.language" :value="document.language"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.format" :value="document.format"></opensilex-StringView>
              <opensilex-StringView label="DocumentDetails.keywords" :value="document.keywords">
                <span v-if="document.keywords"><span :key="keywords" v-for="(keywords) in document.keywords">{{ keywords }} - </span></span>
              </opensilex-StringView>
              <opensilex-UriView
                  v-if="document.source"
                  title="DocumentDetails.source"
                  :url="document.source"
                  :value="document.source"
              ></opensilex-UriView>
            </template>
          </opensilex-Card>
      </b-col>

      <b-col sm="5">
        <opensilex-Card
            label="DocumentDetails.file"
            icon="ik#ik-download"
            v-if="hasFile"
        >
            <template v-slot:body>
              <div class="button-zone">
              <b-button 
                @click="previewFile(document.uri, document.title, document.format)"
              >{{ $t("DocumentDetails.preview") }}</b-button> 
              <b-button 
                @click="loadFile(document.uri, document.title, document.format)"
              >{{ $t("DocumentDetails.download") }}</b-button> 
              </div>
              <div id="preview"></div>
            </template>
        </opensilex-Card>
        
      </b-col>
      </b-row>
    </opensilex-PageContent>
    <opensilex-ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      editTitle="DocumentList.update"
      icon="ik#ik-file-text"
      modalSize="lg"
      @onUpdate="refresh()"
      ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { DocumentGetDTO, DocumentsService } from "opensilex-core/index";
import Oeso from "../../ontologies/Oeso";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DocumentDetails extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $t: any; 
  $i18n: any;
  service: DocumentsService;
  uri: string = null;

  @Ref("documentForm") readonly documentForm!: any;
  @Ref("preview") readonly preview!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  refresh() {
    this.loadDocument(this.uri);
  }
  
  document: DocumentGetDTO = { 
          uri: null,
          identifier:null,
          rdf_type: null,
          title: null,
          date: null,
          description: null,
          targets: null,
          authors: null,
          language: null,
          format: null,
          deprecated: null,
          keywords: null,
          source: null
      };
  
  targetsTypes = [];

  get hasFile() {
    return !this.document.source;
  }

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
        if (this.document.targets.length>0) {
          this.loadTargetsTypes();
        }  
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadFile(uri: string, title: string, format: string) {
    let path = "/core/documents/" + encodeURIComponent(uri);
    this.$opensilex
     .downloadFilefromService(path, title, format);
  }

  previewFile(uri: string, title: string, format: string) {
    let path = "/core/documents/" + encodeURIComponent(uri);
    this.$opensilex
     .previewFilefromGetService(path, title, format);
  }

  update() {
    let document = {
      description: {
        uri: this.document.uri,
        identifier: this.document.identifier,
        rdf_type: this.document.rdf_type,
        title: this.document.title,
        date: this.document.date,
        description: this.document.description,
        targets: this.document.targets,
        authors: this.document.authors,
        language: this.document.language,

        format: this.document.format,
        deprecated: this.document.deprecated,
        keywords: this.document.keywords
      }
    }
    this.documentForm.showEditForm(document);
  }

  deleteDocument(uri: string) {
    this.service
      .deleteDocument(uri)
      .then(() => {
        this.$router.go(-1);
        this.$emit("onDelete", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
  
  loadTargetsTypes() {
    let ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    let types = new Array(Oeso.GERMPLASM_TYPE_URI, Oeso.DEVICE_TYPE_URI, Oeso.PROJECT_TYPE_URI, Oeso.EXPERIMENT_TYPE_URI, Oeso.VARIABLESGROUP_TYPE_URI);
    let body = {
      uris: this.document.targets
    }
    ontologyService.checkURIsTypes(types, body)
    .then((http: HttpResponse<OpenSilexResponse<any>>) => { 
      this.targetsTypes = http.response.result;          
    })
    .catch(this.$opensilex.errorHandler); 
  }   
}
</script>

<style scoped lang="scss">

</style>

<i18n>

en:
  DocumentDetails:
    title: Document
    description: Description
    uri: URI
    docTitle: Title
    type: Type
    date: Date
    authors: Authors
    targets: Target
    language: Language
    format: Format
    keywords: Keywords
    deprecated: Deprecated
    identifier: Identifier
    backToList: Go back to Document list
    file: File
    no-such-file: No file provided
    download: Download File
    preview: Preview File
    update: Update Document
    delete: Delete Document
    source: Source

fr:
  DocumentDetails:
    title: Document
    description: Description
    uri: URI
    docTitle: Titre
    type: Type
    date: Date
    authors: Auteurs
    targets: Cible
    language: Language
    format: Format
    keywords: Mots-clés
    deprecated: Obsolète
    identifier: Identifiant
    backToList: Retourner à la liste des documents
    file: Fichier 
    no-such-file: Aucun fichier associé
    download: Télécharger le fichier
    preview: Aperçu du fichier
    update: Modifier Document
    delete: Supprimer Document
    source: Source
</i18n>
