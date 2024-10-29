<template>
  <div class="container-fluid">
    <opensilex-CreateButton
        v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
        @click="AccountForm.showCreateForm()"
        label="AccountView.create"
        class="createButton">
    </opensilex-CreateButton>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-AccountList
            ref="AccountList"
            @onEdit="showEditForm($event)"
        ></opensilex-AccountList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
        ref="AccountForm"
        component="opensilex-AccountForm"
        createTitle="AccountView.create"
        editTitle="AccountView.update"
        icon="ik#ik-user"
        :lazy="true"
        @onCreate="AccountList.refresh()"
        @onUpdate="AccountList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {OpenSilexStore} from "../../models/Store";
import VueRouter from "vue-router";

@Component
export default class AccountView extends Vue {
  //#region Plugin & Services
  public readonly $store: OpenSilexStore;
  public readonly $router: VueRouter
  //#endregion

  //#region Refs
  @Ref("AccountForm") private readonly AccountForm!: any;
  @Ref("AccountList") private readonly AccountList!: any;
  //#endregion

  //#region Computed properties
  private get user() {
    return this.$store.state.user;
  }

  private get credentials() {
    return this.$store.state.credentials;
  }
  showEditForm(dto){
    let copydto = JSON.parse(JSON.stringify(dto));
    this.AccountForm.showEditForm(copydto);
  }
}
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