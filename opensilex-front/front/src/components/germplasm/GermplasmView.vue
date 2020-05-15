<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik-folder"
      title="component.germplasm.germplasmTitle"
      description="component.germplasm.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
          @click="goToGermplasmCreate"
          label="component.germplasm.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-GermplasmList
          v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_READ_ID)"
          ref="germplasmList"
          @onEdit="germplasmForm.showEditForm($event)"
          @onDetails="showGermplasmDetails"
        ></opensilex-GermplasmList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
      ref="germplasmForm"
      component="opensilex-GermplasmForm"
      createTitle="component.germplasm.add"
      editTitle="component.germplasm.update"
      icon="ik#ik-user"
      modalSize="lg"
      @onCreate="germplasmList.refresh()"
      @onUpdate="germplasmList.refresh()"
    ></opensilex-ModalForm>
    <opensilex-GermplasmDetails
     ref="germplasmDetails">
    </opensilex-GermplasmDetails>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

import { 
  GermplasmService, 
  GermplasmCreationDTO, 
  GermplasmGetDTO,
  OntologyService, 
  ResourceTreeDTO 
  } from "opensilex-core/index"
import GermplasmCreate from "./GermplasmCreate.vue";
import Oeso from "../../ontologies/Oeso";

@Component
export default class GermplasmView extends Vue {
  $opensilex: any;
  $store: any;
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

  created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
  }

  showCreateForm() {
    this.germplasmForm.showCreateForm();
  }

  goToGermplasmCreate(){    
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

  showGermplasmDetails(uri: string){
    console.debug("showGermplasmDetails" + uri);
    this.service
      .getGermplasm(uri)
      .then((http: HttpResponse<OpenSilexResponse<GermplasmGetDTO>>) => {
        console.log(http.response.result);
        this.germplasmDetails.showDetails(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  callUpdateGermplasmService(form: GermplasmCreationDTO, done) {
    console.debug(form);
    done(
      this.service
        .updateGermplasm(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("germplasm updated", uri);
          this.germplasmList.refresh();
        })
        .catch(this.$opensilex.errorHandler)
    );
  }

  editGermplasm(uri: string) {
    console.log("editGermplasm " + uri);
    this.service
      .getGermplasm(uri)
      .then((http: HttpResponse<OpenSilexResponse<GermplasmGetDTO>>) => {
        this.germplasmForm.showEditForm(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteGermplasm(uri: string) {
    console.log("deleteGermplasm " + uri);
    this.service
      .deleteGermplasm(uri)
      .then(() => {
        this.germplasmList.refresh();
        let message =
          this.$i18n.t("component.germplasm.germplasm") +
          " " +
          uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        //this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
  }


}
</script>

<style scoped lang="scss">
</style>

