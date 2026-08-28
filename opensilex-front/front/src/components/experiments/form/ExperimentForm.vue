<template>
  <WizardForm
    ref="wizardRef"
    :steps="steps"
    createTitle="ExperimentForm.create"
    editTitle="ExperimentForm.update"
    icon="ik#ik-layers"
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

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()
const wizardRef = useTemplateRef<InstanceType<typeof WizardForm>>('wizardRef')


const  steps: WizardStep[] = [
    {
      component: ExperimentForm1,
    },
    {
      component: "ExperimentForm2",
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
      opensilex
      .getService<ExperimentsService>("opensilex.ExperimentsService")
      .createExperiment(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        form.uri = uri;
        console.debug("experiment created", uri);
        this.$emit("onCreate", form);
        let message = t("ExperimentList.name") + " " + form.name + " " + t("component.common.success.creation-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Experiment already exists", error);
          opensilex.errorHandler(
            error,
            t("ExperimentForm.experiment-already-exists")
          );
        } else {
          opensilex.errorHandler(error);
        }
      });
  }


const emit = defineEmits<{
  (e: "onUpdate", form: any): void
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