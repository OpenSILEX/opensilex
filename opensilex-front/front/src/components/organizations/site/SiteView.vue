<template>
  <div class="container-fluid">
    <opensilex-CreateButton
      id="createSiteButton"
      @click="onCreateClick"
      :label="t('SiteView.create')"
      class="createButton"
    />

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-SiteList
          ref="siteListRef"
          @onEdit="onSiteListEdit"
          :organizationsForFilter="organizationsForFilter"
        />
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
      ref="siteFormRef"
      lazy="true"
      component="opensilex-SiteForm"
      :createTitle="t('SiteView.create')"
      editTitle="component.site.update"
      icon="ik#ik-map-pin"
      @onCreate="siteListRef?.refresh?.()"
      @onUpdate="siteListRef?.refresh?.()"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from "vue-i18n";

// Props
const props = defineProps<{
  organizationsForFilter?: string[] | null
}>()

const store = useStore()
const { t } = useI18n();

// Refs composants enfants
const siteFormRef = ref<any>(null)
const siteListRef = ref<any>(null)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

// Handlers
function onSiteListEdit(dto: any) {
  siteFormRef.value?.showEditForm?.(dto)
}

function onCreateClick() {
  siteFormRef.value?.showCreateForm?.()
}
</script>

<style scoped lang="scss">
#createSiteButton {
  margin-bottom: 10px;
  margin-top: -15px;
}
</style>

<i18n>
en:
  SiteView:
    title: "Sites"
    description: "Manage and configure sites"
    create: "Add site"
fr:
  SiteView:
    title: "Sites"
    description: "Gérer et configurer les sites"
    create: "Ajouter un site"
</i18n>
