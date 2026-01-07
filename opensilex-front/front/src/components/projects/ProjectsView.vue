<template>
  <div class="container-fluid">
    <opensilex-PageActions v-if="
        user.hasCredential(
          credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)
      "
    >
      <opensilex-CreateButton
        @click="projectFormRef?.showCreateForm?.()"
        label="component.project.add"
        class="createButton"
      />
    </opensilex-PageActions>

    <opensilex-PageContent>
      <opensilex-ProjectList
        ref="projectListRef"
        @onEdit="(p) => projectFormRef?.showEditForm?.(p)"
      />
    </opensilex-PageContent>

    <!-- <opensilex-ProjectForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
      ref="projectFormRef"
      @onCreate="redirectToCreatedProject"
      @onUpdate="projectListRef?.updateSelectedProject?.()"
    /> -->
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

const store = useStore()
const router = useRouter()

// refs vers composants enfants (types "any" pour rester simple en migration)
const projectFormRef = ref<any>(null)
const projectListRef = ref<any>(null)

// user + credentials depuis store (comme avant)
const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

function redirectToCreatedProject(project: any) {
  const uri = project?.uri
  if (!uri) return
  router.push({
    path: '/project/details/' + encodeURIComponent(uri)
  })
}
</script>

<style scoped lang="scss">
.createButton {
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
}
</style>
