<template>
  <div class="container-fluid">
    <opensilex-CreateButton
      @click="userForm.showCreateForm()"
      label="UserView.create"
      class="createButton">
    </opensilex-CreateButton>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-UserList
          ref="userList"
          @onEdit="showEditForm($event)"
        ></opensilex-UserList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
      ref="userForm"
      component="opensilex-UserForm"
      createTitle="UserView.create"
      editTitle="UserView.update"
      icon="ik#ik-user"
      :lazy="true"
      @onCreate="userList.refresh()"
      @onUpdate="userList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class UserView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("userForm") readonly userForm!: any;
  @Ref("userList") readonly userList!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  showEditForm(dto){
    let copydto = JSON.parse(JSON.stringify(dto));
    this.userForm.showEditForm(copydto);
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
  UserView:
    title: Accounts
    description: Manage and configure accounts
    create: Add account
    update: Update account

fr:
  UserView:
    title: Comptes
    description: Gérer et configurer les comptes
    create: Ajouter un compte
    update: Modifier le compte
</i18n>