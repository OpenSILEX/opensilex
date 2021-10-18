<template>
  <opensilex-WizardForm
    ref="wizardRef"
    :steps="steps"
    createTitle="ProjectForm.create"
    editTitle="ProjectForm.update"
    icon="ik#ik-folder"
    modalSize="lg"
    :initForm="getEmptyForm"
    :createAction="create"
    :updateAction="update"
  >
    <template v-slot:icon></template>
  </opensilex-WizardForm>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";

import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";

@Component
export default class ProjectForm extends Vue {
  $opensilex: any;
  $t: any;

  @Ref("wizardRef") readonly wizardRef!: any;

  steps = [
    {
      component: "opensilex-ProjectForm1",
    },
    {
      component: "opensilex-ProjectForm2",
    },
  ];

  getEmptyForm() {
    return {
      uri: null,
      name: "",
      shortname: undefined,
      financial_funding: undefined,
      description: undefined,
      objective: undefined,
      start_date: "",
      end_date: undefined,
      website: undefined,
      experiments: undefined,
      administrative_contacts: [],
      coordinators: [],
      scientific_contacts: [],
      related_projects: [],
    };
  }

  showCreateForm() {
    this.wizardRef.showCreateForm();
  }

  showEditForm(form) {
    let formDtoCopy = JSON.parse(JSON.stringify(form));
    this.wizardRef.showEditForm(formDtoCopy);
  }

  create(form) {
    this.$opensilex
      .getService("opensilex.ProjectsService")
      .createProject(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        form.uri = uri;
        console.debug("project created", uri);
        this.$emit("onCreate", form);
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Project already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("ProjectForm.projet-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    if (form.website === "") {
      form.website = undefined;
    }
    this.$opensilex
      .getService("opensilex.ProjectsService")
      .updateProject(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("project updated", uri);
        this.$emit("onUpdate", form);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ProjectForm:
    create: Add project
    update: Update the project
    project-already-exists: Project already exists

fr:
  ProjectForm:
    create: Ajouter un projet
    update: Modifier le projet
    project-already-exists: Le projet existe déjà
</i18n>