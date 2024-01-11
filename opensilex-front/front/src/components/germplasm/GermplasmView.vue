<template>
  <div class="container-fluid">
    <opensilex-PageActions
      v-if="
      user.hasCredential(
      credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)
    ">
      <opensilex-HelpButton
        @click="helpModal.show()"
        label="component.common.help-button"
        class="helpButton"
      ></opensilex-HelpButton> 
      <opensilex-CreateButton
        @click="goToGermplasmCreate"
        label="GermplasmView.add"
        class="createButton"
      ></opensilex-CreateButton> 
  </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-GermplasmList
          ref="germplasmList"
          @onEdit="editGermplasm"
          @onDelete="deleteGermplasm"
        ></opensilex-GermplasmList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
      ref="germplasmForm"
      component="opensilex-GermplasmForm"
      createTitle="GermplasmView.add"
      editTitle="GermplasmView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onCreate="germplasmList.refresh()"
      @onUpdate="germplasmList.updateSelectedGermplasm()"
    ></opensilex-ModalForm>
    <b-modal ref="helpModal" size="xl" hide-header hide-footer>
      <opensilex-GermplasmHelp @hideBtnIsClicked="hide()"></opensilex-GermplasmHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { GermplasmService, GermplasmCreationDTO, GermplasmUpdateDTO, GermplasmGetSingleDTO, GermplasmGetAllDTO } from "opensilex-core/index"
import VueRouter from "vue-router";
import GermplasmForm from "./GermplasmForm.vue";
import ModalForm from '../common/forms/ModalForm.vue';
import Oeso from "../../ontologies/Oeso";

@Component
export default class GermplasmView extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  service: GermplasmService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  
  @Ref("modalRef") readonly modalRef!: any;
  @Ref("germplasmForm") readonly germplasmForm!: ModalForm<GermplasmForm, GermplasmCreationDTO, GermplasmUpdateDTO>;
  @Ref("germplasmList") readonly germplasmList!: any;
  @Ref("germplasmDetails") readonly germplasmDetails!: any;
  @Ref("germplasmAttributesForm") readonly germplasmAttributesForm!: any;
  @Ref("helpModal") readonly helpModal!: any;

  created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
  }

  goToGermplasmCreate(){    
    this.$store.commit("storeReturnPage", this.$router);
    this.$router.push({ path: '/germplasm/create' });
  }  

  async editGermplasm(uri: string) {
    this.service
      .getGermplasm(uri)
      .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
        let form: GermplasmForm = this.germplasmForm.getFormRef();
        form.readAttributes(http.response.result.metadata);
        //Take the has_parent_germplasm properties from the GermplasmGetSingleDTO and put the correct uris into the relations attribute of GermplasmUpdateDTO
        //The labels will be loaded with another service call inside the GermplasmForm component
        let resultWithRelationsField : GermplasmUpdateDTO = GermplasmForm.readDuplicatableRelations(http.response.result);
        this.germplasmForm.showEditForm(resultWithRelationsField);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteGermplasm(uri: string) {
    this.service
      .deleteGermplasm(uri)
      .then(() => {
        this.germplasmList.refresh();
        let message =
          this.$i18n.t("GermplasmView.title") +
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
  GermplasmView:
    title: Germplasm 
    description: Manage Genetic Resources Information
    add: Add germplasm
    update: Update Germplasm
    delete: Delete Germplasm
    groupGermplasm: Germplasm Group
fr:
  GermplasmView:
    title: Ressources Génétiques 
    description: Gérer les informations du matériel génétique
    add: Ajouter des ressources génétiques
    update: éditer germplasm
    delete: supprimer germplasm
    groupGermplasm: Groupe de Ressources Génétiques
</i18n>

