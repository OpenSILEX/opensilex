<template>
  <div>
    <b-button @click="showCreateForm" variant="success">Add user</b-button>
    <opensilex-UserForm
      ref="userForm"
      @onCreate="callCreateUserService"
      @onUpdate="callUpdateUserService"
    ></opensilex-UserForm>
    <opensilex-UserList ref="UserList" @onEdit="editUser" @onDelete="deleteUser"></opensilex-UserList>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { UserCreationDTO, UserUpdateDTO, UserService, UserGetDTO } from "opensilex-rest/index";

@Component
export default class UserView extends Vue {
  $opensilex: any;
  $store: any;
  service: UserService;

  get user() {
    return this.$store.state.user;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.UserService");
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
          let userList: any = this.$refs.UserList;
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
          let userList: any = this.$refs.UserList;
          userList.refresh();
        })
    );
  }

  editUser(form: UserGetDTO) {
    let userForm: any = this.$refs.userForm;
    userForm.showEditForm(form);
  }

  deleteUser(uri: string) {
    this.service.deleteUser(this.user.getAuthorizationHeader(), uri).then(() => {
      let userList: any = this.$refs.UserList;
      userList.refresh();
    })
    .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

