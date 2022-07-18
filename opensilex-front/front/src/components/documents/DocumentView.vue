<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-DocumentList
          ref="documentList"
          :redirectAfterCreation="true"
        ></opensilex-DocumentList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { DocumentsService } from "opensilex-core/index";

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
  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
      this.tableRef.refresh();
  }
  
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
