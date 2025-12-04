<template>
  <div class="container-fluid">
    <opensilex-PageActions
      v-if="
      user.hasCredential(
      credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)
    ">
      <opensilex-HelpButton
        @click="helpModal.show()"
        label="component.common.help-button"
        class="helpButton"
      ></opensilex-HelpButton> 
      <opensilex-CreateButton
        @click="goToGeneticResourceCreate"
        label="GeneticResourceView.add"
        class="createButton"
      ></opensilex-CreateButton> 
  </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-GeneticResourceList
          ref="geneticResourceList"
          @onEdit="editGeneticResource"
          @onDelete="deleteGeneticResource"
        ></opensilex-GeneticResourceList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)"
      ref="geneticResourceForm"
      component="opensilex-GeneticResourceForm"
      createTitle="GeneticResourceView.add"
      editTitle="GeneticResourceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onCreate="geneticResourceList.refresh()"
      @onUpdate="geneticResourceList.updateSelectedGeneticResource()"
    ></opensilex-ModalForm>
    <b-modal ref="helpModal" size="xl" hide-header hide-footer>
      <opensilex-GeneticResourceHelp @hideBtnIsClicked="hide()"></opensilex-GeneticResourceHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { GeneticResourceService, GeneticResourceCreationDTO, GeneticResourceUpdateDTO, GeneticResourceGetSingleDTO, GeneticResourceGetAllDTO } from "opensilex-core/index"
import VueRouter from "vue-router";
import GeneticResourceForm from "./GeneticResourceForm.vue";
import ModalForm from '../common/forms/ModalForm.vue';
import Oeso from "../../ontologies/Oeso";

@Component
export default class GeneticResourceView extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  service: GeneticResourceService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  
  @Ref("modalRef") readonly modalRef!: any;
  @Ref("geneticResourceForm") readonly geneticResourceForm!: ModalForm<GeneticResourceForm, GeneticResourceCreationDTO, GeneticResourceUpdateDTO>;
  @Ref("geneticResourceList") readonly geneticResourceList!: any;
  @Ref("geneticResourceDetails") readonly geneticResourceDetails!: any;
  @Ref("geneticResourceAttributesForm") readonly geneticResourceAttributesForm!: any;
  @Ref("helpModal") readonly helpModal!: any;

  created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.GeneticResourceService");
  }

  goToGeneticResourceCreate(){    
    this.$store.commit("storeReturnPage", this.$router);
    this.$router.push({ path: '/geneticResource/create' });
  }  

  async editGeneticResource(uri: string) {
    this.service
      .getGeneticResource(uri)
      .then((http: HttpResponse<OpenSilexResponse<GeneticResourceGetSingleDTO>>) => {
        let form: GeneticResourceForm = this.geneticResourceForm.getFormRef();
        form.readAttributes(http.response.result.metadata);
        //Take the has_parent_geneticResource properties from the GeneticResourceGetSingleDTO and put the correct uris into the relations attribute of GeneticResourceUpdateDTO
        //The labels will be loaded with another service call inside the GeneticResourceForm component
        let resultWithRelationsField : GeneticResourceUpdateDTO = GeneticResourceForm.readDuplicatableRelations(http.response.result);
        this.geneticResourceForm.showEditForm(resultWithRelationsField);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteGeneticResource(uri: string) {
    this.service
      .deleteGeneticResource(uri)
      .then(() => {
        this.geneticResourceList.refresh();
        let message =
          this.$i18n.t("GeneticResourceView.title") +
          " " +
          uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
  }

  hide() {
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
  GeneticResourceView:
    title: GeneticResource 
    description: Manage Genetic Resources Information
    add: Add genetic resource
    update: Update Genetic Resource
    delete: Delete Genetic Resource
    groupGeneticResource: Genetic Resource Group
fr:
  GeneticResourceView:
    title: Ressources Génétiques 
    description: Gérer les informations du matériel génétique
    add: Ajouter des ressources génétiques
    update: éditer des ressources génétiques
    delete: supprimer des ressources génétiques
    groupGeneticResource: Groupe de Ressources Génétiques
</i18n>

