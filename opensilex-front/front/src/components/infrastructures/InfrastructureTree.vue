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
        <div class="spaced-actions">
          <opensilex-CreateButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
              )
            "
            @click="createOrganization()"
            label="InfrastructureTree.add"
          ></opensilex-CreateButton>
          <opensilex-CreateButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
              )
            "
            @click="createSite()"
            label="InfrastructureTree.addSite"
          ></opensilex-CreateButton>
        </div>
      </div>
    </template>
    <!-- Card body -->
    <!-- Infrastructure filter -->
    <opensilex-StringFilter
      :filter.sync="filter"
      @update="updateFilter()"
      placeholder="InfrastructureTree.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TreeView
        :nodes.sync="nodes"
        @select="displayNodesDetail"
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
          label="InfrastructureTree.showDetail"
          :small="true"
        ></opensilex-DetailButton>
        <opensilex-EditButton
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
            )
          "
          @click="editOrganizationOrSite(node.data)"
          label="InfrastructureTree.edit"
          :small="true"
        ></opensilex-EditButton>
        <opensilex-Dropdown
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
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
            user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID)
          "
          @click="deleteOrganizationOrSite(node.data)"
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
      :initForm="initOrganizationForm"
    ></opensilex-ModalForm>
    <opensilex-ModalForm
        v-if="
          user.hasCredential(
            credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
          )
        "
        ref="siteForm"
        component="opensilex-SiteForm"
        createTitle="InfrastructureTree.addSite"
        editTitle="InfrastructureTree.editSite"
        icon="ik#ik-globe"
        @onCreate="refresh($event ? $event.uri : undefined)"
        @onUpdate="refresh($event ? $event.uri : undefined)"
        :initForm="initSiteForm"
        :doNotHideOnError="true"
    ></opensilex-ModalForm>
  </b-card>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {InfrastructureGetDTO} from "opensilex-core/index";
