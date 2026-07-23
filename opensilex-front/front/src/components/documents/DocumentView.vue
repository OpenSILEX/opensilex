<template>
  <div class="container-fluid">
    <opensilex-PageContent>
        <opensilex-DocumentList
          ref="documentListRef"
          :redirectAfterCreation="true"
        ></opensilex-DocumentList>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, useTemplateRef } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import DocumentList from "@/components/document/DocumentList.vue";

const store = useStore()
const router = useRouter()

// refs 
const documentListRef = useTemplateRef<InstanceType<typeof DocumentList>>("documentListRef")

// user + credentials  
const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

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
