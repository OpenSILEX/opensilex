<template>
  <div class="container-fluid">
    <opensilex-PageActions>
      <template v-slot>
        <opensilex-HelpButton
          @click="helpModal.show()"
          label="component.common.help-button"
        ></opensilex-HelpButton>
        <opensilex-CreateButton
          v-if="
            user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
          "
          @click="factorForm.showCreateForm()"
          label="component.factor.add-button"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>
    <b-modal ref="helpModal" size="xl" hide-header ok-only>
      <opensilex-FactorsHelp></opensilex-FactorsHelp>
    </b-modal>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
      ref="factorForm"
      modalSize="lg"
      :tutorial="true"
      :successMessage="successMessage"
      component="opensilex-FactorForm"
      createTitle="component.factor.add"
      editTitle="component.factor.update"
      icon="fa#sun"
      :initForm="initForm"
      @onCreate="showFactorDetails"
      @onUpdate="factorList.refresh()"
    ></opensilex-ModalForm>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-FactorList
          ref="factorList"
          :experiment="uri"
          @onEdit="editFactor"
          @onDetails="showFactorDetails"
          @onInteroperability="showSkosReferences"
          @onDelete="deleteFactor"
        ></opensilex-FactorList>
      </template>
    </opensilex-PageContent>
    <opensilex-ExternalReferencesModalForm
      ref="skosReferences"
      :references.sync="selectedFactor"
      @onUpdate="callUpdateFactorService"
    ></opensilex-ExternalReferencesModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

import { FactorsService, FactorDetailsGetDTO, FactorUpdateDTO } from "opensilex-core/index";

@Component
export default class ExperimentFactors extends Vue {
  $opensilex: any;
  $store: any;
  service: FactorsService;
  $t: any;
  $i18n: any;
  $router: any;
  uri: string;

  selectedFactor: any = {
    uri: null,
    name: null,
    category: null,
    description: null,
    experiment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: [],
  };

  initForm(form) {
    form.experiment = this.uri;

    return form;
  }

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

  @Ref("helpModal") readonly helpModal!: any;

  created() {
    console.debug("Loading ExperimentFactors view...");
    this.uri = decodeURIComponent(this.$route.params.uri);

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
    if (factorUriResult instanceof Promise) {
      console.log(factorUriResult);
      factorUriResult.then((factorUri) => {
        console.debug("showFactorDetails", factorUri);
        this.$store.commit("storeReturnPage", this.$router);
        this.$router.push({
          path:
            "/" +
            encodeURIComponent(this.uri) +
            "/factor/details/" +
            encodeURIComponent(factorUri),
        });
      });
    } else {
      console.debug("showFactorDetails", factorUriResult);
      this.$store.commit("storeReturnPage", this.$router);
      this.$router.push({
        path:
          "/" +
          encodeURIComponent(this.uri) +
          "/factor/details/" +
          encodeURIComponent(factorUriResult),
      });
    }
  }

  showSkosReferences(uri: string) {
    console.debug("showSkosReferences" + uri);
    this.service
      .getFactorByURI(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        let result = http.response.result;
        if (result instanceof Promise) {
          result.then((resolve) => {
            this.selectedFactor = result;
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
      .getFactorByURI(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        console.debug(http.response.result);
        this.factorForm.showEditForm(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteFactor(factor: any) {
    console.debug("check Associated factor " + factor.uri);
    let isAssociated = this.$opensilex
      .getService("opensilex.FactorsService")
      .getFactorAssociatedExperiments(factor.uri)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        if (
          http.response.metadata.pagination.totalCount > 0 &&
          factor.experiment != this.uri
        ) {
          this.$opensilex.showErrorToast(
            this.$i18n.t("component.factor.isAssociatedTo")
          );
        } else {
          console.debug("deleteFactor " + factor.uri);
          this.service
            .deleteFactor(factor.uri)
            .then(() => {
              let message =
                this.$i18n.t("component.factor.label") +
                " " +
                factor.uri +
                " " +
                this.$i18n.t("component.common.success.delete-success-message");
              this.$opensilex.showSuccessToast(message);
              this.factorList.refresh();
            })
            .catch(this.$opensilex.errorHandler);
        }
      });
  }

  successMessage(factor) {
    return this.$i18n.t("component.factor.label") + " " + factor.name;
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  component:
    factor :
      description-title-help: Manage experimental factors
      label: Factor
      uri: URI
      uri-help: Unique factor identifier
      alias-placeholder: Enter factor alias
      factors: factor
      add: Add factor
      add-button: Add factor
      update-button: Update factor
      update: Update factor
      name: Name
      name-help: Usual name which describes a factor in an experiment
      name-placeholder: Irrigation, Shading, Planting year, etc.
      category: Category
      category-help: General category used to classify factors
      category-placeholder: Environnement-Irrigation, Field management, etc ...
      description: description
      description-help: Description associated with this factor (protocol, amount of component)
      description-error: Must contain at least 10 characters
      description-placeholder: Protocol n°1289 - Amount of water 5 ml/Days.
      errors:
        user-already-exists: A factor already exists with this URI.
      select:
        other: Other
        fieldManagement: Field management
        lightManagement: Light management
        waterManagement: Water management
        chemical: Chemical
        bioticStress: Biotic stress
        soil: Soil
        nutrient: Nutrient
        atmospheric: Atmospheric
        temperature: Temperature
      isAssociatedTo : This factor is associated with one or more experiments and can not be removed

fr:
  component:
    factor:
      description-title-help: Gestion des facteurs expérimentaux
      label: Facteur
      uri: URI
      uri-help: Identifiant unique du facteur
      filter-placeholder: Utiliser ce filter pour filter les facteurs
      factors: facteur
      add: Ajouter facteur
      add-button: Ajouter facteur
      update-button: Modifier facteur
      update: Mettre à jour un facteur
      name: Nom
      name-en: Nom
      name-help: Nom qui décrit un facteur dans une expérimentation
      name-placeholder: Irrigation, Ombrage, Année de plantation, etc.
      category: Catégorie
      category-help: Grandes catégories servant à classifier les facteurs
      category-placeholder: Irrigation-Environnement, Conduite au champ , etc.
      description: Description
      description-help: Description associée à ce facteur (protocole, apport de composé)
      form-description-placeholder: Protocole n°1289 - Apport d'eau de 5 ml/jour.
      errors:
        user-already-exists: URI du facteur déjà existante.
      select:
        other: Autre
        fieldManagement: Conduite culturale
        lightManagement: Gestion de la lumière
        waterManagement: Gestion de l'eau
        chemical: Chimique
        bioticStress: Stress biotique
        soil: Sol
        nutrient: Nutriments
        atmospheric: Atmosphérique
        temperature: Température
      isAssociatedTo : Ce facteur est associé à une ou plusieurs expérimentations et ne peut être supprimé
</i18n>
