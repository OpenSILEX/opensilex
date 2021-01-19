<template>
  <ValidationObserver ref="validatorRef">
   
    <!-- related projects -->

    <opensilex-ProjectSelector
      label="component.project.relatedProjects"
      :multiple="true"
      :projects.sync="form.related_projects"
    ></opensilex-ProjectSelector>

    <!--Coordinators -->
    <opensilex-UserSelector
      label="component.project.coordinators"
      :users.sync="form.coordinators"
      :multiple="true"
    ></opensilex-UserSelector>

    <!-- Scientific contacts -->
    <opensilex-UserSelector
      label="component.project.scientificContacts"
      :users.sync="form.scientific_contacts"
      :multiple="true"
    ></opensilex-UserSelector>

    <!-- Administrative contacts -->
    <opensilex-UserSelector
      label="component.project.administrativeContacts"
      :users.sync="form.administrative_contacts"
      :multiple="true"
    ></opensilex-UserSelector>

    <!-- Objective -->
    <opensilex-TextAreaForm
      :value.sync="form.objective"
      label="component.project.objective"
      placeholder="component.project.form-objective-placeholder"
    ></opensilex-TextAreaForm>

    <!-- Description -->
    <opensilex-TextAreaForm
      :value.sync="form.description"
      label="component.project.description"
      placeholder="component.project.form-description-placeholder"
    ></opensilex-TextAreaForm>
   </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import { ProjectCreationDTO } from "opensilex-core/index";
@Component
export default class ProjectForm2 extends Vue {
  $opensilex: any;

  @Ref("validatorRef") readonly validatorRef!: any;

  @PropSync("form")
  experiment: ProjectCreationDTO;

  reset() {
    this.validatorRef.reset();
  }

  validate() {
    return this.validatorRef.validate();
  }
}
</script>


