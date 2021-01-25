<template>
  <ValidationObserver ref="validatorRef">
    <opensilex-UserSelector
      label="component.experiment.scientificSupervisors"
      :users.sync="experiment.scientific_supervisors"
      :multiple="true"
    ></opensilex-UserSelector>

    <opensilex-UserSelector
      label="component.experiment.technicalSupervisors"
      :users.sync="experiment.technical_supervisors"
      :multiple="true"
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
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import { ExperimentCreationDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

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
