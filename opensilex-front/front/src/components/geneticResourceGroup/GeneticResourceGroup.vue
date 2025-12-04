<template>
  <div class="container-fluid">
    <div>
      <opensilex-PageActions>
        <opensilex-HelpButton
            label="component.common.help-button"
            class="helpButton"
            @click="helpModal.show()"
        ></opensilex-HelpButton>

        <b-modal ref="helpModal" size="xl" hide-header hide-footer>
          <opensilex-GeneticResourceGroupHelp @hideButtonIsClicked="hideHelpModal()"></opensilex-GeneticResourceGroupHelp>
        </b-modal>

        <opensilex-CreateButton
            label="GeneticResourceGroupView.add"
            class="createButton"
            v-show="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)"
            @click="showCreateForm"
        ></opensilex-CreateButton>

      </opensilex-PageActions>

      <opensilex-ModalForm
          v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)"
          ref="groupGeneticResourceForm"
          modalSize="lg"
          :tutorial="false"
          :createAction="createGeneticResourceGroup"
          :updateAction="updateGeneticResourceGroup"
          component="opensilex-GroupGeneticResourceForm"
          createTitle="GroupGeneticResourceForm.add"
          editTitle="GroupGeneticResourceForm.edit"
      ></opensilex-ModalForm>
        <div :class="{ row: !searchFiltersToggle && selected.uri }">
        <!-- Element list in column if an element is selected-->
        <div
            :class="(!this.searchFiltersToggle && selected && selected.uri) ? 'col-md-6' : null"
        >
          <opensilex-GeneticResourceGroupList
              ref="geneticResourceGroupList"
              :searchFiltersToggle="this.searchFiltersToggle"
              @onEdit="showEditForm"
              @onDelete="deleteGeneticResourceGroup"
              @toggleFilters="searchFiltersToggle=!searchFiltersToggle"
              @select="displayNodeDetail"
          ></opensilex-GeneticResourceGroupList>
        </div>
        <!-- Element details page if filters not open and an element is selected-->

        <Transition>
          <div
              :class="(!this.searchFiltersToggle && selected && selected.uri) ? 'col-md-6' : null"
              v-if="!this.searchFiltersToggle && selected && selected.uri"
          >
            <opensilex-GeneticResourceGroupStructureDetails
                :selected.sync="selected"
            ></opensilex-GeneticResourceGroupStructureDetails>
            <opensilex-GeneticResourceGroupContentList
                ref="geneticResourceGroupContentList"
                :selected.sync="selected.uri"
                :relationsFields="relationsFields"
            ></opensilex-GeneticResourceGroupContentList>

            <opensilex-DocumentTabList
                v-if="selected && selected.uri"
                :selected="selected"
                :uri="[selected.uri]"
                :search=false
            ></opensilex-DocumentTabList>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {GeneticResourceGroupCreationDTO, GeneticResourceGroupGetDTO, GeneticResourceGroupUpdateDTO, GeneticResourceService} from 'opensilex-core/index';
import ModalForm from "../common/forms/ModalForm.vue";
import GeneticResourceGroupList from "./GeneticResourceGroupList.vue";
import GeneticResourceGroupContentList from './GeneticResourceGroupContentList.vue';
import GroupGeneticResourceForm from './GroupGeneticResourceForm.vue';
import {OpenSilexStore} from "../../models/Store";
import {Route} from "vue-router";
import {GeneticResourceGroupGetWithDetailsDTO} from "opensilex-core/model/geneticResourceGroupGetWithDetailsDTO";
import {SelectableItem} from "../common/forms/FormSelector.vue";
import {GeneticResourceGetAllDTO} from "opensilex-core/model/geneticResourceGetAllDTO";

interface UpdateDtoAndGeneticResourceModels {
    updateDto: GeneticResourceGroupUpdateDTO,
    geneticResourceModels: Array<GeneticResourceGetAllDTO>
}

@Component
export default class GeneticResourceGroup extends Vue {
  private nameFilter: string = "";
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;
  $route: Route;
  service: GeneticResourceService;
  $i18n: any;
  public nodes = [];
    selected: GeneticResourceGroupGetDTO = {
      uri: undefined,
      name: undefined,
      description: undefined
    }

