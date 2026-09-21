<template>
  <div class="container-fluid">
    <PageContent>
        <DocumentList
          ref="documentListRef"
          :redirectAfterCreation="true"
        ></DocumentList>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import PageContent from "@/components/layout/PageContent.vue";
import DocumentList from "@/components/documents/DocumentList.vue";

</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DocumentView:
    add: Add document
    update: Update Document
    delete: Delete DOcument
fr:
  DocumentView:
    add: Ajouter un document
    update: Editer un document
    delete: Supprimer un document
</i18n>
