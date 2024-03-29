<template>
  <div>
      <div class="pageActionsBtns">
        <opensilex-CreateButton
          v-if="user.hasCredential(modificationCredentialId)"
          @click="createDocument()"
          label="DocumentTabList.add"
          class="createButton"
        ></opensilex-CreateButton>
      </div>

      <opensilex-StringFilter
          v-if="search"
          :filter.sync="filter.title"
          @update="updateFilters()"
          :debounce="300"
          :lazy="false"
          placeholder="DocumentList.filter.title-placeholder"
      ></opensilex-StringFilter>

        <div class="card">
    <div class="card-body">
      <opensilex-PageContent
          v-if="renderComponent">
          <template v-slot>
            <opensilex-TableAsyncView
                  ref="tableRef"
                  :searchMethod="searchDocuments"
                  :fields="fields"
                  defaultSortBy="name"
                >
                  <template v-slot:cell(uri)="{data}">
                    <opensilex-UriLink :uri="data.item.uri"
                    :value="data.item.title"
                    :to="{path: '/document/details/'+ encodeURIComponent(data.item.uri)}"
                    ></opensilex-UriLink>
                  </template>

                  <template v-slot:row-details>
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
                        v-if="user.hasCredential(modificationCredentialId)"
                        @click="editDocument(data.item.uri)"
                        label="DocumentTabList.update"
                        :small="true"
                      ></opensilex-EditButton>
                      <opensilex-DeprecatedButton
                        v-if="user.hasCredential(modificationCredentialId)"
                        :deprecated="data.item.deprecated"
                        @click="deprecatedDocument(data.item.uri)"
                        :small="true"
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
          </template>
      </opensilex-PageContent>
    </div>
        </div>

      <opensilex-ModalForm
        v-if="user.hasCredential(modificationCredentialId)"
        ref="documentForm"
        component="opensilex-DocumentForm"
        createTitle="DocumentTabList.add"
        editTitle="DocumentTabList.update"
        modalSize="lg"
        :initForm="initForm"
        icon="ik#ik-file-text"
        @onCreate="refresh()"
        @onUpdate="refresh()"
      ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop, Watch } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { DocumentGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class DocumentTabList extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("documentForm") readonly documentForm!: any;
  renderComponent = true;

  @Prop()
  uri;

  @Prop()
  modificationCredentialId;
  
  @Prop({
    default: true
  })
  search: boolean;

  @Watch("uri")
  onTargetChange() {
      this.renderComponent = false;

      this.$nextTick(() => {
      // Add the component back in
      this.renderComponent = true;
      });
  }

  get user() {
    return this.$store.state.user;
  }

  fields = [
    {
      key: "uri",
      label: "DocumentTabList.title",
      sortable: true
    },
    {
      key: "authors",
      label: "DocumentTabList.author",
      sortable: true
    },
    {
      key: "date",
      label: "DocumentTabList.date",
      sortable: true
    },
    {
      key: "rdf_type_name",
      label: "DocumentTabList.type",
      sortable: true
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];
  
  filter = {
    title: undefined,
    deprecated: "false",
    date: undefined,
    rdf_type: undefined,
    authors: undefined,
    keywords: undefined,
    targets: undefined,
    multiple: undefined
  };

  resetFilters() {
    this.filter = {
      title: undefined,
      deprecated: "false",
      date: undefined,
      rdf_type: undefined,
      authors: undefined,
      keywords: undefined,
      targets: undefined,
      multiple: undefined
    };
    this.refresh();
  }

  searchDocuments(options) {  
    return this.$opensilex
      .getService("opensilex.DocumentsService")
      .searchDocuments(
      undefined, // type filter
      this.filter.title, //title filter
      undefined, // date filter
      this.uri, // targets filter
      undefined, // authors filter
      undefined, //keywords filter
      undefined, // multiple filter
      false, // deprecated filter
      options.orderBy,
      options.currentPage,
      options.pageSize);
  }

  refresh() {
    if (this.tableRef) {
      this.tableRef.refresh();
    }
  }

  createDocument() {
    this.documentForm.showCreateForm();
  }

  initForm() {
    if(this.uri){
      return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: decodeURIComponent(this.uri),
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined
      },
      file: undefined
      }
    }
  }

  deprecatedDocument(uri: string) {
    this.$opensilex
      .getService("opensilex.DocumentsService") 
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
            deprecated: true,
            keywords: document.keywords
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

  editDocument(uri: string) {
    console.debug("editDocument" + uri);
    this.$opensilex
      .getService("opensilex.DocumentsService")  
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
            keywords: document.keywords
          }
        };
        this.documentForm.showEditForm(form);
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadFile(uri: string, title: string, format: string) {
    let path = "/core/documents/" + encodeURIComponent(uri);
    this.$opensilex
     .downloadFilefromService(path, title, format);
  }

  updateFilters() {
    this.$opensilex.updateURLParameter("name", this.filter.title, "");
    this.refresh();
  }

  resetSearch() {
    this.filter.title = "";
    this.$opensilex.updateURLParameter("name", undefined, undefined);
    this.refresh();
  }

  browseSource(source: string) {
    window.open(source);
  }

}
</script>

<style scoped lang="scss">

.pageActionsBtns {
    margin-left: 10px;
    margin-bottom: 10px
}
</style>

<i18n>
en:
    DocumentTabList:
        documents: Documents
        uri: URI
        title: Titre
        author: Author
        date: Date
        add: Add document
        type: type
        update: Update document
        delete: Delete Document
        deprecated: Deprecated

fr:
    DocumentTabList:
        documents: Documents
        uri: URI
        title: Titre
        author: Auteur
        date: Date
        add: Ajouter un document
        type: type
        update: Modifier le document
        delete: Supprimer le Document
        deprecated: Obsolète
</i18n>

