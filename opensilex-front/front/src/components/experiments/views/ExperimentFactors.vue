<template>
  <div>
    <PageActions class="pageActionsBtns">
      <HelpButton
        @click="helpModal.show()"
        label="component.common.help-button"
        class="helpButton"
      ></HelpButton>

      <CreateButton
        v-if="
          user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
        "
        @click="factorForm.showCreateForm()"
        :label="t('component.factor.add-button')"
        class="createButton"
      ></CreateButton>
    </PageActions>
  
    <b-modal ref="helpModal" size="xl" hide-header hide-footer>
      <FactorsHelp @hideBtnIsClicked="hide()"></FactorsHelp>
    </b-modal>

    <FactorForm
      v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
      ref="factorForm"
      modalSize="lg"
      :tutorial="true"
      :successMessage="successMessage"
      component="FactorForm"
      createTitle="component.factor.add"
      editTitle="component.factor.update"
      icon="fa#sun"
      :initForm="initForm"
      @onCreate="showFactorDetails"
      @onUpdate="factorList.refresh()"
    ></FactorForm>

    <PageContent>
      <template v-slot>
        <div class="card">
          <div class="card-body">
            <FactorList
              ref="factorList"
              :experiment="uri"
              @onEdit="editFactor"
              @onDetails="showFactorDetails"
              @onInteroperability="showSkosReferences"
              @onDelete="deleteFactor"
            ></FactorList>
          </div>
        </div>
      </template>
    </PageContent>
    
    <ExternalReferencesModalForm
      ref="skosReferences"
      :references.sync="selectedFactor"
      @onUpdate="callUpdateFactorService"
    ></ExternalReferencesModalForm>
  </div>
 
</template>

<script setup lang="ts">

import Vue, {computed, inject, ref, useTemplateRef} from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
// @ts-ignore
import { FactorsService, FactorDetailsGetDTO, FactorUpdateDTO } from "opensilex-core/index";
import PageActions from "@/components/layout/PageActions.vue";
import HelpButton from "@/components/common/buttons/HelpButton.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import ExternalReferencesModalForm from "@/components/common/external-references/ExternalReferencesModalForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {useRouter} from "vue-router";
import FactorList from "@/components/experiments/factors/FactorList.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
let service = opensilex.getService<FactorsService>('opensilex.FactorsService')
const { t } = useI18n()
const router = useRouter()
const uri = ref<string>()

const selectedFactor = ref<any>({
  uri: null,
  name: null,
  category: null,
  description: null,
  experiment: null,
  exactMatch: [],
  closeMatch: [],
  broader: [],
  narrower: [],
})

  function initForm(form) {
    form.experiment = uri.value

    return form;
  }

  const user = computed(() => {
    return store.state.user;
  })

const credentials = computed(() => {
  return store.state.credentials;
})

const factorForm = useTemplateRef<InstanceType<typeof FactorForm>>('factorForm')
const factorList = useTemplateRef<InstanceType<typeof FactorList>>('factorList')
const skosReferences = useTemplateRef<InstanceType<typeof ExternalReferencesModalForm>>('skosReferences')
const modalRef = ref<any>()
const helpModal = ref<any>()

  function created() {
    console.debug("Loading ExperimentFactors view...");
    uri.value = decodeURIComponent(route.params.uri.value);

    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }

 function showCreateForm() {
    factorForm.value.showCreateForm();
  }

  function callUpdateFactorService(form: FactorUpdateDTO, done) {
    done(
      service
        .updateFactor(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Updated factor", uri);
          factorList.value.refresh();
        })
    );
  }
  function showFactorDetails(factorUriResult: any) {
    if (factorUriResult instanceof Promise) {
      console.log(factorUriResult);
      factorUriResult.then((factorUri) => {
        console.debug("showFactorDetails", factorUri);
        store.commit("storeReturnPage", router);
        router.push({
          path:
            "/" +
            encodeURIComponent(uri.value) +
            "/factor/details/" +
            encodeURIComponent(factorUri),
        });
      });
    } else {
      console.debug("showFactorDetails", factorUriResult);
      store.commit("storeReturnPage", router);
      router.push({
        path:
          "/" +
          encodeURIComponent(uri.value) +
          "/factor/details/" +
          encodeURIComponent(factorUriResult),
      });
    }
  }

  function showSkosReferences(uri: string) {
    console.debug("showSkosReferences" + uri);
    service
      .getFactorByURI(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        let result = http.response.result;
        if (result instanceof Promise) {
          result.then((resolve) => {
            selectedFactor.value = result;
            skosReferences.value.show();
          });
        } else {
          selectedFactor.value = result;
          skosReferences.value.show();
        }
      })
      .catch(opensilex.errorHandler);
  }

  function editFactor(uri: any) {
    console.debug("editFactor" + uri);
    service
      .getFactorByURI(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        console.debug(http.response.result);
        factorForm.value.showEditForm(http.response.result);
      })
      .catch(opensilex.errorHandler);
  }

  function deleteFactor(factor: any) {
    console.debug("check Associated factor " + factor.uri);
    let isAssociated = opensilex
      .getService("opensilex.FactorsService")
      .getFactorAssociatedExperiments(factor.uri)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        if (
          http.response.metadata.pagination.totalCount > 0 &&
          factor.experiment != uri.value
        ) {
          opensilex.showErrorToast(
            t("component.factor.isAssociatedTo")
          );
        } else {
          console.debug("deleteFactor " + factor.uri);
          service
            .deleteFactor(factor.uri)
            .then(() => {
              let message =
                t("component.factor.label") +
                " " +
                factor.uri +
                " " +
                t("component.common.success.delete-success-message");
              opensilex.showSuccessToast(message);
              factorList.value.refresh();
            })
            .catch(opensilex.errorHandler);
        }
      });
  }

  function successMessage(factor) {
    return t("component.factor.label") + " " + factor.name;
  }
  function hide() {
    helpModal.value.hide();
  }

</script>

<style scoped lang="scss">

.pageActionsBtns {
  margin-left: 10px;
  margin-bottom: 10px;
}

.helpButton {
  margin-left: -5px;
  color: #00A28C;
  font-size: 1.2em;
  border: none;
}
  
.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1;
}


.createButton {
  margin-left: 5px;
}

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
