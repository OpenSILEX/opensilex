<template>
  <div class="container-fluid">
    <div>
      <opensilex-PageActions>
        <opensilex-HelpButton
            label="component.common.help-button"
            class="helpButton"
        ></opensilex-HelpButton>
        <opensilex-CreateButton
            label="GermplasmGroupView.add"
            class="createButton"
            v-show="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
            @click="showCreateForm"
        ></opensilex-CreateButton>
      </opensilex-PageActions>
      <opensilex-ModalForm
          v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
          ref="groupGermplasmForm"
          modalSize="lg"
          :tutorial="false"
          :createAction="createGermplasmGroup"
          :updateAction="updateGermplasmGroup"
          component="opensilex-GroupGermplasmForm"
          createTitle="GroupGermplasmForm.add"
          editTitle="GroupGermplasmForm.edit"
      ></opensilex-ModalForm>
        <div :class="{ row: !searchFiltersToggle && selected.uri }">
        <!-- Element list in column if an element is selected-->
        <div
            :class="(!this.searchFiltersToggle && selected && selected.uri) ? 'col-md-6' : null"
        >
          <opensilex-GermplasmGroupList
              ref="germplasmGroupList"
              :searchFiltersToggle="this.searchFiltersToggle"
              @onEdit="showEditForm"
              @onDelete="deleteGermplasmGroup"
              @toggleFilters="searchFiltersToggle=!searchFiltersToggle"
              @select="displayNodeDetail"
          ></opensilex-GermplasmGroupList>
        </div>
        <!-- Element details page if filters not open and an element is selected-->

        <Transition>
          <div
              :class="(!this.searchFiltersToggle && selected && selected.uri) ? 'col-md-6' : null"
              v-if="!this.searchFiltersToggle && selected && selected.uri"
          >
            <opensilex-GermplasmGroupStructureDetails
                :selected.sync="selected"
            ></opensilex-GermplasmGroupStructureDetails>
            <opensilex-GermplasmGroupContentList
                ref="germplasmGroupContentList"
                :selected.sync="selected.uri"
                :relationsFields="relationsFields"
            ></opensilex-GermplasmGroupContentList>

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
import {GermplasmGroupCreationDTO, GermplasmGroupGetDTO, GermplasmGroupUpdateDTO, GermplasmService} from 'opensilex-core/index';
import ModalForm from "../common/forms/ModalForm.vue";
import GermplasmGroupList from "./GermplasmGroupList.vue";
import GermplasmGroupContentList from './GermplasmGroupContentList.vue';
import GroupGermplasmForm from './GroupGermplasmForm.vue';
import {OpenSilexStore} from "../../models/Store";
import {Route} from "vue-router";
import {GermplasmGroupGetWithDetailsDTO} from "opensilex-core/model/germplasmGroupGetWithDetailsDTO";
import {SelectableItem} from "../common/forms/SelectForm.vue";
import {GermplasmGetAllDTO} from "opensilex-core/model/germplasmGetAllDTO";

interface UpdateDtoAndGermplasmModels {
    updateDto: GermplasmGroupUpdateDTO,
    germplasmModels: Array<GermplasmGetAllDTO>
}

