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
          @click="showCreateForm"
          label="component.user.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-UserList
          v-if="user.hasCredential(credentials.CREDENTIAL_USER_READ_ID)"
          ref="userList"
          @onEdit="showEditForm"
          @onDelete="deleteUser"
        ></opensilex-UserList>
      </template>
    </opensilex-PageContent>
    <opensilex-UserForm
      ref="userForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
      @onCreate="createUser"
      @onUpdate="updateUser"
    ></opensilex-UserForm>
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

  created() {
    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  createUser(form: UserCreationDTO, done) {
    done(
      this.service
        .createUser(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("User created", uri);
          let userList: any = this.userList;
          userList.refresh();
        })
    );
  }

  updateUser(form: UserUpdateDTO, done) {
    done(
      this.service
        .updateUser(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("User updated", uri);
          let userList: any = this.userList;
          userList.refresh();
        })
    );
  }

  deleteUser(uri: string) {
    this.service
      .deleteUser(uri)
      .then(() => {
        let userList: any = this.userList;
        userList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  showCreateForm() {
    this.userForm.showCreateForm();
  }

  showEditForm(form) {
    this.userForm.showEditForm(form);
  }
}
</script>

<style scoped lang="scss">
</style>

