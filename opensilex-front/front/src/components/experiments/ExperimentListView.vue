<template>
  <div class="container-fluid">
    <PageActions v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)">
      <CreateButton
        @click="experimentForm.showCreateForm()"
        label="component.experiment.search.buttons.create-experiment"
        class="createButton"
      ></CreateButton>
    </PageActions>

    <PageContent>
      <template v-slot>
        <ExperimentList
          ref="experimentList"
          @onEdit="showEditForm($event)"
        ></ExperimentList>
      </template>
    </PageContent>

    <ExperimentForm
      v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
      v-model:form="experiment"
      ref="experimentForm"
      @onCreate="redirectToCreatedExperiment"
      @onUpdate="experimentList.updateSelectedExperiment()"
    ></ExperimentForm>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, inject, useTemplateRef } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import DTOConverter from '../../models/DTOConverter';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import PageActions from '../layout/PageActions.vue';
import CreateButton from '../common/buttons/CreateButton.vue';
import ExperimentList from '@/components/experiments/ExperimentList.vue';
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import ExperimentForm from "@/components/experiments/form/ExperimentForm.vue";
import PageContent from "@/components/layout/PageContent.vue";
import {ExperimentCreationDTO} from "opensilex-core/model/experimentCreationDTO";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const store = useStore();
const router = useRouter();
const experimentForm = useTemplateRef<InstanceType<typeof ExperimentForm>>('experimentForm');
const experimentList = useTemplateRef<InstanceType<typeof ExperimentList>>('experimentList');
const experiment = ref<ExperimentCreationDTO>({} as ExperimentCreationDTO);

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

function refresh() {
  experimentList.value?.refresh();
}

function showEditForm(uri: string) {
  opensilex
    .getService<ExperimentsService>('opensilex.ExperimentsService')
    .getExperiment(uri)
    .then((http) => {
      experimentForm.value?.showEditForm(
        DTOConverter.extractURIFromResourceProperties(http.response.result)
      );
    });
}

function redirectToCreatedExperiment(experiment) {
  router.push({
    path: '/experiment/details/' + encodeURIComponent(experiment.uri),
  });
}

</script>

<style scoped lang="scss">
.createButton,
.helpButton {
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
  margin-right: 5px;
}
</style>

<i18n>
en:
  ExperimentList:
    filter-label: Search by name
    label-filter-placeholder: Enter a name
    filter-year: Search by year
    year-filter-placeholder: Enter a year
    filter-species: Search by species

fr:
  ExperimentList:
    filter-label: Filtrer par nom
    label-filter-placeholder: Saisir un nom
    filter-year: Filtrer par année
    year-filter-placeholder: Saisir une année
    filter-species: Filtrer par espèces
</i18n>
