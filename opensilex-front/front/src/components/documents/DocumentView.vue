<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-file-text"
      title="DocumentView.title"
      description="DocumentView.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions
      v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
    >
      <template v-slot>
        <opensilex-CreateButton @click="documentForm.showCreateForm()" label="DocumentView.add"></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-DocumentList
          v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_READ_ID)"
          ref="documentList"
          v-bind:credentialsGroups="credentialsGroups"
        ></opensilex-DocumentList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="DocumentView.add"
      editTitle="DocumentView.update"
      modalSize="lg"
      icon="ik#ik-settings"
      @onCreate="documentList.refresh()"
      @onUpdate="documentList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { DocumentsService, DocumentGetDTO} from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse
} from "../../lib/HttpResponse";

@Component
export default class DocumentView extends Vue {
  $opensilex: any;
  service: DocumentsService;
  $store: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("documentForm") readonly documentForm!: any;
  @Ref("documentList") readonly documentList!: any;

   created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.DocumentsService");
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DocumentView:
    title: Document
    description: Manage Document
    add: Add document
    update: Update Document
    delete: Delete DOcument
fr:
  DocumentView:
    title: Document
    description: Gestion des documents
    add: Ajouter un document
    update: Editer un document
    delete: Supprimer un document
</i18n>
