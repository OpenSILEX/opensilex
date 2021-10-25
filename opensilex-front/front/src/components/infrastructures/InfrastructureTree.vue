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

    <opensilex-TreeView
        :nodes.sync="nodes"
        @select="displayNodesDetail"
        :multipleElementsTooltip="$t('InfrastructureTree.multiple-parents-tooltip')"
    >
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
      :initForm="setParents"
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

  public nodes = [];

  parentURI;

  private langUnwatcher;

  private selected: InfrastructureGetDTO;

  @Ref("infrastructureForm") readonly infrastructureForm!: any;

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
      .searchInfrastructures(this.filter)
      .then((http: HttpResponse<OpenSilexResponse<Array<InfrastructureGetDTO>>>) => {
        //@todo dag
        if (this.infrastructureForm && this.infrastructureForm.getFormRef()) {
          if (this.filter == "") {
            this.infrastructureForm
              .getFormRef()
              .setParentInfrastructures(http.response.result);
          } else {
            this.infrastructureForm.getFormRef().init();
          }
        }

        let nodes = this.$opensilex.buildTreeFromDag(http.response.result, {});

        if (nodes.length > 0) {
          nodes[0].isSelected = true;

          if (!uri) {
            this.displayNodeDetail(nodes[0].data.uri, true);
          }
        }

        if (uri) {
          this.displayNodeDetail(uri, true);
        }

        this.nodes = nodes;
      })
      .catch(this.$opensilex.errorHandler);
  }

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

  showDetail(uri) {
    this.$router.push({
      path: "/infrastructure/details/" + encodeURIComponent(uri),
    });
  }

  createInfrastructure(parentURI?) {
    this.parentURI = parentURI;
    this.infrastructureForm.showCreateForm();
  }

  editInfrastructure(uri) {
    this.service
      .getInfrastructure(uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        //@todo dag
        this.parentURI = detailDTO.parents[0];

        let editDTO: InfrastructureUpdateDTO = {
          ...detailDTO,
          uri: detailDTO.uri,
          groups: detailDTO.groups.map(group => group.uri),
          facilities: detailDTO.facilities.map(facility => facility.uri)
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

  setParents(form) {
    form.parents = [this.parentURI];
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
    multiple-parents-tooltip: "This organization has several parent organizations"
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
    multiple-parents-tooltip: "Cette organisation a plusieurs organizations parentes"
</i18n>
