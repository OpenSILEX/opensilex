<template>
  <div class="container-fluid"> 
    <opensilex-PageHeader
      icon="fa#sun"
      title="component.menu.experimentalDesign.factors"
      description="component.factor.description"
    ></opensilex-PageHeader>
      <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
          @click="factorForm.showCreateForm()"
          label="component.factor.add-button"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>
    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
      ref="factorForm"
      modalSize="lg"
      component="opensilex-FactorForm"
      createTitle="component.factor.add"
      editTitle="component.factor.update"
      icon="fa#sun"
      @onDetails="showFactorDetails"
    ></opensilex-ModalForm>
    <opensilex-FactorList
      ref="factorList"
      @onEdit="editFactor"
      @onDelete="deleteFactor"
      @onDetails="showFactorDetails"
      @onInteroperability="showSkosReferences"
    ></opensilex-FactorList>
    <opensilex-FactorDetails ref="factorDetails" @onUpdate="callUpdateFactorService"></opensilex-FactorDetails>
    <opensilex-ExternalReferencesForm
      ref="skosReferences"
      :skosReferences="selectedFactor"
      @onUpdate="callUpdateFactorService"
    ></opensilex-ExternalReferencesForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  FactorCreationDTO,
  FactorsService,
  FactorGetDTO,
  FactorDetailsGetDTO,
  FactorSearchDTO,
  FactorUpdateDTO
} from "opensilex-core/index";

@Component
export default class FactorView extends Vue {
  $opensilex: any;
  $store: any;
  service: FactorsService;
  $t: any;
  $i18n: any;

  selectedFactor: any = {
    uri: null,
    name: null,
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: []
    // lang: "en-US"
  };

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("modalRef") readonly modalRef!: any;

  @Ref("factorForm") readonly factorForm!: any;

  @Ref("factorList") readonly factorList!: any;

  @Ref("factorDetails") readonly factorDetails!: any;

  @Ref("skosReferences") readonly skosReferences!: any;

  created() {
    console.debug("Loading FactorView view...");
    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }


  showCreateForm() {
    this.factorForm.showCreateForm();
  }

  callCreateFactorService(form: FactorCreationDTO, done) {
    done(
      this.service
        .createFactor(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Created factor", uri);
          this.factorList.refresh();
          this.creationOrUpdateMessage(form, uri);
          return uri;
        })
    );
  }

  callUpdateFactorService(form: FactorUpdateDTO, done) {
    done(
      this.service
        .updateFactor(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Updated factor", uri);
          this.creationOrUpdateMessage(form, uri);
          this.factorList.refresh();
        })
    );
  }
  showFactorDetails(uri: string) {
    console.debug("showFactorDetails" + uri);
    this.service
      .getFactor(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        this.factorDetails.showDetails(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  showSkosReferences(uri: string) {
    console.debug("showSkosReferences" + uri);
    this.service
      .getFactor(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        let result = http.response.result;
        if (result instanceof Promise) {
          result.then(resolve => {
            this.selectedFactor = resolve;
            this.skosReferences.show();
          });
        } else {
          this.selectedFactor = result;
          this.skosReferences.show();
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  editFactor(uri: string) {
    console.debug("editFactor" + uri);
    this.service
      .getFactor(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        this.factorForm.showEditForm(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteFactor(uri: string) {
    console.debug("deleteFactor " + uri);
    this.service
      .deleteFactor(uri)
      .then(() => {
        this.factorList.refresh();
        let message =
          this.$i18n.t("component.factor.label") +
          " " +
          uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
  }

  creationOrUpdateMessage(instance, uri) {
    let message =
      this.$i18n.t("component.factor.label") +
      " " +
      instance.names.en +
      " (" +
      uri +
      ")  ";
    if (this.editFactor) {
      message = message + this.$i18n.t("component.common.success.update-success-message");
    } else {
      message = message + this.$i18n.t("component.common.success.creation-success-message");
    }
    this.$opensilex.showSuccessToast(message);
  }
}
</script>

<style scoped lang="scss">
</style>

