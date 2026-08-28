<template>
  <WizardForm
    ref="wizardRef"
    :steps="steps"
    createTitle="component.experiment.add"
    editTitle="component.experiment.update"
    modalSize="lg"
    :initForm="getEmptyForm"
    :createAction="create"
    :updateAction="update"
  >
    <template v-slot:icon></template>
  </WizardForm>
</template>

<script setup lang="ts">

import Vue, {inject, useTemplateRef} from "vue";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "core/HttpResponse";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import WizardForm, {WizardStep} from "@/components/common/forms/WizardForm.vue";
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import ExperimentForm1 from "@/components/experiments/form/ExperimentForm1.vue";
import ExperimentForm2 from "@/components/experiments/form/ExperimentForm2.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()
const wizardRef = useTemplateRef<InstanceType<typeof WizardForm>>('wizardRef')


const  steps: WizardStep[] = [
    {
      component: ExperimentForm1,
    },
    {
      component: ExperimentForm2,
    },
  ];

  function getEmptyForm() {
    return {
      uri: undefined,
      name: "",
      projects: [],
      start_date: undefined,
      end_date: undefined,
      objective: "",
      description: "",
      keywords: [],
      scientific_supervisors: [],
      technical_supervisors: [],
      groups: [],
      organisations: [],
      facilities: [],
      species: [],
      is_public: false,
      funding: [],
      variables: [],
      sensors: [],
      alternative_name:""
    };
  }

  function showCreateForm() {
    wizardRef.value.showCreateForm();
  }

  function showEditForm(form) {
    wizardRef.value.showEditForm(form);
  }

async function create(form) {
  try {
    const http: HttpResponse<OpenSilexResponse<any>> =
        await opensilex
            .getService<ExperimentsService>("opensilex.ExperimentsService")
            .createExperiment(form);

    const uri = http.response.result;

    if (!uri) {
      throw new Error("No URI returned after experiment creation");
    }

    form.uri = uri;

    console.debug("experiment created", form);

    const message =
        t("ExperimentList.name") +
        " " +
        form.name +
        " " +
        t("component.common.success.creation-success-message");

    opensilex.showSuccessToast(message);

    return form;
  } catch (error) {
    if (error.status == 409) {
      console.error("Experiment already exists", error);
      opensilex.errorHandler(
          error,
          t("ExperimentForm.experiment-already-exists")
      );
    } else {
      opensilex.errorHandler(error);
    }

    return false;
  }
}


const emit = defineEmits<{
  (e: string, form: any): void
}>()

async function update(form: any) {
  opensilex
      .getService<ExperimentsService>("opensilex.ExperimentsService")
      .updateExperiment(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        const uri = http.response.resultwizar

        console.debug("experiment updated", uri)

        emit("onUpdate", form)

        const message =
            t("ExperimentList.name") +
            " " +
            form.name +
            " " +
            t("component.common.success.update-success-message")

        opensilex.showSuccessToast(message)
      })
      .catch(opensilex.errorHandler)
}

defineExpose({
  showCreateForm,
  showEditForm
})
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentForm:
    create: Add experiment
    update: Update experiment
    experiment-already-exists: Experiment already exists

fr:
  ExperimentForm:
    create: Ajouter une expérimentation
    update: Modifier une expérimentation
    experiment-already-exists: L'expérimentation existe déjà
</i18n>