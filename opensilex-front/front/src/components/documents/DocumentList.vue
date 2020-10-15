<template>
  <div>
    <opensilex-SearchFilterField
      @search="updateFilter()"
      @clear="resetFilters()"
      withButton="false"
    >
      <template v-slot:filters>
        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DocumentList.filter.name')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.name"
            placeholder="DocumentList.filter.name-placeholder"
          ></opensilex-StringFilter>
        </div>
        <!-- <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DocumentList.filter.subject')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.subject"
            placeholder="DocumentList.filter.subject-placeholder"
          ></opensilex-StringFilter>
        </div> -->
         <div class="col col-xl-3 col-sm-6 col-12">
          <opensilex-TypeForm
            :type.sync="filter.rdfType"
            :baseType="$opensilex.Oeso.DOCUMENT_TYPE_URI"
            placeholder="DocumentList.filter.rdfType-placeholder"
          ></opensilex-TypeForm>
        </div>
        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DocumentList.filter.user')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.user"
            placeholder="DocumentList.filter.user-placeholder"
          ></opensilex-StringFilter>
        </div>
        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('DocumentList.filter.date')}}</label>
            <opensilex-StringFilter
              :filter.sync="filter.date"
              placeholder="DocumentList.filter.date-placeholder"
            ></opensilex-StringFilter>
        </div>
        <div class="col col-xl-1 col-sm-2 col-12">
          <opensilex-CheckboxForm
            label="DocumentList.filter.deprecated"
            :value.sync="filter.deprecated"
          ></opensilex-CheckboxForm>
        </div>
      </template>
    </opensilex-SearchFilterField>
<opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDocuments"
      :fields="fields"
      defaultSortBy="label"
    >
      <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink :uri="data.item.uri"
        :value="data.item.name"
        :to="{path: '/document/details/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:row-details>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
            @click="editDocument(data.item.uri)"
            label="DocumentList.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_DELETE_ID)"
            @click="deleteDocument(data.item.uri)"
            label="DocumentList.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
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
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
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

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  filter = {
    name: undefined,
    deprecated: "false",
    subject: undefined,
    date: undefined,
    rdfType: undefined,
    user: undefined
  };

  resetFilters() {
    this.filter = {
      name: undefined,
      deprecated: "false",
      subject: undefined,
      date: undefined,
      rdfType: undefined,
      user: undefined
    };
    this.refresh();
  }

  created() {
    let query: any = this.$route.query;
    this.service = this.$opensilex.getService("opensilex.DocumentsService");
  }

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

    fields = [
    {
      key: "uri",
      label: "DocumentList.name",
      sortable: true
    },
    {
      key: "creator",
      label: "DocumentList.creator",
      sortable: true
    },
    {
      key: "date",
      label: "DocumentList.date",
      sortable: true
    },
    {
      key: "language",
      label: "DocumentList.language",
      sortable: true
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  refresh() {
    this.tableRef.refresh();
  }

  searchDocuments(options) {
    return this.service.searchDocuments(
      this.filter.name, //name filter
      this.filter.deprecated, // deprecated filter
      this.filter.subject, // subject filter
      this.filter.date, // date filter
      this.filter.rdfType, // type filter
      this.filter.user, // user filter
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
            type: document.type,
            concerns: document.concerns,
            creator: document.creator,
            language: document.language,
            name: document.name,
            date: document.date,
            format: document.format,
            comment: document.comment,
            subject: document.subject,
            deprecated: document.deprecated
          }
        };
        this.documentForm.showEditForm(form);
      })
      .catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DocumentList:
    uri: URI
    name: Name
    rdfType: Document Type 
    date: Creation Date 
    subject: Subject
    update: Update Document
    concerns: Concerned Items
    delete: Delete Document
    creator: Creator
    language: Language

    filter:
      name: Name
      name-placeholder: Enter name
      search: Search
      reset: Reset
      date: Date
      date-placeholder: Enter date
      deprecated: Deprecated
      subject: Subject
      subject-placeholder: Enter subject
      rdfType: Type
      rdfType-placeholder: Select a document type
      user: Creator
      user-placeholder: Enter creator's URI

fr:
  DocumentList:
    uri: URI
    name: Nom
    rdfType: Type du document
    date: Date de création
    subject: Sujet
    update: Editer le document
    concerns: Items concernés
    delete: Supprimer le document
    creator: Auteur
    language: Langue

    filter:
      name: Recherche de documents
      name-placeholder: Saisir un nom
      search: Rechercher
      reset: Réinitialiser
      date: Date
      date-placeholder: Saisir une date
      deprecated: Obsolète
      subject: Sujet
      subject-placeholder: Saisir un sujet
      rdfType: Type
      rdfType-placeholder: Selectionner un type de document
      user: Auteur
      user-placeholder: Saisir l'uri d'un auteur
</i18n>
