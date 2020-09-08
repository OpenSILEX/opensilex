<template>
  <b-card>
    <!-- Card header -->
    <template v-slot:header>
      <h3>
        <opensilex-Icon icon="ik#ik-globe" />
        {{$t("component.infrastructure.list")}}
      </h3>
      <div class="card-header-right">
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
          @click="createInfrastructure()"
          label="component.infrastructure.add"
        ></opensilex-CreateButton>
      </div>
    </template>
    <!-- Card body -->
    <!-- Infrastructure filter -->
    <opensilex-StringFilter
      :filter.sync="filter"
      @update="updateFilter()"
      placeholder="component.infrastructure.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TreeView :nodes.sync="nodes" @select="displayNodesDetail">
      <template v-slot:node="{ node }">
        <span class="item-icon">
          <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.type)" />
        </span>&nbsp;
        <strong v-if="node.data.selected">{{ node.title }}</strong>
        <span v-if="!node.data.selected">{{ node.title }}</span>
      </template>

      <template v-slot:buttons="{ node }">
        <opensilex-EditButton
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
          @click="editInfrastructure(node.data.uri)"
          label="component.infrastructure.facility.edit"
          :small="true"
        ></opensilex-EditButton>
        <opensilex-AddChildButton
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
          @click="createInfrastructure(node.data.uri)"
          label="component.infrastructure.facility.add-child"
          :small="true"
        ></opensilex-AddChildButton>
        <opensilex-DeleteButton
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID)"
          @click="deleteInfrastructure(node.data.uri)"
          label="component.infrastructure.facility.delete"
          :small="true"
        ></opensilex-DeleteButton>
      </template>
    </opensilex-TreeView>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
      ref="infrastructureForm"
      component="opensilex-InfrastructureForm"
      createTitle="component.infrastructure.add"
      editTitle="component.infrastructure.update"
      icon="ik#ik-globe"
      @onCreate="refresh($event.uri)"
      @onUpdate="refresh($event.uri)"
      :initForm="setParent"
    ></opensilex-ModalForm>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureGetDTO,
  InfrastructureFacilityGetDTO,
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

  private filter: any = "";

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService(
      "opensilex-core.InfrastructuresService"
    );

    let query: any = this.$route.query;
    if (query.filter) {
      this.filter = decodeURI(query.filter);
    }

    this.refresh();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        if (this.selected != null) {
          this.displayNodeDetail(this.selected.uri, true);
        }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  refresh(uri?) {
    this.service
      .searchInfrastructuresTree(this.filter)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        if (this.infrastructureForm) {
          this.infrastructureForm
            .getFormRef()
            .setParentInfrastructures(http.response.result);
        }
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

  public displayNodesDetail(node: any) {
    this.displayNodeDetail(node.data.uri);
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

  parentURI;
  createInfrastructure(parentURI?) {
    this.parentURI = parentURI;
    this.infrastructureForm.showCreateForm();
  }

  editInfrastructure(uri) {
    this.service
      .getInfrastructure(uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.parentURI = detailDTO.parent;
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

  setParent(form) {
    form.parent = this.parentURI;
  }
}
</script>

<style scoped lang="scss">
.sl-vue-tree-root {
  min-height: 100px;
  max-height: 300px;
  overflow-y: auto;
}

.leaf-spacer {
  display: inline-block;
  width: 23px;
}

@media (max-width: 768px) {
  .sl-vue-tree-root {
    min-height: auto;
  }
}
</style>

