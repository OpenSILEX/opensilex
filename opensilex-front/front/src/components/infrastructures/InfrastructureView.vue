<template>
  <div class="container-fluid">
    <opensilex-PageHeader icon="ik-globe" title="component.menu.infrastructures" description="component.infrastructure.description"></opensilex-PageHeader>
    <div class="card">
      <div class="card-header row clearfix">
        <div class="col col-sm-3">
          <div class="card-options d-inline-block">
            <b-button
              @click="showCreateForm(null)"
              variant="primary"
              v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
            >
              <i class="ik ik-plus"></i>
              {{$t('component.infrastructure.add')}}
            </b-button>
            <b-tooltip
              target="create-experiment"
            >{{ $t('component.experiment.search.buttons.create-experiment-help') }}</b-tooltip>
          </div>
        </div>
      </div>
    </div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <opensilex-InfrastructureTree
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_READ_ID)"
          ref="infrastructureTree"
          @onEdit="editInfrastructure"
          @onDelete="deleteInfrastructure"
          @onAddChild="showCreateForm"
          @onSelect="getNodeDetails"
        ></opensilex-InfrastructureTree>
      </div>
    </div>
    <opensilex-InfrastructureForm
      ref="infrastructureForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
      :parentOptions="parentOptions"
      @onCreate="callCreateInfrastructureService"
      @onUpdate="callUpdateInfrastructureService"
    ></opensilex-InfrastructureForm>
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
    this.infrastructureForm.showCreateForm(parentURI);
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

