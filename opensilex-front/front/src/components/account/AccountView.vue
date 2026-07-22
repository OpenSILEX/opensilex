<template>
  <div class="container-fluid">
    <CreateButton
      v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
      @click="onCreateButtonClick()"
      :label="t('component.account.add')"
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

    <AccountForm
      v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
      ref="accountForm"
      @onCreate="accountList.refresh()"
      @onUpdate="accountList.refresh()"
    ></AccountForm>
  </div>
</template>

<script setup lang="ts">
import {useStore} from "vuex";
import {computed, useTemplateRef} from "vue";
import {OpenSilexStore} from "@/models/Store";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import AccountList from "@/components/account/AccountList.vue";
import {useI18n} from "vue-i18n";
import AccountForm from "@/components/account/AccountForm.vue";

const store = useStore() as OpenSilexStore;
const { t } = useI18n();

//#region Refs
const accountForm = useTemplateRef<InstanceType<typeof AccountForm>>('accountForm');
const accountList = useTemplateRef<InstanceType<typeof AccountList>>('accountList');
//#endregion

//#region Computed properties
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
//#endregion

//#region Event handlers and watchers
function onCreateButtonClick(): void {
  accountForm.value.showCreateForm();
}

function onAccountListEdit(dto: any): void {
  const copydto = JSON.parse(JSON.stringify(dto));
  accountForm.value.showEditForm(copydto);
}
//#endregion
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}
</style>