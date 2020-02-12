<template>
  <div>
    <b-button
      @click="showCreateForm"
      variant="success"
      v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
    >{{$t('component.user.add')}}</b-button>
    <opensilex-UserForm
      ref="userForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
      @onCreate="callCreateUserService"
      @onUpdate="callUpdateUserService"
    ></opensilex-UserForm>
    <opensilex-UserList
      v-if="user.hasCredential(credentials.CREDENTIAL_USER_READ_ID)"
      ref="userList"
      @onEdit="editUser"
      @onDelete="deleteUser"
    ></opensilex-UserList>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  UserCreationDTO,
  UserUpdateDTO,
  UsersService,
  UserGetDTO
} from "opensilex-rest/index";

@Component
export default class UserView extends Vue {
  $opensilex: any;
  $store: any;
  service: UsersService;
  
  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.UsersService");
  }

  showCreateForm() {
    let userForm: any = this.$refs.userForm;
    userForm.showCreateForm();
  }

  callCreateUserService(form: UserCreationDTO, done) {
    done(
      this.service
        .createUser(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("User created", uri);
          let userList: any = this.$refs.userList;
          userList.refresh();
        })
    );
  }

  callUpdateUserService(form: UserUpdateDTO, done) {
    done(
      this.service
        .updateUser(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("User updated", uri);
          let userList: any = this.$refs.userList;
          userList.refresh();
        })
    );
  }

  editUser(form: UserGetDTO) {
    let userForm: any = this.$refs.userForm;
    userForm.showEditForm(form);
  }

  deleteUser(uri: string) {
    this.service
      .deleteUser(this.user.getAuthorizationHeader(), uri)
      .then(() => {
        let userList: any = this.$refs.userList;
        userList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

