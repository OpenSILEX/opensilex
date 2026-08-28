<template>
  <n-form
      ref="refValidator"
      :rules="rules"
      :model="experiment">
    <PersonSelector
        label="component.experiment.scientificSupervisors"
        :persons.sync="experiment.scientific_supervisors"
        :multiple="true"
        helpMessage="ExperimentForm.scientificSupervisors"
    ></PersonSelector>

    <PersonSelector
        label="component.experiment.technicalSupervisors"
        :persons.sync="experiment.technical_supervisors"
        :multiple="true"
        helpMessage="ExperimentForm.technicalSupervisors"
    ></PersonSelector>

    <ProjectSelector
        label="component.experiment.projects"
        :projects.sync="experiment.projects"
        :multiple="true"
    ></ProjectSelector>

    <OrganizationSelector
        label="component.experiment.organizations"
        :organizations.sync="experiment.organisations"
        :multiple="true"
    ></OrganizationSelector>

    <FacilitySelector
        label="component.experiment.facilities"
        :facilities.sync="experiment.facilities"
        :multiple="true"
        path="facilities"
    ></FacilitySelector>

    <GroupSelector
        label="component.experiment.groups"
        :groups.sync="experiment.groups"
        :multiple="true"
    ></GroupSelector>

    <FundingSelector
        label="component.experiment.funding"
        :funding.sync="experiment.funding"
        :multiple="true"
        helpMessage="ExperimentForm.funding"
    ></FundingSelector>

    <CheckboxForm
        :value.sync="form.is_public"
        label="component.experiment.public-label"
        title="component.experiment.public-title"
    ></CheckboxForm>
  </n-form>
</template>

<script setup lang="ts">
import Vue, {computed, inject, useTemplateRef} from "vue";
// @ts-ignore
import {ExperimentCreationDTO} from "core/index";
import PersonSelector from "@/components/persons/PersonSelector.vue";
import ProjectSelector from "@/components/projects/ProjectSelector.vue";
import OrganizationSelector from "@/components/organizations/OrganizationSelector.vue";
import FacilitySelector from "@/components/facilities/FacilitySelector.vue";
import GroupSelector from "@/components/groups/GroupSelector.vue";
import FundingSelector from "@/components/experiments/FundingSelector.vue";
import CheckboxForm from "@/components/common/forms/CheckboxForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {FormRules, NForm} from "naive-ui";
import {required} from "@/models/FormFieldsFormatter";

const opensilex = inject<OpenSilexVuePlugin>('opensilex')
const validatorRef = useTemplateRef<InstanceType<typeof NForm>>('refValidator')

const rules = computed<FormRules>(() => ({}))

const experiment = defineModel<ExperimentCreationDTO>("form");

function reset() {
  return this.validatorRef.reset();
}

function validate() {
  return validatorRef.value.validate();
}

defineExpose({
  validate
})
</script>
<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentForm:
    scientificSupervisors: Experiment supervisor(s). Only persons existing in the system. If unavailable in the predefined list, persons can be added from the Users menu, prior to the new experiment creation.
    technicalSupervisors: Technicians and scientists (including phd students, interns, etc.) involved in the experiment implementation
    funding: Select the experiment's funding, only the first three selected will be displayed in the logos
fr:
  ExperimentForm:
    scientificSupervisors: Responsables de l'expérimentation. Uniquement les personnes existant dans le système. Si elles ne sont pas disponibles dans la liste prédéfinie, les personnes peuvent être ajoutées à partir du menu Utilisateurs, avant la création de la nouvelle expérimentation.
    technicalSupervisors: Techniciens et scientifiques (y compris les doctorants, stagiaires, etc.) participant à la mise en œuvre de l'expérience.
    funding: Sélectionnez les financements de l'expérience, seuls les trois premiers sélectionnés seront affichés dans les logos
</i18n>