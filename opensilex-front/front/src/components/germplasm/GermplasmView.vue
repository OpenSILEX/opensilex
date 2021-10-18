<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#seedling"
      title="GermplasmView.title"
      description="GermplasmView.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions
    v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)">
      <template v-slot>
        <opensilex-HelpButton
          @click="helpModal.show()"
          label="component.common.help-button"
        ></opensilex-HelpButton> 
        <opensilex-CreateButton
          @click="goToGermplasmCreate"
          label="GermplasmView.add"
        ></opensilex-CreateButton> 
      </template>
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
      @onUpdate="germplasmList.refresh()"
    ></opensilex-ModalForm>
    <b-modal ref="helpModal" size="xl" hide-header ok-only>
      <opensilex-GermplasmHelp></opensilex-GermplasmHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

import { GermplasmService, GermplasmCreationDTO, GermplasmUpdateDTO, GermplasmGetSingleDTO } from "opensilex-core/index"
import VueRouter from "vue-router";

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
  @Ref("germplasmForm") readonly germplasmForm!: any;
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


  callCreateGermplasmService(form: GermplasmCreationDTO, done) {
    done(
      this.service
        .createGermplasm(false,form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("germplasm created", uri);
          this.germplasmList.refresh();
        })
    );
  }

  callUpdateGermplasmService(form: GermplasmUpdateDTO, done) {
    console.debug(form);
    done(
      this.service
        .updateGermplasm(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          this.$router.push({
            path: "/germplasm/" + encodeURIComponent(uri),
          });

        })
        .catch(this.$opensilex.errorHandler)
    );
  }


  editGermplasm(uri: string) {
    console.debug("editGermplasm " + uri);
    this.service
      .getGermplasm(uri)
      .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
        this.germplasmForm.getFormRef().getAttributes(http.response.result);
        this.germplasmForm.showEditForm(http.response.result);
        
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteGermplasm(uri: string) {
    console.debug("deleteGermplasm " + uri);
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


}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  GermplasmView:
    title: Germplasm 
    description: Manage Genetic Resources Information
    add: Add germplasm
    update: Update Germplasm
    delete: Delete Germplasm
fr:
  GermplasmView:
    title: Ressources Génétiques 
    description: Gérer les informations du matériel génétique
    add: Ajouter des ressources génétiques
    update: éditer germplasm
    delete: supprimer germplasm
</i18n>