import {InfrastructureUpdateDTO} from "opensilex-core/model/infrastructureUpdateDTO";
import OpenSilexVuePlugin, {GenericTreeOption, TreeOption} from "../../models/OpenSilexVuePlugin";
import {SiteGetDTO} from "opensilex-core/model/siteGetDTO";
import ModalForm from "../common/forms/ModalForm.vue";
import {SiteUpdateDTO} from "opensilex-core/model/siteUpdateDTO";
import {DropdownButtonOption} from "../common/dropdown/Dropdown.vue";
import {ResourceDagDTO} from "opensilex-core/model/resourceDagDTO";
import Oeso from "../../ontologies/Oeso";
import {OrganizationsService} from "opensilex-core/api/organizations.service";

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
export default class InfrastructureTree extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $route: any;
  service: OrganizationsService;

  nodes: Array<OrganizationOrSiteTreeNode> = [];

  parentURI: string;

  private langUnwatcher;

  private selectedOrganization: InfrastructureGetDTO;
  private selectedSite: SiteGetDTO;

  @Ref("infrastructureForm") readonly infrastructureForm!: ModalForm;
  @Ref("siteForm") readonly siteForm!: ModalForm;

  private createOptions: Array<DropdownButtonOption> = [
    {
      label: this.$t("InfrastructureTree.add-child").toString(),
      id: AddOption.ADD_ORGANIZATION,
      data: AddOption.ADD_ORGANIZATION
    },
    {
      label: this.$t("InfrastructureTree.addSite").toString(),
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
      return this.$t("InfrastructureTree.organization-multiple-tooltip");
    }
    return this.$t("InfrastructureTree.site-multiple-tooltip");
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

  refresh(uri?) {
    Promise.all([
      this.fetchOrganizationsAsTree(),
      this.fetchSites()
    ]).then(result => {
      let tree: Array<GenericTreeOption> = result[0];
      let sites: Array<SiteGetDTO> = result[1];

      let mappedNodes = this.appendSitesToTree(tree, sites);

      if (mappedNodes.length > 0) {
        mappedNodes[0].isSelected = true;

        if (!uri) {
          if (mappedNodes[0].data.isOrganization) {
            this.displayOrganizationDetail(mappedNodes[0].data.uri, true);
          } else {
            this.displaySiteDetail(mappedNodes[0].data.uri, true);
          }
        }
      }

      this.nodes = mappedNodes;
    }).catch(this.$opensilex.errorHandler);
  }

  private async fetchOrganizationsAsTree(): Promise<Array<GenericTreeOption>> {
    try {
      let orgHttp: HttpResponse<OpenSilexResponse<Array<ResourceDagDTO>>> = await this.service.searchInfrastructures(this.filter);

      if (this.infrastructureForm && this.infrastructureForm.getFormRef()) {
        if (this.filter == "") {
          this.infrastructureForm
              .getFormRef()
              .setParentInfrastructures(orgHttp.response.result);
        } else {
          this.infrastructureForm.getFormRef().init();
        }
      }

      return this.$opensilex.buildTreeFromDag(orgHttp.response.result, {});
    } catch (e) {
      this.$opensilex.errorHandler(e);
      throw e;
    }
  }

  private async fetchSites(): Promise<Array<SiteGetDTO>> {
    return (await this.service.searchSites(this.filter)).response.result;
  }

  private appendSitesToTree(tree: Array<GenericTreeOption>, sites: Array<SiteGetDTO>): Array<OrganizationOrSiteTreeNode> {
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
          if (org.uri === orgNode.id) {
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
              parents: site.organizations.map(org => org.uri)
            };
            let siteNode: OrganizationOrSiteTreeNode = {
              id: site.uri,
              children: [],
              isDefaultExpanded: true,
              isExpanded: true,
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
        .getInfrastructure(uri)
        .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
          let detailDTO: InfrastructureGetDTO = http.response.result;
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
      path: "/infrastructure/details/" + encodeURIComponent(uri),
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
    this.infrastructureForm.showCreateForm();
  }

  editOrganization(uri) {
    this.service
      .getInfrastructure(uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.parentURI = Array.isArray(detailDTO.parents) && detailDTO.parents.length > 0
          ? detailDTO.parents[0].uri
          : undefined;

        let editDTO: InfrastructureUpdateDTO = {
          ...detailDTO,
          uri: detailDTO.uri,
          groups: detailDTO.groups.map(group => group.uri),
          facilities: detailDTO.facilities.map(facility => facility.uri),
          parents: detailDTO.parents.map(parent => parent.uri)
        };
        this.infrastructureForm.showEditForm(editDTO);
      });
  }

  deleteOrganization(uri: string) {
    this.service
      .deleteInfrastructure(uri)
      .then(() => {
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  showSiteDetail(uri) {
    this.$router.push({
      path: "/infrastructure/site/details/" + encodeURIComponent(uri),
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
          let detailDTO: SiteGetDTO = http.response.result;

          let editDTO: SiteUpdateDTO = {
            ...detailDTO,
            uri: detailDTO.uri,
            groups: detailDTO.groups.map(group => group.uri),
            facilities: detailDTO.facilities.map(facility => facility.uri),
            organizations: detailDTO.organizations.map(org => org.uri)
          };
          this.siteForm.showEditForm(editDTO);
        });
  }

  deleteSite(uri) {
    this.service
        .deleteSite(uri)
        .then(() => {
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
  button {
    margin-left: 10px;
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
    addSite: Add site
    editSite: Edit site
    delete: Delete organization
    infrastructure-component: Organizations and sites
    infrastructure-help: "The organizations represent the hierarchy between the different sites, units, ... with a specific address and / or with dedicated teams."
    showDetail: Organization details
    organization-multiple-tooltip: "This organization has several parent organizations"
    site-multiple-tooltip: "This site hosts several organizations"
fr:
  InfrastructureTree:
    filter-placeholder: Rechercher des organisations...
    add: Ajouter une organisation
    update: Modifier l'organisation
    edit: Editer l'organisation
    add-child: Ajouter une sous-organisation
    addSite: Ajouter un site
    editSite: Editer le site
    delete: Supprimer l'organisation
    infrastructure-component: Organisations et sites
    infrastructure-help: "Les organisations représentent la hiérarchie entre les différents sites, unités, ... disposant d'une adresse particulière et/ou avec des équipes dédiées."
    showDetail: Détail de l'organisation
    organization-multiple-tooltip: "Cette organisation a plusieurs organisations parentes"
    site-multiple-tooltip: "Ce site héberge plusieurs organisations"


</i18n>
