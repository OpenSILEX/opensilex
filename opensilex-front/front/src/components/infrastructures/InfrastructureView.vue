<template>
  <div>
    <b-button
      @click="showCreateForm(null)"
      variant="success"
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
    >{{$t('component.infrastructure.add')}}</b-button>
    <opensilex-InfrastructureForm
      ref="infrastructureForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
      :parentOptions="parentOptions"
      @onCreate="callCreateInfrastructureService"
      @onUpdate="callUpdateInfrastructureService"
    ></opensilex-InfrastructureForm>
    <opensilex-InfrastructureTree
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_READ_ID)"
      ref="infrastructureTree"
      @onEdit="editInfrastructure"
      @onDelete="deleteInfrastructure"
      @onAddChild="showCreateForm"
      @onSelect="(v) => defaultParent = v"
    ></opensilex-InfrastructureTree>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureGetDTO,
  InfrastructureUpdateDTO
} from "opensilex-core/index";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;

  parentOptions = [];

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  public created() {
    this.service = this.$opensilex.getService(
      "opensilex-core.InfrastructuresService"
    );

    this.refresh();
  }

  private refresh() {
    this.service
      .searchInfrastructuresTree(this.user.getAuthorizationHeader())
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        let infrastructures = http.response.result;

        let options = [];
        infrastructures.forEach((resourceTree: ResourceTreeDTO) => {
          options.push(this.buildTreeOptions(resourceTree));
        });

        this.parentOptions = options;
      })
      .catch(this.$opensilex.errorHandler);
  }

  private buildTreeOptions(resourceTree: ResourceTreeDTO) {
    let option = {
      id: resourceTree.uri,
      label: resourceTree.name,
      isDefaultExpanded: true,
      children: []
    };

    resourceTree.children.forEach(child => {
      option.children.push(this.buildTreeOptions(child));
    });

    if (option.children.length == 0) {
      delete option.children;
    }
    return option;
  }

  showCreateForm(parentURI) {
    let infrastructureForm: any = this.$refs.infrastructureForm;
    infrastructureForm.showCreateForm(parentURI);
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
          this.refresh();
        })
    );
  }

  callUpdateInfrastructureService(form: InfrastructureUpdateDTO, done) {
    done();
    this.service
      .updateInfrastructure(this.user.getAuthorizationHeader(), form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure updated", uri);
        let groupList: any = this.$refs.groupList;
        groupList.refresh();
      })
  }

  editInfrastructure(form: InfrastructureGetDTO) {
    let infrastructureForm: any = this.$refs.infrastructureForm;
    infrastructureForm.showEditForm(form);
  }

  deleteInfrastructure(uri: string) {
    this.service
      .deleteInfrastructure(this.user.getAuthorizationHeader(), uri)
      .then(() => {
        let infrastructureTree: any = this.$refs.infrastructureTree;
        infrastructureTree.refresh();
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

