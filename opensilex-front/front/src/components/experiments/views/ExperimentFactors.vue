<template>
  <div>
    <PageActions class="pageActionsBtns">
      <HelpButton
        @click="helpModal.show()"
        label="component.common.help-button"
        class="helpButton"
        :small="true"
      ></HelpButton>

      <CreateButton
        v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
        @click="factorForm.showCreateForm()"
        :label="t('component.experiment.add-factor')"
        class="createButton"
      ></CreateButton>
    </PageActions>
  
    <Modal
        ref="helpModal"
        modalSize="lg"
        hide-header
        hide-footer>
      <FactorsHelp @hideBtnIsClicked="hide()"></FactorsHelp>
    </Modal>

    <FactorForm
      v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
      :experiment="uri"
      ref="factorForm"
      modalSize="lg"
      :tutorial="true"
      :successMessage="successMessage"
      component="FactorForm"
      :createTitle="t('component.experiment.add-factor')"
      :editTitle="t('component.experiment.update-factor')"
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

import {computed, inject, onMounted, ref, useTemplateRef} from "vue";
import { FactorsService, FactorDetailsGetDTO, FactorUpdateDTO } from "opensilex-core/index";
import PageActions from "@/components/layout/PageActions.vue";
import HelpButton from "@/components/common/buttons/HelpButton.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import ExternalReferencesModalForm from "@/components/common/external-references/ExternalReferencesModalForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {useRoute, useRouter} from "vue-router";
import FactorList from "@/components/experiments/factors/FactorList.vue";
import FactorForm from "@/components/experiments/factors/FactorForm.vue";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import Modal from "@/components/common/views/Modal.vue";
import FactorsHelp from "@/components/experiments/factors/FactorsHelp.vue";

//#region Private

//#region Plugins and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
let service = opensilex.getService<FactorsService>('opensilex.FactorsService')
const { t } = useI18n()
const router = useRouter()
const route = useRoute()
//#endregion

//#region Template refs
const factorForm = useTemplateRef<InstanceType<typeof FactorForm>>('factorForm')
const factorList = useTemplateRef<InstanceType<typeof FactorList>>('factorList')
const skosReferences = useTemplateRef<InstanceType<typeof ExternalReferencesModalForm>>('skosReferences')
const helpModal = useTemplateRef<InstanceType<typeof Modal>>('helpModal')

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
//#endregion
//#region Data and computed
const user = computed(() => {
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})
//#endregion
//#region Hooks
onMounted(()  => {
  console.debug("Loading ExperimentFactors view...");
  uri.value = decodeURIComponent(route.params.uri as string);
})
//#endregion
  function initForm(form) {
    form.experiment = uri.value

    return form;
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
      .getService<FactorsService>("opensilex.FactorsService")
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

//#endregion
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

.createButton, .helpButton{
  margin: 5px 15px 5px -10px;
}

</style>

