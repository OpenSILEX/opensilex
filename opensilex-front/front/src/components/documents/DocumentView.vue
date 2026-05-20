<template>
  <div class="container-fluid py-3">



    <opensilex-PageContent>
      <template v-slot>
        <opensilex-DocumentList
          ref="documentListRef"
          :redirectAfterCreation="true"
        ></opensilex-DocumentList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

const store = useStore()
const router = useRouter()

// refs 
const documentListRef = ref<any>(null)

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
