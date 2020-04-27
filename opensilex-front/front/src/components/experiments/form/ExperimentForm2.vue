<template>
  <ValidationObserver ref="validatorRef">
    <opensilex-UserSelector
      label="component.experiment.scientificSupervisors"
      :users.sync="experiment.scientificSupervisors"
      :multiple="true"
    ></opensilex-UserSelector>

    <opensilex-UserSelector
      label="component.experiment.technicalSupervisors"
      :users.sync="experiment.technicalSupervisors"
      :multiple="true"
    ></opensilex-UserSelector>

    <opensilex-InfrastructureSelector
      label="component.experiment.infrastructures"
      :infrastructures.sync="experiment.infrastructures"
      :multiple="true"
    ></opensilex-InfrastructureSelector>

    <opensilex-GroupSelector
      label="component.experiment.groups"
      :groups.sync="experiment.groups"
      :multiple="true"
    ></opensilex-GroupSelector>
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

  loadProjects() {
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );
    service
      .searchProjects(undefined,undefined,undefined,undefined,undefined,undefined,undefined, 0, 1000)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            let dto = http.response.result[i];
            this.projectList.push({ value: dto.uri, text: dto.label });
          }
        }
      )
      .catch(this.$opensilex.errorHandler);
  }

  validate() {
    return this.validatorRef.validate();
  }
}
</script>
<style scoped lang="scss">
</style>
