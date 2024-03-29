<template>
<div >
        <div class="spaced-actions">
          <opensilex-CreateButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
              )
            "
            @click="createOrganization()"
            label="OrganizationTree.add"
            class="createButton firstButton"
          ></opensilex-CreateButton>
          <opensilex-CreateButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
              )
            "
            @click="createSite()"
            label="OrganizationTree.addSite"
            class="createButton"
          ></opensilex-CreateButton>
        </div>
  <b-card>
    <!-- Card header -->
    <template v-slot:header>
      <h3>
        {{ $t("OrganizationTree.organization-component") }}
        &nbsp;
        <font-awesome-icon
          icon="question-circle"
          class="organizationHelp"
          v-b-tooltip.hover.top="$t('OrganizationTree.organization-help')"
        />
      </h3>

    </template>

    <!-- Card body -->
    <!-- Organization filter -->
    <opensilex-StringFilter
      :filter.sync="filter"
      :debounce="300"
      :lazy="false"
      @update="updateFilter()"
      placeholder="OrganizationTree.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TreeView
        :nodes.sync="nodes"
        @select="displayNodesDetail"
        ref="treeView"
    >
      <template v-slot:node="{ node }">
        <span class="item-icon">
          <opensilex-Icon
              :icon="$opensilex.getRDFIcon(node.data.rdf_type)"
          />
        </span>&nbsp;
        <strong v-if="node.data.selectedOrganization">{{ node.title }}</strong>
        <span v-if="!node.data.selectedOrganization">{{ node.title }}</span>
        <span class="tree-multiple-icon">
          <opensilex-Icon
              v-if="hasMultipleParents(node)"
              icon="fa#project-diagram"
              v-b-tooltip.hover.top="getMultipleParentsTooltip(node)" />
        </span>
      </template>

      <template v-slot:buttons="{ node }">
        <opensilex-DetailButton
          @click="showOrganizationOrSiteDetail(node.data)"
          :label="node.data.isOrganization ? $t('OrganizationTree.showDetail') : $t('OrganizationTree.showDetailSite')"
          :small="true"
        ></opensilex-DetailButton>
        <opensilex-EditButton
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
            )
          "
          @click="editOrganizationOrSite(node.data)"
          :label="node.data.isOrganization ? $t('OrganizationTree.edit') : $t('OrganizationTree.editSite')"
          :small="true"
        ></opensilex-EditButton>
        <opensilex-Dropdown
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
              ) && node.data.isOrganization
            "
            @click="(option) => createOrganizationOrSite(option, node.data.uri)"
            :small="true"
            :options="createOptions"
            icon="fa#plus"
            variant="outline-success"
            right
        >
        </opensilex-Dropdown>
        <opensilex-DeleteButton
          v-if="
            user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_DELETE_ID)
          "
          @click="deleteOrganizationOrSite(node.data)"
          :label="node.data.isOrganization ? $t('OrganizationTree.delete') : $t('OrganizationTree.deleteSite')"
          :small="true"
        ></opensilex-DeleteButton>
      </template>
    </opensilex-TreeView>

    <opensilex-ModalForm
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
        )
      "
      ref="organizationForm"
      component="opensilex-OrganizationForm"
      createTitle="OrganizationTree.add"
      editTitle="OrganizationTree.update"
      icon="ik#ik-globe"
      @onCreate="onCreate"
      @onUpdate="onUpdate"
      :initForm="initOrganizationForm"
      :lazy="true"
    ></opensilex-ModalForm>
    <opensilex-ModalForm
        v-if="
          user.hasCredential(
            credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
          )
        "
        ref="siteForm"
        component="opensilex-SiteForm"
        createTitle="OrganizationTree.addSite"
        editTitle="OrganizationTree.editSite"
        icon="ik#ik-globe"
        @onCreate="onCreate"
        @onUpdate="onUpdate"
        :initForm="initSiteForm"
        :doNotHideOnError="true"
        :lazy="true"
    ></opensilex-ModalForm>
  </b-card>
