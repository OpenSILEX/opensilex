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
      :profiles="profiles"
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
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  GroupsService,
  GroupCreationDTO,
  GroupUpdateDTO,
  GroupGetDTO,
  ProfilesService,
  ProfileGetDTO
} from "opensilex-rest/index";

@Component
export default class GroupView extends Vue {
  $opensilex: any;
  $store: any;
  service: GroupsService;
  profiles: Array<ProfileGetDTO> = [];

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  async created() {
    this.service = this.$opensilex.getService("opensilex.GroupsService");
    console.debug("Loading profiles list...");
    let profilesService: ProfilesService = await this.$opensilex.loadService(
      "opensilex-rest.ProfilesService"
    );
    let http: HttpResponse<OpenSilexResponse<
      Array<ProfileGetDTO>
    >> = await profilesService.getAllProfiles(
      this.$opensilex.getUser().getAuthorizationHeader()
    );
    this.profiles = http.response.result;
    console.debug("Profiles list loaded !", this.profiles);
  }

  showCreateForm() {
    let groupForm: any = this.$refs.groupForm;
    groupForm.showCreateForm();
  }

  callCreateGroupService(form: GroupCreationDTO, done) {
    done(
      this.service
        .createGroup(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Group created", uri);
          let groupList: any = this.$refs.groupList;
          groupList.refresh();
        })
    );
  }

  callUpdateGroupService(form: GroupUpdateDTO, done) {
    done(
      this.service
        .updateGroup(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Group updated", uri);
          let groupList: any = this.$refs.groupList;
          groupList.refresh();
        })
    );
  }

  editGroup(form: GroupGetDTO) {
    let groupForm: any = this.$refs.groupForm;
    groupForm.showEditForm(form);
  }

  deleteGroup(uri: string) {
    this.service
      .deleteGroup(this.user.getAuthorizationHeader(), uri)
      .then(() => {
        let groupList: any = this.$refs.groupList;
        groupList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

