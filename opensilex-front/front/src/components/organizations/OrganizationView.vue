<template>
  <div class="container-fluid">
    <opensilex-CreateButton
      id="createOrgaButton"
      @click="onCreateClick"
      :label="t('OrganizationView.create')"
      class="createButton"
    />

    <opensilex-PageContent>
      <opensilex-OrganizationList
        ref="organizationList"
        @onEdit="onOrganizationListEdit"
      />
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user?.hasCredential(credentials?.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
      ref="organizationForm"
      :lazy="true"
      component="opensilex-OrganizationForm"
      :createTitle="t('OrganizationView.create')"
      :editTitle="t('OrganizationView.update')"
      icon="bi#bi-geo-alt"
      @onCreate="refreshList"
      @onUpdate="refreshList"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, getCurrentInstance } from "vue";
import type OrganizationList from "@/components/organizations/OrganizationList.vue";
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n'

const instance = getCurrentInstance();
const store = useStore();
const { t } = useI18n()

// refs
const organizationForm = ref<any>(null);
const organizationList = ref<InstanceType<typeof OrganizationList> | null>(null);

// computed
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

// handlers
function onOrganizationListEdit(dto: any) {
  organizationForm.value?.showEditForm(dto);
}

function onCreateClick() {
  organizationForm.value?.showCreateForm();
}

function refreshList() {
  organizationList.value?.refresh();
}
</script>

<style scoped lang="scss">
#createOrgaButton {
  margin-bottom: 10px;
  margin-top: -15px;
}
</style>

<i18n>
en:
  OrganizationView:
    title: "Organizations"
    description: "Manage and configure organizations"
    create: "Add organization"
    update: "Update organization"
fr:
  OrganizationView:
    title: "Organisations"
    description: "Gérer et configurer les organisations"
    create: "Ajouter une organisation"
    update: "Modifier une organisation"
</i18n>