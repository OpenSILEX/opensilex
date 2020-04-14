<template>
  <div>
    <b-button
      v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
      @click="showCreateForm"
      variant="success"
    >{{$t('component.group.add')}}</b-button>
    <opensilex-GroupForm
      ref="groupForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
      @onCreate="callCreateGroupService"
      @onUpdate="callUpdateGroupService"
    ></opensilex-GroupForm>
    <opensilex-GroupList
      ref="groupList"
      v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_READ_ID)"
      @onEdit="editGroup"
      @onDelete="deleteGroup"
    ></opensilex-GroupList>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  GroupCreationDTO,
  GroupUpdateDTO,
  GroupGetDTO,
  SecurityService,
  ProfileGetDTO
} from "opensilex-security/index";

@Component
export default class GroupView extends Vue {
  $opensilex: any;
  $store: any;
  service: SecurityService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  async created() {
    this.service = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
  }

  @Ref("groupForm") readonly groupForm!: any;

  @Ref("groupList") readonly groupList!: any;

  showCreateForm() {
    let groupForm: any = this.groupForm;
    groupForm.showCreateForm();
  }

  callCreateGroupService(form: GroupCreationDTO, done) {
    done(
      this.service
        .createGroup( form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Group created", uri);
          let groupList: any = this.groupList;
          groupList.refresh();
        })
    );
  }

  callUpdateGroupService(form: GroupUpdateDTO, done) {
    done(
      this.service
        .updateGroup(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Group updated", uri);
          let groupList: any = this.groupList;
          groupList.refresh();
        })
    );
  }

  editGroup(form: GroupGetDTO) {
    let groupForm: any = this.groupForm;
    groupForm.showEditForm(form);
  }

  deleteGroup(uri: string) {
    this.service
      .deleteGroup( uri)
      .then(() => {
        let groupList: any = this.groupList;
        groupList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