@Component
export default class GermplasmGroup extends Vue {
  private nameFilter: string = "";
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;
  $route: Route;
  service: GermplasmService;
  $i18n: any;
  public nodes = [];
    selected: GermplasmGroupGetDTO = {
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

  @Ref("groupGermplasmForm") readonly groupGermplasmForm!: ModalForm<GroupGermplasmForm, GermplasmGroupCreationDTO, GermplasmGroupUpdateDTO>

  @Ref("germplasmGroupList") readonly germplasmGroupList!: GermplasmGroupList;

  @Ref("germplasmGroupContentList") readonly germplasmGroupContentList!: GermplasmGroupContentList;

  searchFiltersToggle: boolean = true;

  @Prop()
  germplasmList: Array<SelectableItem>;

  created() {
    this.service = this.$opensilex.getService("opensilex-core.GermplasmService");
    //Check if a group was preselected before navigating to the page by looking at route
    let preselected = this.$route.query.selected;
    if(typeof preselected === "string"){
      this.service.getGermplasmGroup(preselected).then((http: HttpResponse<OpenSilexResponse>) => {
        this.$nextTick(() => {
          this.selected = http.response.result;
          this.searchFiltersToggle = false;
        });
      });
    }
  }

  showCreateForm() {
    this.$nextTick(() => {
      this.groupGermplasmForm.showCreateForm();
    })
  }

    /**
     *
     * @param dto a GermplasmGroupGetWithDetailsDTO object, germplasm_list contains GermplasmGetAllDto's
     *
     * creates a GermplasmGroupUpdateDto format, where germplasm_list is a list of uris.
     * And extracts the germplasm models with uri's made long.
     */
  convertUpdateDtoAndExtractGermplasms(dto: GermplasmGroupGetWithDetailsDTO) : UpdateDtoAndGermplasmModels {
    let copy = JSON.parse(JSON.stringify(dto));
        let germplasm_uris : Array<String> = [];
        let new_germplasm_list : Array<GermplasmGetAllDTO>= [];
    if (copy.germplasm_list) {
      for(let germplasm of copy.germplasm_list){
        let germplasmUri = this.$opensilex.getLongUri(germplasm.uri);
        germplasm_uris.push(germplasmUri);
        germplasm.uri = germplasmUri;
        new_germplasm_list.push(germplasm);
      }
      copy.germplasm_list = germplasm_uris;
    }
    return {updateDto : (copy as GermplasmGroupUpdateDTO), germplasmModels : new_germplasm_list};
  }

  showEditForm(uri) {
      this.service.getGermplasmGroupWithGermplasms(uri).then((http: HttpResponse<OpenSilexResponse<GermplasmGroupGetWithDetailsDTO>>) => {
      this.$nextTick(() => {
          let updateDtoAndExtractedGermplasms = this.convertUpdateDtoAndExtractGermplasms(http.response.result);
          this.groupGermplasmForm.setSelectorsToFirstTimeOpenAndSetLabels(updateDtoAndExtractedGermplasms.germplasmModels);
        this.groupGermplasmForm.showEditForm(updateDtoAndExtractedGermplasms.updateDto);
      });
    });
  }

  createGermplasmGroup(form) {
    return this.service.createGermplasmGroup(form)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let message = this.$t(form.name) + this.$i18n.t("component.common.success.creation-success-message");
          this.$opensilex.showSuccessToast(message);
          this.germplasmGroupList.refresh();
        })
        .catch(error => {
            this.$opensilex.errorHandler(error);
        });
  }

  updateGermplasmGroup(form) {
    return this.service.updateGermplasmGroup(form)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let message = this.$t(form.name) + this.$i18n.t("component.common.success.update-success-message");
          this.$opensilex.showSuccessToast(message);
          this.germplasmGroupList.refresh();
          this.afterUpdateGermplasmGroup(form.uri);
        })
        .catch(this.$opensilex.errorHandler);
  }

  deleteGermplasmGroup(uri) {
    this.service.deleteGermplasmGroup(uri).then((http: HttpResponse<OpenSilexResponse>) => {
      let message = this.$i18n.t(http.response.result) + " " + this.$i18n.t("component.common.success.delete-success-message");
      this.$opensilex.showSuccessToast(message);
      if (this.nodes.length > 0) {
        this.selected = this.nodes[0].data;
      } else {
        this.selected = undefined;
      }
      this.germplasmGroupList.refresh();
    });
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get user() {
    return this.$store.state.user;
  }

  afterUpdateGermplasmGroup(uri) {
    this.service.getGermplasmGroup(uri)
    .then((http) => {
      this.selected = http.response.result;
    })
    .catch(this.$opensilex.errorHandler);
  }

  public displayNodeDetail(data: any) {
    this.searchFiltersToggle = false;
    this.selected = data;
    this.$opensilex.updateURLParameter("selected", this.selected.uri);
    this.germplasmGroupContentList.refresh();
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
  GermplasmGroupView:
    title: Germplasm Group
    type: Germplasm
    add: Add group
    update: Update group
    delete: Delete group
    no-germplasm-provided: No germplasm provided
  GermplasmGroupList:
    filter-placeholder: Search objects by name
  GermplasmGroupStructureList:
    germplasm: "(0 germplasm) | (1 germplasm) | ({count} germplasm)"
fr:
  GermplasmGroupView:
    title: Groupe de Ressources Génétiques
    type: Ressources Génétiques
    add: Ajouter un groupe
    update: éditer groupe
    delete: supprimer groupe
    no-germplasm-provided: Aucune ressource génétique associée
  GermplasmGroupList:
    filter-placeholder: Rechercher des élements par nom
  GermplasmGroupStructureList:
    germplasm: "(0 ressources génétiques) | (1 ressource génétique) | ({count} ressources génétiques)"
</i18n>