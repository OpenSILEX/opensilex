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
      helpMessage="ProjectForm.coordinators"
    ></opensilex-UserSelector>

    <!-- Scientific contacts -->
    <opensilex-UserSelector
      label="component.project.scientificContacts"
      :users.sync="form.scientific_contacts"
      :multiple="true"
      helpMessage="ProjectForm.scientificContacts"
    ></opensilex-UserSelector>

    <!-- Administrative contacts -->
    <opensilex-UserSelector
      label="component.project.administrativeContacts"
      :users.sync="form.administrative_contacts"
      :multiple="true"
      helpMessage="ProjectForm.administrativeContacts"
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
// @ts-ignore
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

<i18n>

en:
  ProjectForm:
    scientificContacts: Scientists involved in the project. They must already be declared in the system. If unavailable in the predefined list, people can be added from the Users menu, before the new project creation.
    administrativeContacts: Administrative personnel linked to the project (e.g. human ressources people). They must already be declared in the system.
    coordinators: Project coordinators
fr:
  ProjectForm:
    scientificContacts: Scientifiques impliqués dans le projet. Uniquement les personnes existant dans le système. Si elles ne sont pas disponibles dans la liste prédéfinie, les personnes peuvent être ajoutées à partir du menu Utilisateurs, avant la création du nouveau projet.
    administrativeContacts: Personnel administratif lié au projet (par exemple, les personnes chargées des ressources humaines). Uniquement les personnes existant dans le système.
    coordinators: Coordinateurs du projet

</i18n>



