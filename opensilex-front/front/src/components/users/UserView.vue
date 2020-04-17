<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik-user"
      title="component.menu.security.users"
      description="component.user.description"
    ></opensilex-PageHeader>
    <div class="card">
      <div class="card-header row clearfix">
        <div class="col col-sm-3">
          <div class="card-options d-inline-block">
            <b-button
              @click="showCreateForm"
              variant="primary"
              v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
            >
              <i class="ik ik-plus"></i>
              {{$t('component.user.add')}}
            </b-button>
          </div>
        </div>
      </div>
    </div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <opensilex-UserList
          v-if="user.hasCredential(credentials.CREDENTIAL_USER_READ_ID)"
          ref="userList"
          @onEdit="editUser"
          @onDelete="deleteUser"
        ></opensilex-UserList>
      </div>
    </div>
    <opensilex-UserForm
      ref="userForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
      @onCreate="callCreateUserService"
      @onUpdate="callUpdateUserService"
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
  service: SecurityService;

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

  showCreateForm() {
    let userForm: any = this.userForm;
    userForm.showCreateForm();
  }

  callCreateUserService(form: UserCreationDTO, done) {
    console.log(form);
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

  callUpdateUserService(form: UserUpdateDTO, done) {
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

  editUser(form: UserGetDTO) {
    let userForm: any = this.userForm;
    userForm.showEditForm(form);
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
}
</script>

<style scoped lang="scss">
</style>

