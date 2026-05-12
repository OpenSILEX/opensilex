<template>
  <div class="container-fluid">
    <PageActions
        v-if="
      user.hasCredential(
      credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)
    ">
      <HelpButton
          @click="showHelpModal = true"
          label="component.common.help-button"
          class="helpButton"
      ></HelpButton>
      <CreateButton
          @click="router.push({ path: '/germplasm/create' })"
          :label="t('add')"
          class="createButton"
      ></CreateButton>
    </PageActions>

    <PageContent>
      <template v-slot>
        <GermplasmList
            ref="germplasmList"
            @onEdit="editGermplasm"
            @onDelete="deleteGermplasm"
        ></GermplasmList>
      </template>
    </PageContent>

    <ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
        ref="germplasmForm"
        component="opensilex-GermplasmForm"
        :createTitle="t('add')"
        :editTitle="t('update')"
        icon="fa#seedling"
        @onCreate="germplasmList.refresh()"
        @onUpdate="germplasmList.updateSelectedGermplasm()"
    ></ModalForm>
    <n-modal
        v-model:show="showHelpModal"
        preset="card"
        :style="{ width: '600px' }"
    >
      <GermplasmHelp></GermplasmHelp>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import HelpButton from "@/components/common/buttons/HelpButton.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import GermplasmHelp from "@/components/germplasm/list/GermplasmHelp.vue";
import {NModal} from "naive-ui";
import {computed, inject, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useRouter} from "vue-router";
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import {GermplasmGetSingleDTO} from "opensilex-core/model/germplasmGetSingleDTO";
import {GermplasmUpdateDTO} from "opensilex-core/model/germplasmUpdateDTO";
import GermplasmForm from "@/components/germplasm/form/GermplasmForm.vue";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import GermplasmList from "@/components/germplasm/list/GermplasmList.vue";
import {useI18n} from "vue-i18n";
import PageActions from "@/components/layout/PageActions.vue";

//#region Public

//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const store = useStore();
const { t } = useI18n();
const router = useRouter();
const service = opensilex.getService<GermplasmService>("opensilex.GermplasmService")

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const germplasmForm = useTemplateRef<InstanceType<typeof ModalForm>>("germplasmForm");
const germplasmList = useTemplateRef<InstanceType<typeof GermplasmList>>("germplasmList");

const showHelpModal = ref(false);

function editGermplasm(uri: string) {
  service.getGermplasm(uri)
      .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
        let form: InstanceType<typeof GermplasmForm> = this.germplasmForm.getFormRef();
        form.readAttributes(http.response.result.metadata);
        //Take the has_parent_germplasm properties from the GermplasmGetSingleDTO and put the correct uris into the relations attribute of GermplasmUpdateDTO
        //The labels will be loaded with another service call inside the GermplasmForm component
        let resultWithRelationsField : GermplasmUpdateDTO = GermplasmForm.readDuplicatableRelations(http.response.result);
        germplasmForm.value.showEditForm(resultWithRelationsField);
      })
      .catch(opensilex.errorHandler);
}

function deleteGermplasm(uri: string) {
  service
      .deleteGermplasm(uri)
      .then(() => {
        germplasmList.value.refresh();
        let message = t("deleteSuccessMessage", { uri });
        opensilex.showSuccessToast(message);
      })
      .catch(opensilex.errorHandler);
}
//#endregion
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  deleteSuccessMessage: "Germplasm <{uri}> has been deleted."
  add: Add and modify germplasms
  update: Update Germplasm
  delete: Delete Germplasm
  groupGermplasm: Germplasm Group
fr:
  deleteSuccessMessage: "La ressource génétique <{uri}> a été supprimé."
  add: Ajouter et modifier des ressources génétiques
  update: éditer germplasm
  delete: supprimer germplasm
  groupGermplasm: Groupe de Ressources Génétiques
</i18n>