    relationsFields: any[] = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true,
    }
  ];

  @Ref("groupGeneticResourceForm") readonly groupGeneticResourceForm!: ModalForm<GroupGeneticResourceForm, GeneticResourceGroupCreationDTO, GeneticResourceGroupUpdateDTO>

  @Ref("geneticResourceGroupList") readonly geneticResourceGroupList!: GeneticResourceGroupList;

  @Ref("geneticResourceGroupContentList") readonly geneticResourceGroupContentList!: GeneticResourceGroupContentList;

  @Ref("helpModal") readonly helpModal!: any;

  searchFiltersToggle: boolean = true;

  @Prop()
  geneticResourceList: Array<SelectableItem>;

  created() {
    this.service = this.$opensilex.getService("opensilex-core.GeneticResourceService");
    //Check if a group was preselected before navigating to the page by looking at route
    let preselected = this.$route.query.selected;
    if(typeof preselected === "string"){
      this.service.getGeneticResourceGroup(preselected).then((http: HttpResponse<OpenSilexResponse>) => {
        this.$nextTick(() => {
          this.selected = http.response.result;
          this.searchFiltersToggle = false;
        });
      });
    }
  }

  showCreateForm() {
    this.$nextTick(() => {
      this.groupGeneticResourceForm.showCreateForm();
    })
  }

    /**
     *
     * @param dto a GeneticResourceGroupGetWithDetailsDTO object, geneticResource_list contains GeneticResourceGetAllDto's
     *
     * creates a GeneticResourceGroupUpdateDto format, where geneticResource_list is a list of uris.
     * And extracts the geneticResource models with uri's made long.
     */
  convertUpdateDtoAndExtractGeneticResources(dto: GeneticResourceGroupGetWithDetailsDTO) : UpdateDtoAndGeneticResourceModels {
    let copy = JSON.parse(JSON.stringify(dto));
        let geneticResource_uris : Array<String> = [];
        let new_geneticResource_list : Array<GeneticResourceGetAllDTO>= [];
    if (copy.geneticResource_list) {
      for(let geneticResource of copy.geneticResource_list){
        let geneticResourceUri = this.$opensilex.getLongUri(geneticResource.uri);
        geneticResource_uris.push(geneticResourceUri);
        geneticResource.uri = geneticResourceUri;
        new_geneticResource_list.push(geneticResource);
      }
      copy.geneticResource_list = geneticResource_uris;
    }
    return {updateDto : (copy as GeneticResourceGroupUpdateDTO), geneticResourceModels : new_geneticResource_list};
  }

  showEditForm(uri) {
      this.service.getGeneticResourceGroupWithGeneticResources(uri).then((http: HttpResponse<OpenSilexResponse<GeneticResourceGroupGetWithDetailsDTO>>) => {
      this.$nextTick(() => {
        let updateDtoAndExtractedGeneticResources = this.convertUpdateDtoAndExtractGeneticResources(http.response.result);
        this.groupGeneticResourceForm.setSelectorsToFirstTimeOpenAndSetLabels(updateDtoAndExtractedGeneticResources.geneticResourceModels);
        this.groupGeneticResourceForm.showEditForm(updateDtoAndExtractedGeneticResources.updateDto);
      });
    });
  }

  createGeneticResourceGroup(form) {
    return this.service.createGeneticResourceGroup(form)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let message = this.$t(form.name) + this.$i18n.t("component.common.success.creation-success-message");
          this.$opensilex.showSuccessToast(message);
          this.geneticResourceGroupList.refresh();
        })
        .catch(error => {
            this.$opensilex.errorHandler(error);
        });
  }

  updateGeneticResourceGroup(form) {
    return this.service.updateGeneticResourceGroup(form)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let message = this.$t(form.name) + this.$i18n.t("component.common.success.update-success-message");
          this.$opensilex.showSuccessToast(message);
          this.geneticResourceGroupList.refresh();
          this.afterUpdateGeneticResourceGroup(form.uri);
          this.geneticResourceGroupContentList.refresh();
        })
        .catch(this.$opensilex.errorHandler);
  }

  deleteGeneticResourceGroup(uri) {
    this.service.deleteGeneticResourceGroup(uri).then((http: HttpResponse<OpenSilexResponse>) => {
      let message = this.$i18n.t(http.response.result) + " " + this.$i18n.t("component.common.success.delete-success-message");
      this.$opensilex.showSuccessToast(message);
      if (this.nodes.length > 0) {
        this.selected = this.nodes[0].data;
      } else {
        this.selected = undefined;
      }
      this.geneticResourceGroupList.refresh();
    });
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get user() {
    return this.$store.state.user;
  }

  afterUpdateGeneticResourceGroup(uri) {
    this.service.getGeneticResourceGroup(uri)
    .then((http) => {
      this.selected = http.response.result;
    })
    .catch(this.$opensilex.errorHandler);
  }

  public displayNodeDetail(data: any) {
    this.searchFiltersToggle = false;
    this.selected = data;
    this.$opensilex.updateURLParameter("selected", this.selected.uri);
    this.geneticResourceGroupContentList.refresh();
  }

  hideHelpModal() {
    this.helpModal.hide();
  }

}
</script>

<style scoped lang="scss">
.createButton, .helpButton {
  margin-bottom: 10px;
  margin-right: 5px;
  margin-top: 5px;
}

.helpButton {
  margin-left: -5px;
  color: #00A28C;
  font-size: 1.2em;
  border: none
}

.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}
</style>

<i18n>

en:
  GeneticResourceGroupView:
    title: Genetic Resource Group
    type: Genetic Resource
    add: Add group
    update: Update group
    delete: Delete group
    no-geneticResource-provided: No genetic resource provided
  GeneticResourceGroupList:
    filter-placeholder: Search objects by name
  GeneticResourceGroupStructureList:
    geneticResource: "(0 genetic resource) | (1 genetic resource) | ({count} genetic resource)"
fr:
  GeneticResourceGroupView:
    title: Groupe de Ressources Génétiques
    type: Ressources Génétiques
    add: Ajouter un groupe
    update: éditer groupe
    delete: supprimer groupe
    no-geneticResource-provided: Aucune ressource génétique associée
  GeneticResourceGroupList:
    filter-placeholder: Rechercher des élements par nom
  GeneticResourceGroupStructureList:
    geneticResource: "(0 ressources génétiques) | (1 ressource génétique) | ({count} ressources génétiques)"
</i18n>