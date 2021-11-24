<template>
  <ValidationObserver ref="validatorRef">
    <opensilex-UserSelector
      label="component.experiment.scientificSupervisors"
      :users.sync="experiment.scientific_supervisors"
      :multiple="true"
      helpMessage="ExperimentForm.scientificSupervisors"
    ></opensilex-UserSelector>

    <opensilex-UserSelector
      label="component.experiment.technicalSupervisors"
      :users.sync="experiment.technical_supervisors"
      :multiple="true"
      helpMessage="ExperimentForm.technicalSupervisors"
    ></opensilex-UserSelector>

    <opensilex-ProjectSelector
      label="component.experiment.projects"
      :projects.sync="experiment.projects"
      :multiple="true"
    ></opensilex-ProjectSelector>

    <opensilex-InfrastructureSelector
      label="component.experiment.infrastructures"
      :infrastructures.sync="experiment.organisations"
      :multiple="true"
    ></opensilex-InfrastructureSelector>

    <opensilex-InfrastructureFacilitySelector
      label="component.experiment.facilities"
      :facilities.sync="experiment.facilities"
      :multiple="true"
    ></opensilex-InfrastructureFacilitySelector>

    <opensilex-GroupSelector
      label="component.experiment.groups"
      :groups.sync="experiment.groups"
      :multiple="true"
    ></opensilex-GroupSelector>

    <opensilex-CheckboxForm
      :value.sync="form.is_public"
      label="component.experiment.public-label"
      title="component.experiment.public-title"
    ></opensilex-CheckboxForm>
  </ValidationObserver>
</template>

<script lang="ts">
import { Component, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ExperimentCreationDTO } from "opensilex-core/index";

@Component
export default class ExperimentForm2 extends Vue {
  $opensilex: any;

  @Ref("validatorRef") readonly validatorRef!: any;

  @PropSync("form")
  experiment: ExperimentCreationDTO;

  reset() {
    return this.validatorRef.reset();
  }

  validate() {
    return this.validatorRef.validate();
  }
}
</script>
<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentForm:
    scientificSupervisors: Experiment supervisor(s). Only persons existing in the system. If unavailable in the predefined list, persons can be added from the Users menu, prior to the new experiment creation. 
    technicalSupervisors: Technicians and scientists (including phd students, interns, etc.) involved in the experiment implementation
fr:
  ExperimentForm:
    scientificSupervisors: Responsables de l'expérimentation. Uniquement les personnes existant dans le système. Si elles ne sont pas disponibles dans la liste prédéfinie, les personnes peuvent être ajoutées à partir du menu Utilisateurs, avant la création de la nouvelle expérimentation. 
    technicalSupervisors: Techniciens et scientifiques (y compris les doctorants, stagiaires, etc.) participant à la mise en œuvre de l'expérience.
</i18n>