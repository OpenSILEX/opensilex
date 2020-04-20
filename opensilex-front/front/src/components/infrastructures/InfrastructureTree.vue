<template>
  <div class="card">
    <div class="card-header">
      <h3>
        <i class="ik ik-globe"></i>
        {{$t("component.infrastructure.list")}}
      </h3>
      <div class="card-header-right">
        <b-button
          @click="showCreateForm(null)"
          variant="primary"
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
        >
          <i class="ik ik-plus"></i>
          {{$t('component.infrastructure.add')}}
        </b-button>
      </div>
    </div>
    <div class="card-body">
      <b-input-group>
        <b-form-input
          v-model="filterPattern"
          debounce="300"
          :placeholder="$t('component.infrastructure.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterPattern" variant="primary" @click="filterPattern = ''">
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>
      </b-input-group>
      <sl-vue-tree v-model="nodes" @select="displayNodesDetail">
        <template slot="toggle" slot-scope="{ node }">
          <span v-if="!node.isLeaf">
            <font-awesome-icon v-if="node.isExpanded" icon="chevron-down" size="sm" />
            <font-awesome-icon v-if="!node.isExpanded" icon="chevron-right" size="sm" />
          </span>
        </template>

        <template slot="title" slot-scope="{ node }">
          <div v-if="node.isLeaf && node.data.parent" class="leaf-spacer"></div>
          <span class="item-icon">
            <font-awesome-icon :icon="$opensilex.getRDFIcon(node.data.type)" size="sm" />
          </span>&nbsp;
          <strong v-if="node.data.selected">{{ node.title }}</strong>
          <span v-if="!node.data.selected">{{ node.title }}</span>
          <b-button-group class="tree-button-group" size="sm">
            <b-button
              size="sm"
              v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
              @click.prevent="editInfrastructure(node.data.uri)"
              variant="outline-primary"
            >
              <font-awesome-icon icon="edit" size="sm" />
            </b-button>
            <b-button
              size="sm"
              v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
              @click.prevent="showCreateForm(node.data.uri)"
              variant="outline-success"
            >
              <font-awesome-icon icon="plus" size="sm" />
            </b-button>
            <b-button
              size="sm"
              v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID)"
              @click.prevent="deleteInfrastructure(node.data.uri)"
              variant="danger"
            >
              <font-awesome-icon icon="trash-alt" size="sm" />
            </b-button>
          </b-button-group>
        </template>
      </sl-vue-tree>
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
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureGetDTO,
  InfrastructureDeviceGetDTO,
  InfrastructureTeamDTO,
  InfrastructureUpdateDTO
} from "opensilex-core/index";
import { GroupCreationDTO, GroupUpdateDTO } from "opensilex-security/index";

@Component
export default class InfrastructureTree extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    this.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  created() {
    this.service = this.$opensilex.getService(
      "opensilex-core.InfrastructuresService"
    );

    let query: any = this.$route.query;
    if (query.filterPattern) {
      this.filterPatternValue = decodeURI(query.filterPattern);
    }

    this.initParentOptions();
    this.refresh();
  }

  mounted() {
    this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        if (this.selected != null) {
          this.displayNodeDetail(this.selected.uri, true);
        }
      }
    );
  }

  parentOptions = [];
  private initParentOptions() {
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

  refresh(uri?) {
    let pattern = this.filterPattern;
    this.service
      .searchInfrastructuresTree(pattern)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        let treeNode = [];

        let first = true;
        for (let i in http.response.result) {
          let resourceTree: ResourceTreeDTO = http.response.result[i];
          let node = this.dtoToNode(resourceTree, first, uri);
          treeNode.push(node);

          if (first && uri == null) {
            this.displayNodeDetail(node.data.uri, true);
            first = false;
          }
        }

        if (uri != null) {
          this.displayNodeDetail(uri, true);
        }

        if (http.response.result.length == 0) {
          this.selected = null;
        }

        this.nodes = treeNode;

        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              filterPattern: encodeURI(pattern)
            }
          })
          .catch(function() {});
      })
      .catch(this.$opensilex.errorHandler);
  }

  private dtoToNode(dto: ResourceTreeDTO, first: boolean, uri: string) {
    let isLeaf = dto.children.length == 0;

    let childrenDTOs = [];
    if (!isLeaf) {
      for (let i in dto.children) {
        childrenDTOs.push(this.dtoToNode(dto.children[i], false, uri));
      }
    }

    let isSeleted = first && uri == null;
    if (uri != null) {
      isSeleted = uri == dto.uri;
    }
    return {
      title: dto.name,
      data: dto,
      isLeaf: isLeaf,
      children: childrenDTOs,
      isExpanded: true,
      isSelected: isSeleted,
      isDraggable: false,
      isSelectable: true
    };
  }

  public nodes = [];

  private selected: InfrastructureGetDTO;

  public displayNodesDetail(nodes: any[]) {
    if (nodes.length > 0) {
      let node = nodes[nodes.length - 1];
      this.displayNodeDetail(node.data.uri);
    }
  }

  public displayNodeDetail(uri: string, forceRefresh?: boolean) {
    if (forceRefresh || this.selected == null || this.selected.uri != uri) {
      return this.service
        .getInfrastructure(uri)
        .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
          let detailDTO: InfrastructureGetDTO = http.response.result;
          this.selected = detailDTO;
          this.$emit("onSelect", detailDTO);
        });
    }
  }

  @Ref("infrastructureForm") readonly infrastructureForm!: any;

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
          this.refresh(uri);
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
        this.refresh(uri);
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
        this.refresh(uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
.table {
  border: 1px solid #dee2e6;
  border-radius: 3px;
  border-collapse: separate;
}
.table th {
  text-align: left;
}

.sl-vue-tree-root {
  min-height: 100px;
  max-height: 300px;
  overflow-y: auto;
}

.user-list {
  padding-left: 10px;
}

.leaf-spacer {
  display: inline-block;
  width: 23px;
}

.device-split-cell {
  width: 50%;
  display: inline-block;
}

.add-device-button-container {
  text-align: right;
}

.table-device {
  margin-top: 10px;
}

.button-cell {
  padding-top: 1px;
  padding-bottom: 1px;
}

@media (max-width: 768px) {
  .sl-vue-tree-root {
    min-height: auto;
  }

  .table {
    margin-top: 10px;
  }
}
</style>

