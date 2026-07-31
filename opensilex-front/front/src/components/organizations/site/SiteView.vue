<template>
  <div class="container-fluid">
    <CreateButton
      id="createSiteButton"
      @click="onCreateClick"
      :label="t('component.site.create')"
      class="createButton"
    />

    <PageContent>
      <template v-slot>
        <SiteList
          ref="siteListRef"
          @onEdit="onSiteListEdit"
          :organizationsForFilter="organizationsForFilter"
        />
      </template>
    </PageContent>

    <SiteForm
      v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
      ref="siteFormRef"
      :createTitle="t('component.site.create')"
      :editTitle="t('component.site.update')"
      :initialOrganizations="organizationsForFilter"
      @onSuccess="siteListRef?.refresh?.()"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from "vue-i18n";
import PageContent from "@/components/layout/PageContent.vue";
import SiteList from "@/components/organizations/site/SiteList.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import SiteForm from "@/components/organizations/site/SiteForm.vue";

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
