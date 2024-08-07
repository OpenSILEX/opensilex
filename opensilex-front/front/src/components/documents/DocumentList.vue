<template>
  <div>
    <opensilex-PageActions
      v-if="
      user.hasCredential(
        credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)
        ">
        <opensilex-CreateButton
        @click="documentForm.showCreateForm()"
        label="DocumentView.add"
        class="createButton"
        ></opensilex-CreateButton>
    </opensilex-PageActions>

    <opensilex-PageContent
      class="pagecontent"
    >
     <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
      v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
      :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>

    <!-- Filters -->
    <Transition>
      <div v-show="SearchFiltersToggle">
    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="resetFilters()"
      withButton="false"
      :showAdvancedSearch="true"
      class="searchFilterField"
    >
      <template v-slot:filters>
        <!-- title and keyword -->
      <br><br>
        <div>
          <opensilex-StringFilter
            :filter.sync="filter.multiple"
            placeholder="DocumentList.filter.searchAll-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-StringFilter><br>
        </div>
      </template>

      <template v-slot:advancedSearch>
        <!-- type -->
        <div>
          <opensilex-TypeForm
            :type.sync="filter.rdf_type"
            :baseType="$opensilex.Oeso.DOCUMENT_TYPE_URI"
            placeholder="DocumentList.filter.type-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-TypeForm>
        </div>

        <!-- title -->
        <div>
          <label>{{$t('DocumentList.filter.title')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.title"
            placeholder="DocumentList.filter.title-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-StringFilter><br>
        </div>

        <!-- date -->   
        <div>
          <label>{{$t('DocumentList.filter.date')}}</label>
            <opensilex-StringFilter
              :filter.sync="filter.date"
              placeholder="DocumentList.filter.date-placeholder"
              type="number"
              min= "1900"
              max= "2900"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            ></opensilex-StringFilter><br>
        </div>

        <!-- targets -->
        <div>
          <label>{{$t('DocumentList.filter.targets')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.targets"
            placeholder="DocumentList.filter.targets-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-StringFilter><br>
        </div>

        <!-- author -->
        <div>
          <label>{{$t('DocumentList.filter.author')}}</label>
            <opensilex-InputForm
              :value.sync="filter.authors"
              placeholder="DocumentList.filter.author-placeholder"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            ></opensilex-InputForm>
        </div>

        <!-- keywords -->
        <div>
          <label>{{$t('DocumentList.filter.keywords')}}</label>
          <opensilex-InputForm
            :value.sync="filter.keywords"
            placeholder="DocumentList.filter.keywords-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-InputForm>
        </div>  
        
        <!-- deprecated -->
        <div>
          <opensilex-CheckboxForm
            label="DocumentList.filter.deprecated"
            :value.sync="filter.deprecated"
            class="searchFilter"
          ></opensilex-CheckboxForm>
        </div>
      </template>
    </opensilex-SearchFilterField>
      </div>
    </Transition>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDocuments"
      :fields="fields"
      defaultSortBy="label"
    >
      <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink :uri="data.item.uri"
        :value="data.item.title"
        :to="{path: '/document/details/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>

     <template v-slot:cell(authors)="{data}">
       <span v-if="data.item.authors">
       <span :key="index" v-for="(author, index) in data.item.authors">
          <span :title="author">{{ author }}</span>
          <span v-if="index + 1 < data.item.authors.length"> - </span>
        </span>   
        </span>  
     </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
            @click="editDocument(data.item.uri)"
            label="DocumentList.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeprecatedButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
            @click="deprecatedDocument(data.item.uri)"
            :small="true"
            :deprecated="data.item.deprecated"
          ></opensilex-DeprecatedButton>
          <opensilex-Button
              v-if="!data.item.source"
           component="opensilex-DocumentDetails"
            @click="loadFile(data.item.uri, data.item.title, data.item.format)"
            label="DocumentList.download"
            :small="true"
            icon= "ik#ik-download"
            variant="outline-info"
          ></opensilex-Button>
          <opensilex-Button
            v-if="data.item.source"
            @click="browseSource(data.item.source)"
            label="DocumentList.browseSource"
            :small="true"
            icon="ik#ik-link"
            variant="outline-info"
          ></opensilex-Button>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
    <opensilex-ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      editTitle="DocumentList.update"
      createTitle="DocumentList.add"
      icon="ik#ik-file-text"
      modalSize="lg"
      @onCreate="refreshOrRedirectAfterCreation"
      @onUpdate="updateSelectedDocument()"
    >
    </opensilex-ModalForm>
        </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { DocumentsService, DocumentGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DocumentList extends Vue {
  $opensilex: any;
  service: DocumentsService;
  $store: any;
  $route: any;

  @Ref("documentForm") readonly documentForm!: any;
  @Ref("tableRef") readonly tableRef!: any;

  @Prop({
    default: false
  })
  redirectAfterCreation;

  refresh() {
    this.$opensilex.updateURLParameters(this.filter);
    this.tableRef.changeCurrentPage(1);
  }

  refreshOrRedirectAfterCreation(document) {
    if (document !== undefined && this.redirectAfterCreation) {
      this.$router.push({
        path: '/document/details/' + encodeURIComponent(document.uri)
      })
    } else {
      this.refresh();
    }
  }

  updateSelectedDocument(){
    this.$opensilex.updateURLParameters(this.filter);
    this.tableRef.refresh();
  }
  
  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  filter = {
    rdf_type: undefined,
    title: undefined,
    date: undefined,
    targets: undefined,
    authors: undefined,
    keywords: undefined,
    multiple: undefined,
    deprecated: false
  };

  resetFilters() {
    this.filter = {
      rdf_type: undefined,
      title: undefined,
      date: undefined,
      targets: undefined,
      authors: undefined,
      keywords: undefined,
      multiple: undefined,
      deprecated: false
    };
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DocumentsService");
    this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
  }

    data(){
    return {
      SearchFiltersToggle : false,
    }
  }

  fields = [
    {
      key: "uri",
      label: "DocumentList.title",
      sortable: true
    },
    {
      key: "rdf_type_name",
      label: "DocumentList.type",
      sortable: true
    },
    {
      key: "authors",
      label: "DocumentList.author",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  searchDocuments(options) {
    return this.service.searchDocuments(
      this.filter.rdf_type, // type filter
      this.filter.title, //title filter
      this.filter.date, // date filter
      this.filter.targets, // targets filter
      this.filter.authors, // user filter
      this.filter.keywords, // keywords filter
      this.filter.multiple, // multiple filter
      this.filter.deprecated.toString(), // deprecated filter
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  deleteDocument(uri: string) {
    this.service
      .deleteDocument(uri)
      .then(() => {
        this.refresh();
        this.$emit("onDelete", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  editDocument(uri: string) {
    console.debug("editDocument" + uri);
    this.service
      .getDocumentMetadata(uri)
      .then((http: HttpResponse<OpenSilexResponse<DocumentGetDTO>>) => {
        let document = http.response.result;
        let form = {
          description: {
            uri: document.uri,
            identifier: document.identifier,
            rdf_type: document.rdf_type,
            title: document.title,
            date: document.date,
            description: document.description,
            targets: document.targets,
            authors: document.authors,
            language: document.language,
            format: document.format,
            deprecated: document.deprecated,
            keywords: document.keywords,
            source: document.source
          }
        };
        this.documentForm.showEditForm(form);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deprecatedDocument(uri: string){
   this.service
      .getDocumentMetadata(uri)
      .then((http: HttpResponse<OpenSilexResponse<DocumentGetDTO>>) => {
        let document = http.response.result;
        let form = {
          description: {
            uri: document.uri,
            identifier: document.identifier,
            rdf_type: document.rdf_type,
            title: document.title,
            date: document.date,
            description: document.description,
            targets: document.targets,
            authors: document.authors,
            language: document.language,
            format: document.format,
            deprecated: !document.deprecated,
            keywords: document.keywords,
            source: document.source
          }
        };
      this.updateForDeprecated(form);
      })
      .catch(this.$opensilex.errorHandler);
  }

  updateForDeprecated(form) {
    return this.$opensilex
     .uploadFileToService("/core/documents", form, null, true)
     .then((http: OpenSilexResponse<any>) => {
        let uri = http.result;
        this.$emit("onUpdate", form);
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadFile(uri: string, title: string, format: string) {
    let path = "/core/documents/" + encodeURIComponent(uri);
    this.$opensilex
     .downloadFilefromService(path, title, format);
  }

  browseSource(source: string) {
    window.open(source);
  }
  
  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }

}
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
}
.searchFilterField * {
  margin-left: 6px;
}
</style>

<i18n>

en:
  DocumentList:
    uri: URI
    title: Title
    type: Document Type 
    date: Creation Date 
    keywords: Keywords
    add: Add document
    update: Update Document
    targets: Concerns
    delete: Delete Document
    author: Author
    language: Language
    deprecated: Deprecated
    cancelDeprecated: Cancel Deprecated
    identifier: Identifier
    download: Download file
    browseSource: Browse source

    filter:
      title: Title
      title-placeholder: Enter title
      search: Search
      reset: Reset
      date: Year
      date-placeholder: Enter year
      deprecated: Deprecated
      keywords: Keywords
      keywords-placeholder: Enter Keywords
      type: Type
      type-placeholder: Select a document type
      author: Author
      author-placeholder: Enter Author's title
      targets: Target
      targets-placeholder: Enter target's URI
      searchAll-placeholder: Search by title and keyword 

fr:
  DocumentList:
    uri: URI
    title: Titre
    type: Type du document
    date: Date de création
    keywords: Mots-clés
    add: Ajouter un document
    update: Editer le document
    targets: Cible
    delete: Supprimer le document
    author: Auteur
    language: Langue
    deprecated: Obsolète
    cancelDeprecated: Annuler Obsolète
    identifier: Identifiant
    download: Télécharger le fichier
    browseSource: Naviguer à la source

    filter:
      title: Titre
      title-placeholder: Saisir un titre
      search: Rechercher
      reset: Réinitialiser
      date: Année
      date-placeholder: Saisir une année
      deprecated: Obsolète
      keywords: Mots-clés
      keywords-placeholder: Saisir un mot-clé
      type: Type
      type-placeholder: Sélectionner un type de document
      author: Auteur
      author-placeholder: Saisir le nom d'un auteur
      targets: Concernes
      targets-placeholder: Entrez l'URI concerné
      searchAll-placeholder: Recherche par titre et mot-clé
      
</i18n>