</div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {
  OrganizationGetDTO,
  OrganizationUpdateDTO,
  ResourceDagDTO,
  SiteGetDTO,
  SiteUpdateDTO
} from "opensilex-core/index";
import OpenSilexVuePlugin, {GenericTreeOption, TreeOption} from "../../models/OpenSilexVuePlugin";
import ModalForm from "../common/forms/ModalForm.vue";
import {DropdownButtonOption} from "../common/dropdown/Dropdown.vue";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import TreeView from "../common/views/TreeView.vue";
import DTOConverter from "../../models/DTOConverter";
import OrganizationForm from "./OrganizationForm.vue";
import {OrganizationCreationDTO} from "opensilex-core/model/organizationCreationDTO";
import SiteForm from "./site/SiteForm.vue";
import {SiteCreationDTO} from "opensilex-core/model/siteCreationDTO";
import {SiteGetListDTO} from "opensilex-core/model/siteGetListDTO";
import {OpenSilexStore} from "../../models/Store";
import {Route} from "vue-router";

type OrganizationOrSiteData = ResourceDagDTO & {
  isOrganization: boolean,
  isSite: boolean
}

interface OrganizationOrSiteTreeNode extends TreeOption<OrganizationOrSiteTreeNode> {
  data: OrganizationOrSiteData
}

enum AddOption {
  ADD_ORGANIZATION = "Add organization",
  ADD_SITE= "Add site"
}

