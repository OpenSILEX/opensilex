<template>
  <div>
    <b-button
      @click="showCreateForm"
      variant="success"
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
    >{{$t('component.infrastructure.add')}}</b-button>
    <opensilex-core-InfrastructureForm
      ref="infrastructureForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
      @onCreate="callCreateInfrastructureService"
      @onUpdate="callUpdateInfrastructureService"
    ></opensilex-core-InfrastructureForm>
    <opensilex-core-InfrastructureTree
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_READ_ID)"
      ref="infrastructureTree"
      @onEdit="editInfrastructure"
      @onDelete="deleteInfrastructure"
    ></opensilex-core-InfrastructureTree>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { InfrastructuresService } from "../../lib/api/api";
import { ResourceTreeDTO, InfrastructureGetDTO } from "../../lib";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService(
      "opensilex.InfrastructuresService"
    );
  }

  showCreateForm() {
    let infrastructureForm: any = this.$refs.infrastructureForm;
    infrastructureForm.showCreateForm();
  }

  callCreateInfrastructureService(form: any, done) {
    done(
      this.service
        .createInfrastructure(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Infrastructure created", uri);
          let infraTree: any = this.$refs.infrastructureTree;
          infraTree.refresh();
        })
    );
  }

  callUpdateInfrastructureService(form: InfrastructureGetDTO, done) {
    done();
    // this.service
    //   .updateInfrastructure(this.user.getAuthorizationHeader(), form)
    //   .then((http: HttpResponse<OpenSilexResponse<any>>) => {
    //     let uri = http.response.result;
    //     console.debug("Infrastructure updated", uri);
    //     let groupList: any = this.$refs.groupList;
    //     groupList.refresh();
    //   })
  }

  editUser(form: InfrastructureGetDTO) {
    let infrastructureForm: any = this.$refs.infrastructureForm;
    infrastructureForm.showEditForm(form);
  }

  deleteUser(uri: string) {
    this.service
      .deleteInfrastructure(this.user.getAuthorizationHeader(), uri)
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

