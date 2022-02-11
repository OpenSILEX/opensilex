<template>
  <div class="container-fluid">
    <!-- <opensilex-PageHeader
      icon="ik#ik-user"
      title="UserView.title"
      description="UserView.description"
    ></opensilex-PageHeader> -->

    <!-- <opensilex-PageActions 
      class="pageActions"
      v-if="
      user.hasCredential(
        credentials.CREDENTIAL_USER_MODIFICATION_ID)
    ">
      <b-dropdown
        id="AddDropdown"
        class="top-menu-add-btn"
        :title="user.getAddMessage()"
        variant="link"
      >
        <template v-slot:button-content>
          <i class="icon ik ik-plus header-plus"></i>
        </template>
        <b-dropdown-item href="#"> -->
          <opensilex-CreateButton
            @click="userForm.showCreateForm()"
            label="UserView.create"
            class="createButton">
          </opensilex-CreateButton>
        <!-- </b-dropdown-item>
      </b-dropdown>
    </opensilex-PageActions> -->

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
.pageActions {
    position: fixed;
    top: 8px;
    left: 360px;
    width: 10px;
    background: none;
    z-index: 1100;
}

@media (min-width: 200px) and (max-width: 675px) {
  .pageActions {
   left: 270px
  }
}
.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}
</style>

<i18n>
en:
  UserView:
    title: Users
    description: Manage and configure users
    create: Add user
    update: Update user

fr:
  UserView:
    title: Utilisateurs
    description: Gérer et configurer les utilisateurs
    create: Ajouter un utilisateur
    update: Modifier l'utilisateur
</i18n>