@Component
export default class OrganizationTree extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;
  $route: Route;
  service: OrganizationsService;

  nodes: Array<OrganizationOrSiteTreeNode> = [];

  parentURI: string;

  private langUnwatcher;

  private selectedOrganization: OrganizationGetDTO;
  private selectedSite: SiteGetDTO;

  @Ref("organizationForm") readonly organizationForm!: ModalForm<OrganizationForm, OrganizationCreationDTO, OrganizationUpdateDTO>;
  @Ref("siteForm") readonly siteForm!: ModalForm<SiteForm, SiteCreationDTO, SiteUpdateDTO>;
  @Ref("treeView") readonly treeView: TreeView<OrganizationOrSiteData>;

  private createOptions: Array<DropdownButtonOption> = [
    {
      label: this.$t("OrganizationTree.add-child").toString(),
      id: AddOption.ADD_ORGANIZATION,
      data: AddOption.ADD_ORGANIZATION
    },
    {
      label: this.$t("OrganizationTree.addSite").toString(),
      id: AddOption.ADD_SITE,
      data: AddOption.ADD_SITE
    }
  ];

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  private hasMultipleParents(node: OrganizationOrSiteTreeNode) {
    return node.data.parents.length > 1;
  }

  private getMultipleParentsTooltip(node: OrganizationOrSiteTreeNode) {
    if (node.data.isOrganization) {
      return this.$t("OrganizationTree.organization-multiple-tooltip");
    }
    return this.$t("OrganizationTree.site-multiple-tooltip");
  }

  private filter: any = "";

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService(
      "opensilex-core.OrganizationsService"
    );

    let query: any = this.$route.query;
    if (query.filter) {
      this.filter = decodeURIComponent(query.filter);
    }

    this.refresh();
  }

  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        if (this.selectedOrganization != null) {
          this.displayOrganizationDetail(this.selectedOrganization.uri, true);
        }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  refresh(uri?): Promise<void> {
    return Promise.all([
      this.fetchOrganizationsAsTree(),
      this.fetchSites()
    ]).then(result => {
      let tree: Array<GenericTreeOption> = result[0];
      let sites: Array<SiteGetListDTO> = result[1];

      // Keep no selection
      this.selectedOrganization = undefined;
      this.selectedSite = undefined;

      this.nodes = this.appendSitesToTree(tree, sites);

      if (uri) {
        this.selectFirstNode(node => node.data.uri === uri);
      } else {
        this.$emit("onSelect");
      }
    }).catch(this.$opensilex.errorHandler);
  }

  /**
   * Programmatically select the first node that matches the given predicate.
   *
   * @param predicate
   * @param currentNode The node to explore with its children. If not specified, all nodes will be explored.
   */
  selectFirstNode(predicate: (node: OrganizationOrSiteTreeNode) => boolean, currentNode?: OrganizationOrSiteTreeNode): OrganizationOrSiteTreeNode {
    // If no current node is given, apply the method on each root node
    if (!currentNode) {
      for (let node of this.nodes) {
        let result = this.selectFirstNode(predicate, node);
        if (result) {
          return result;
        }
      }
      return undefined;
    }

    // Test the predicate against the current node
    if (predicate(currentNode)) {
      // Select it
      currentNode.isSelected = true;
      this.displayNodesDetail(currentNode);
      return currentNode;
    }

    // Test the predicate against the children
    if (currentNode.children) {
      for (let node of currentNode.children) {
        let result = this.selectFirstNode(predicate, node);
        if (result) {
          return result;
        }
      }
    }
    return undefined;
  }

  private async fetchOrganizationsAsTree(): Promise<Array<GenericTreeOption>> {
    try {
      let orgHttp: HttpResponse<OpenSilexResponse<Array<ResourceDagDTO>>> = await this.service.searchOrganizations(this.filter);

      if (this.organizationForm && this.organizationForm.getFormRef()) {
        if (this.filter == "") {
          this.organizationForm
              .getFormRef()
              .setParentOrganizations(orgHttp.response.result);
        } else {
          this.organizationForm.getFormRef().init();
        }
      }

      return this.$opensilex.buildTreeFromDag(orgHttp.response.result, {});
    } catch (e) {
      this.$opensilex.errorHandler(e);
      throw e;
    }
  }

  private async fetchSites(): Promise<Array<SiteGetListDTO>> {
    return (await this.service.searchSites(this.filter)).response.result;
  }

  private appendSitesToTree(tree: Array<GenericTreeOption>, sites: Array<SiteGetListDTO>): Array<OrganizationOrSiteTreeNode> {
    return this.$opensilex.mapTree<GenericTreeOption, OrganizationOrSiteTreeNode>(tree, (node, mappedChildren) => {
      let orgNode: OrganizationOrSiteTreeNode = {
        ...node,
        children: mappedChildren,
        data: {
          ...node.data,
          isOrganization: true,
          isSite: false
        }
      };

      for (let site of sites) {
        for (let org of site.organizations) {
          if (org === orgNode.id) {
            if (!Array.isArray(orgNode.children)) {
              orgNode.children = [];
            }
            let siteNodeData: OrganizationOrSiteData = {
              isSite: true,
              isOrganization: false,
              uri: site.uri,
              name: site.name,
              rdf_type: site.rdf_type,
              rdf_type_name: site.rdf_type_name,
              children: [],
              parents: site.organizations
            };
            let siteNode: OrganizationOrSiteTreeNode = {
              id: site.uri,
              children: [],
              isDefaultExpanded: true,
              isExpanded: true,
              isDraggable: false,
              isDisabled: false,
              isLeaf: true,
              label: site.name,
              data: siteNodeData,
              title: site.name,
              isSelectable: true
            };
            orgNode.isLeaf = false;
            orgNode.children.push(siteNode);
          }
        }
      }

      return orgNode;
    });
  }

  public displayNodesDetail(node: OrganizationOrSiteTreeNode) {
    if (node.data.isOrganization) {
      this.displayOrganizationDetail(node.data.uri);
    } else if (node.data.isSite) {
      this.displaySiteDetail(node.data.uri);
    }
  }

  public displayOrganizationDetail(uri: string, forceRefresh?: boolean) {
    if (forceRefresh || this.selectedOrganization == null || this.selectedOrganization.uri != uri) {
      return this.service
        .getOrganization(uri)
        .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
          let detailDTO: OrganizationGetDTO = http.response.result;
          this.selectedOrganization = detailDTO;
          this.selectedSite = undefined;
          this.$emit("onSelect", detailDTO);
        });
    }
  }

  public displaySiteDetail(uri: string, forceRefresh?: boolean) {
    if (forceRefresh || this.selectedOrganization == null || this.selectedOrganization.uri != uri) {
      return this.service
        .getSite(uri)
        .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
          let siteDto = http.response.result;
          this.selectedSite = siteDto;
          this.selectedOrganization = undefined;
          this.$emit("onSelect", siteDto);
        });
    }
  }

  showOrganizationOrSiteDetail(siteOrOrg: OrganizationOrSiteData) {
    if (siteOrOrg.isOrganization) {
      this.showOrganizationDetail(siteOrOrg.uri);
    } else if (siteOrOrg.isSite) {
      this.showSiteDetail(siteOrOrg.uri);
    }
  }

  editOrganizationOrSite(siteOrOrg: OrganizationOrSiteData) {
    if (siteOrOrg.isOrganization) {
      this.editOrganization(siteOrOrg.uri);
    } else if (siteOrOrg.isSite) {
      this.editSite(siteOrOrg.uri);
    }
  }

  deleteOrganizationOrSite(siteOrOrg: OrganizationOrSiteData) {
    if (siteOrOrg.isOrganization) {
      this.deleteOrganization(siteOrOrg.uri);
    } else if (siteOrOrg.isSite) {
      this.deleteSite(siteOrOrg.uri);
    }
  }

  showOrganizationDetail(uri) {
    this.$router.push({
      path: "/organization/details/" + encodeURIComponent(uri),
    });
  }

  createOrganizationOrSite(option: DropdownButtonOption, uri?: string) {
    if (option.data === AddOption.ADD_ORGANIZATION) {
      this.createOrganization(uri);
    } else if (option.data === AddOption.ADD_SITE) {
      this.createSite(uri);
    }
  }

  createOrganization(parentURI?) {
    this.parentURI = parentURI;
    this.organizationForm.showCreateForm();
  }

  editOrganization(uri) {
    this.service
      .getOrganization(uri)
      .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
        let detailDTO: OrganizationGetDTO = http.response.result;
        this.parentURI = Array.isArray(detailDTO.parents) && detailDTO.parents.length > 0
          ? detailDTO.parents[0].uri
          : undefined;

        let editDTO: OrganizationUpdateDTO = DTOConverter.extractURIFromResourceProperties(detailDTO);
        this.organizationForm.showEditForm(editDTO);
      });
  }

  deleteOrganization(uri: string) {
    this.service
      .deleteOrganization(uri)
      .then(() => {
        this.$emit("onDelete");
        let message = this.$i18n.t("OrganizationForm.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  showSiteDetail(uri) {
    this.$router.push({
      path: "/organization/site/details/" + encodeURIComponent(uri),
    });
  }

  createSite(parentURI?: string) {
    this.parentURI = parentURI;
    this.siteForm.showCreateForm();
  }

  editSite(uri) {
    this.service
        .getSite(uri)
        .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
          let editDTO: SiteUpdateDTO = DTOConverter.extractURIFromResourceProperties(http.response.result);
          this.siteForm.showEditForm(editDTO);
        });
  }

  deleteSite(uri) {
    this.service
        .deleteSite(uri)
        .then(() => {
          this.$emit("onDelete");
          this.refresh();
        })
        .catch(this.$opensilex.errorHandler);
  }

  initOrganizationForm(form) {
    form.parents = [this.parentURI];
  }

  initSiteForm(form) {
    if (this.parentURI) {
      form.organizations = [this.parentURI];
    }
  }

  onCreate() {
    let selectedNode = this.treeView.getSelectedNode();
    this.refresh(selectedNode?.uri);
    this.$emit("onCreate");
  }

  onUpdate() {
    let selectedNode = this.treeView.getSelectedNode();
    this.refresh(selectedNode?.uri);
    this.$emit("onCreate");
  }
}
</script>

