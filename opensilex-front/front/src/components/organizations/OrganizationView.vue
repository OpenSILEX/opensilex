<template>
  <div class="container-fluid">
    <CreateButton
      id="createOrgaButton"
      @click="onCreateClick"
      :label="t('OrganizationView.create')"
      class="createButton"
    />

    <PageContent>
      <OrganizationListComponent
        ref="organizationList"
        @onEdit="onOrganizationListEdit"
      />
    </PageContent>

    <OrganizationFormComponent
      v-if="user?.hasCredential(credentials?.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
      ref="organizationForm"
      :createTitle="t('OrganizationView.create')"
      :editTitle="t('OrganizationView.update')"
      @onSuccess="refreshList"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, useTemplateRef } from "vue";
import type OrganizationList from "@/components/organizations/OrganizationList.vue";
import type OrganizationForm from "@/components/organizations/OrganizationForm.vue";
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n'
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import OrganizationListComponent from "@/components/organizations/OrganizationList.vue";
import OrganizationFormComponent from "@/components/organizations/OrganizationForm.vue";

const store = useStore();
const { t } = useI18n()

const organizationForm = useTemplateRef<InstanceType<typeof OrganizationFormComponent>>('organizationForm');
const organizationList = useTemplateRef<InstanceType<typeof OrganizationListComponent>>('organizationList');

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

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