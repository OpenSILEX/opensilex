<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.user.user-uri"
      helpMessage="component.common.uri.help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.label"
      label="component.project.name"
      type="text"
      :required="true"
      placeholder="component.project.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Short name -->
    <opensilex-InputForm
      :value.sync="form.shortname"
      label="component.project.shortname"
      type="text"
      placeholder="component.project.form-shortname-placeholder"
    ></opensilex-InputForm>

    <!-- Period -->
    <div class="row">
      <!-- Start Date -->
      <div class="col-lg-6">
        <opensilex-InputForm
          :value.sync="form.startDate"
          label="component.common.startDate"
          type="date"
          :required="true"
        ></opensilex-InputForm>
      </div>

      <!-- End Date -->
      <div class="col-lg-6">
        <opensilex-InputForm
          :value.sync="form.endDate"
          label="component.common.endDate"
          type="date"
        ></opensilex-InputForm>
      </div>
    </div>

    <!-- Financial funding -->

    <opensilex-InputForm
      :value.sync="form.hasFinancialFunding"
      label="component.project.financialFunding"
      type="text"
      placeholder="component.project.form-financialFunding-placeholder"
    ></opensilex-InputForm>

    <!-- Website -->

    <opensilex-InputForm
      :value.sync="form.homePage"
      label="component.project.website"
      type="text"
      rules="url"
      placeholder="component.project.form-website-placeholder"
    ></opensilex-InputForm>

    <!--Coordinators -->
    <opensilex-UserSelector
      v-if="form.coordinators"
      label="component.project.coordinators"
      :users.sync="form.coordinators"
      :multiple="true"
    ></opensilex-UserSelector>

    <!-- Scientific contacts -->
    <opensilex-UserSelector
      v-if="form.scientificContacts"
      label="component.project.scientificContacts"
      :users.sync="form.scientificContacts"
      :multiple="true"
    ></opensilex-UserSelector>

    <!-- Administrative contacts -->
    <opensilex-UserSelector
      v-if="form.administrativeContacts"
      label="component.project.administrativeContacts"
      :users.sync="form.administrativeContacts"
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
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { ProjectGetDTO, ProjectsService } from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class ProjectForm extends Vue {
  $opensilex: any;

  get user() {
    return this.$store.state.user;
  }

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        label: "",
        shortname: "",
        hasFinancialFunding: "",
        description: "",
        objective: "",
        startDate: "",
        endDate: "",
        keywords: [],
        homePage: "",
        experiments: [],
        administrativeContacts: [],
        coordinators: [],
        scientificContacts: [],
        relatedProjects: []
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      uri: null,
      label: "",
      shortname: undefined,
      hasFinancialFunding: undefined,
      description: undefined,
      objective: undefined,
      startDate: "",
      endDate: undefined,
      keywords: undefined,
      homePage: undefined,
      experiments: undefined,
      administrativeContacts: [],
      coordinators: [],
      scientificContacts: [],
      relatedProjects: undefined
    };
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.ProjectsService")
      .createProject(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Project created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Project already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("component.project.errors.project-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
      .getService("opensilex.ProjectsService")
      .updateProject(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Project updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped>
div >>> label.required::after {
  content: " * ";
  color: red;
}
</style>

