<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-user"
      title="component.menu.security.users"
      description="component.user.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
          @click="userForm.showCreateForm()"
          label="component.user.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-UserList
          v-if="user.hasCredential(credentials.CREDENTIAL_USER_READ_ID)"
          ref="userList"
          @onEdit="userForm.showEditForm($event)"
        ></opensilex-UserList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
      ref="userForm"
      component="opensilex-UserForm"
      createTitle="component.user.add"
      editTitle="component.user.update"
      icon="ik#ik-user"
      @onCreate="userList.refresh()"
      @onUpdate="userList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  UserCreationDTO,
  UserUpdateDTO,
  SecurityService,
  UserGetDTO
} from "opensilex-security/index";

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
}
</script>

<style scoped lang="scss">
</style>