<style scoped lang="scss">
.sl-vue-tree-root {
  min-height: 100px;
  max-height: 300px;
  overflow-y: auto;
}

::v-deep .sl-vue-tree-nodes-list {
  overflow: visible;
}

.leaf-spacer {
  display: inline-block;
  width: 23px;
}
.organizationHelp{
  font-size: 1.3em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
}


@media (max-width: 768px) {
  .sl-vue-tree-root {
    min-height: auto;
  }
}

.tree-multiple-icon {
  padding-left: 8px;
  color: #3cc6ff;
}

.spaced-actions {
  margin-top: -15px;
  margin-bottom: 10px;
}

.firstButton {
  margin-right: 10px;
}

</style>

<i18n>
en:
  OrganizationTree:
    filter-placeholder: Search organizations...
    add: Add organization
    update: Update organization
    edit: Edit organization
    add-child: Add sub-organization
    addSite: Add site
    editSite: Edit site
    delete: Delete organization
    deleteSite: Delete site
    organization-component: Organizations and sites
    organization-help: "The organizations represent the hierarchy between the different sites, units, ... with a specific address and / or with dedicated teams."
    showDetail: Organization details
    showDetailSite: Site details
    organization-multiple-tooltip: "This organization has several parent organizations"
    site-multiple-tooltip: "This site hosts several organizations"
fr:
  OrganizationTree:
    filter-placeholder: Rechercher des organisations...
    add: Ajouter une organisation
    update: Modifier l'organisation
    edit: Editer l'organisation
    add-child: Ajouter une sous-organisation
    addSite: Ajouter un site
    editSite: Editer le site
    delete: Supprimer l'organisation
    deleteSite: Supprimer le site
    organization-component: Organisations et sites
    organization-help: "Les organisations représentent la hiérarchie entre les différents sites, unités, ... disposant d'une adresse particulière et/ou avec des équipes dédiées."
    showDetail: Détail de l'organisation
    showDetailSite: Détail du site
    organization-multiple-tooltip: "Cette organisation a plusieurs organisations parentes"
    site-multiple-tooltip: "Ce site héberge plusieurs organisations"


</i18n>
