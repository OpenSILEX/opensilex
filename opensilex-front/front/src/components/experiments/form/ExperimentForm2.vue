<template>
  <n-form
      ref="refValidator"
      :rules="rules"
      :model="experiment">
    <PersonSelector
        label="component.experiment.scientificSupervisors"
        v-model:persons="experiment.scientific_supervisors"
        :multiple="true"
        helpMessage="component.experiment.scientificSupervisors-help"
    ></PersonSelector>

    <PersonSelector
        label="component.experiment.technicalSupervisors"
        v-model:persons="experiment.technical_supervisors"
        :multiple="true"
        helpMessage="component.experiment.technicalSupervisors-help"
    ></PersonSelector>

    <ProjectSelector
        label="component.experiment.projects"
        v-model:projects="experiment.projects"
        :multiple="true"
    ></ProjectSelector>

    <OrganizationSelector
        label="component.experiment.organizations"
        v-model:organizations="experiment.organisations"
        :multiple="true"
    ></OrganizationSelector>

    <FacilitySelector
        label="component.experiment.facilities"
        v-model:facilities="experiment.facilities"
        :multiple="true"
        path="facilities"
    ></FacilitySelector>

    <GroupSelector
        label="component.experiment.groups"
        v-model:groups="experiment.groups"
        :multiple="true"
    ></GroupSelector>

    <FundingSelector
        label="component.experiment.funding"
        v-model:fundinguri="experiment.funding"
        :multiple="true"
        helpMessage="component.experiment.funding-help"
    ></FundingSelector>

    <CheckboxForm
        v-model:value="experiment.is_public"
        label="component.experiment.public-label"
        title="component.experiment.public-title"
    ></CheckboxForm>
  </n-form>
</template>

<script setup lang="ts">
import {computed, inject, useTemplateRef} from "vue";
import {ExperimentCreationDTO} from "opensilex-core/index";
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

function validate() {
  return validatorRef.value.validate();
}

defineExpose({
  validate
})
</script>
<style scoped lang="scss">
</style>
