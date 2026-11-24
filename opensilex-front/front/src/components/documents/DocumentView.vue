<template>
  <div class="container-fluid">
    <PageContent>
      <div class="pageActionsBtns">
        <CreateButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
            @click="showCreateForm"
            :label="t('component.common.addDocument')"
            class="createButton"
        />
      </div>
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
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import {computed, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";

const { t } = useI18n()
const store = useStore()
const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)
const documentListRef = useTemplateRef<InstanceType<typeof DocumentList>>("documentListRef")

function showCreateForm() {
  documentListRef.value?.showCreateForm()
}

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
