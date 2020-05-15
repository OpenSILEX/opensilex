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
      modalSize="xl"
      :successMessage="successMessage"
      component="opensilex-FactorForm"
      createTitle="component.factor.add"
      editTitle="component.factor.update"
      icon="fa#sun"
      @onCreate="showFactorDetails"
      @onUpdate="factorList.refresh()"
    ></opensilex-ModalForm>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-FactorList
          v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_READ_ID)"
          ref="factorList"
          @onEdit="editFactor"
          @onDetails="showFactorDetails"
          @onInteroperability="showSkosReferences"
          @onDelete="deleteFactor"
        ></opensilex-FactorList>
      </template>
    </opensilex-PageContent>
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
  $router: any;

  selectedFactor: any = {
    uri: null,
    names: {},
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: []
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

  @Ref("skosReferences") readonly skosReferences!: any;

  created() {
    console.debug("Loading FactorView view...");
    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }

  showCreateForm() {
    this.factorForm.showCreateForm();
  }

  callUpdateFactorService(form: FactorUpdateDTO, done) {
    done(
      this.service
        .updateFactor(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Updated factor", uri);
          this.factorList.refresh();
        })
    );
  }
  showFactorDetails(factorUriResult: any) {
    factorUriResult.then(factorUri => {
      console.debug("showFactorDetails", factorUri);
      this.factorList.refresh();
      this.$router.push({ path: "/factor/" + encodeURIComponent(factorUri) });
    });
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

  editFactor(uri: any) {
    console.debug("editFactor" + uri);
    this.service
      .getFactor(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        console.log(http.response.result);
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

  successMessage(factor) {
    return this.$i18n.t("component.factor.label") + " " + factor.names.en;
  }
}
</script>

<style scoped lang="scss">
</style>

