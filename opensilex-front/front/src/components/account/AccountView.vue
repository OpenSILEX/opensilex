<template>
  <div class="container-fluid">
    <CreateButton
      v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
      @click="onCreateButtonClick()"
      :label="t('AccountView.create')"
      class="createButton">
    </CreateButton>

    <PageContent>
      <template v-slot>
        <AccountList
          ref="accountList"
          @onEdit="onAccountListEdit($event)"
        ></AccountList>
      </template>
    </PageContent>

    <ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
      ref="accountForm"
      component="opensilex-AccountForm"
      createTitle="AccountView.create"
      editTitle="AccountView.update"
      icon="ik#ik-user"
      :lazy="true"
      @onCreate="accountList.refresh()"
      @onUpdate="accountList.refresh()"
    ></ModalForm>
  </div>
</template>

<script setup lang="ts">
import {useStore} from "vuex";
import {computed, useTemplateRef} from "vue";
import {OpenSilexStore} from "@/models/Store";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import AccountList from "@/components/account/AccountList.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import {useI18n} from "vue-i18n";

const store = useStore() as OpenSilexStore;
const { t } = useI18n();

//#region Refs
const accountForm = useTemplateRef<InstanceType<typeof ModalForm>>('accountForm');
const accountList = useTemplateRef<InstanceType<typeof AccountList>>('accountList');
//#endregion

//#region Computed properties
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
//#endregion

//#region Event handlers and watchers
function onCreateButtonClick(): void {
  accountForm.value?.showCreateForm();
}

function onAccountListEdit(dto: any): void {
  const copydto = JSON.parse(JSON.stringify(dto));
  accountForm.value?.showEditForm(copydto);
}
//#endregion
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}
</style>

<i18n>
en:
  AccountView:
    title: Accounts
    description: Manage and configure accounts
    create: Add account
    update: Update account

fr:
  AccountView:
    title: Comptes
    description: Gérer et configurer les comptes
    create: Ajouter un compte
    update: Modifier le compte
</i18n>