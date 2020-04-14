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
      @onSelect="getNodeDetails"
    ></opensilex-InfrastructureTree>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
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
  $i18n: any;

  selected: InfrastructureGetDTO = null;
  getNodeDetails(value) {
    this.selected = value;
  }
  parentOptions = [];

  @Ref("infrastructureForm") readonly infrastructureForm!: any;

  @Ref("infrastructureTree") readonly infrastructureTree!: any;

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
      .searchInfrastructuresTree()
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        let infrastructures = http.response.result;
        this.parentOptions = this.$opensilex.buildTreeListOptions(
          infrastructures
        );
      })
      .catch(this.$opensilex.errorHandler);
  }

  showCreateForm(parentURI) {
    let infrastructureForm: any = this.infrastructureForm;
    infrastructureForm.showCreateForm(parentURI);
  }

  callCreateInfrastructureService(form: any, done) {
    done(
      this.service
        .createInfrastructure(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Infrastructure created", uri);
          let infraTree: any = this.infrastructureTree;
          infraTree.refresh();
          this.refresh();
        })
    );
  }

  callUpdateInfrastructureService(form: InfrastructureUpdateDTO, done) {
    done();
    this.service
      .updateInfrastructure(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure updated", uri);
        let infraTree: any = this.infrastructureTree;
        infraTree.refresh(uri);
        this.refresh();
      });
  }

  editInfrastructure(uri) {
    this.service
      .getInfrastructure(uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.infrastructureForm.showEditForm(detailDTO);
      });
  }

  deleteInfrastructure(uri: string) {
    this.service
      .deleteInfrastructure(uri)
      .then(() => {
        this.infrastructureTree.refresh();
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

