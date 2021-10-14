<template>
  <b-card>
    <!-- Card header -->
    <template v-slot:header>
      <h3>
        {{ $t("InfrastructureTree.infrastructure-component") }}
        &nbsp;
        <font-awesome-icon
          icon="question-circle"
          v-b-tooltip.hover.top="$t('InfrastructureTree.infrastructure-help')"
        />
      </h3>

      <div class="card-header-right">
        <opensilex-CreateButton
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
            )
          "
          @click="createInfrastructure()"
          label="InfrastructureTree.add"
        ></opensilex-CreateButton>
      </div>
    </template>
    <!-- Card body -->
    <!-- Infrastructure filter -->
    <opensilex-StringFilter
      :filter.sync="filter"
      @update="updateFilter()"
      placeholder="InfrastructureTree.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TreeView :nodes.sync="nodes" @select="displayNodesDetail">
      <template v-slot:node="{ node }">
        <span class="item-icon">
          <opensilex-Icon
            :icon="$opensilex.getRDFIcon(node.data.rdf_type)"
          /> </span
        >&nbsp;
        <strong v-if="node.data.selected">{{ node.title }}</strong>
        <span v-if="!node.data.selected">{{ node.title }}</span>
      </template>

      <template v-slot:buttons="{ node }">
        <opensilex-DetailButton
          @click="showDetail(node.data.uri)"
          label="InfrastructureTree.showDetail"
          :small="true"
        ></opensilex-DetailButton>
        <opensilex-EditButton
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
            )
          "
          @click="editInfrastructure(node.data.uri)"
          label="InfrastructureTree.edit"
          :small="true"
        ></opensilex-EditButton>
        <opensilex-AddChildButton
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
            )
          "
          @click="createInfrastructure(node.data.uri)"
          label="InfrastructureTree.add-child"
          :small="true"
        ></opensilex-AddChildButton>
        <opensilex-DeleteButton
          v-if="
            user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID)
          "
          @click="deleteInfrastructure(node.data.uri)"
          label="InfrastructureTree.delete"
          :small="true"
        ></opensilex-DeleteButton>
      </template>
    </opensilex-TreeView>

    <opensilex-ModalForm
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
        )
      "
      ref="infrastructureForm"
      component="opensilex-InfrastructureForm"
      createTitle="InfrastructureTree.add"
      editTitle="InfrastructureTree.update"
      icon="ik#ik-globe"
      @onCreate="refresh($event ? $event.uri : undefined)"
      @onUpdate="refresh($event ? $event.uri : undefined)"
      :initForm="setParent"
    ></opensilex-ModalForm>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { OrganisationsService, ResourceTreeDTO, InfrastructureGetDTO } from "opensilex-core/index";
import {InfrastructureUpdateDTO} from "opensilex-core/model/infrastructureUpdateDTO";

@Component
export default class InfrastructureTree extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  service: OrganisationsService;

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
      "opensilex-core.OrganisationsService"
    );

    let query: any = this.$route.query;
    if (query.filter) {
      this.filter = decodeURIComponent(query.filter);
    }

    this.refresh();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
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
        if (this.infrastructureForm && this.infrastructureForm.getFormRef()) {
          if (this.filter == "") {
            this.infrastructureForm
              .getFormRef()
              .setParentInfrastructures(http.response.result);
          } else {
            this.infrastructureForm.getFormRef().init();
          }
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
      isSelectable: true,
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

  showDetail(uri) {
    this.$router.push({
      path: "/infrastructure/details/" + encodeURIComponent(uri),
    });
  }

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

        let editDTO: InfrastructureUpdateDTO = {
          ...detailDTO,
          groups: detailDTO.groups.map(group => group.uri)
        };
        this.infrastructureForm.showEditForm(editDTO);
      });
  }

  deleteInfrastructure(uri: string) {
    this.service
      .deleteInfrastructure(uri)
      .then(() => {
        this.refresh();
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

<i18n>
en:
  InfrastructureTree:
    filter-placeholder: Search organizations...
    add: Add organization
    update: Update organization
    edit: Edit organization
    add-child: Add sub-organization
    delete: Delete organization
    infrastructure-component: Organizations
    infrastructure-help: "The organizations represent the hierarchy between the different sites, units, ... with a specific address and / or with dedicated teams."
    showDetail: Organization details
fr:
  InfrastructureTree:
    filter-placeholder: Rechercher des organisations...
    add: Ajouter une organisation
    update: Modifier l'organisation
    edit: Editer l'organisation
    add-child:  Ajouter une sous-organisation
    delete: Supprimer l'organisation
    infrastructure-component: Organisations
    infrastructure-help: "Les organisations représentent la hiérarchie entre les différents sites, unités, ... disposant d'une adresse particulière et/ou avec des équipes dédiées."
    showDetail: Détail de l'organisation
</i18n>
