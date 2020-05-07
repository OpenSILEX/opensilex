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
      :value.sync="form.shortname"
      label="component.project.name"
      type="text"
      :required="true"
      placeholder="component.project.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Longname -->
    <opensilex-InputForm
      :value.sync="form.label"
      label="component.project.longname"
      type="text"
      placeholder="component.project.form-longname-placeholder"
    ></opensilex-InputForm>

    <!-- Period -->
    <b-form inline>

      <opensilex-DateForm
        :value.sync="form.startDate"
        label="component.common.startDate"
        placeholder="Select a date"
        :required="true"
      ></opensilex-DateForm>

      <opensilex-DateForm
        :value.sync="form.endDate"
        label="component.common.endDate"
        placeholder="Select a date"
      ></opensilex-DateForm>

    </b-form>
<!-- 
    <b-form inline>
      <label class="mr-sm-2 required" for="inline-form-custom-select-pref">Start Date</label>
      <b-input-group class="mt-3 mb-3" size="sm" id="inline-form-custom-select-pref">
        <ValidationProvider
          :name="$t('component.common.startDate')"
          rules="required"
          v-slot="{ errors }"
        >
          <datePicker
            v-model="form.startDate"
            input-class="form-control"
            placeholder="Select a date"
            :clear-button="true"
            @input="onStartDateSelected"
            @cleared="onStartDateCleared"
          ></datePicker>

          <div class="error-message alert alert-danger">{{ errors[0] }}</div>
        </ValidationProvider>
      </b-input-group>

      <label class="mr-sm-2 ml-4" for="inline-2">End Date</label>
      <b-input-group class="mt-3 mb-3" size="sm" id="inline-2">
        <datePicker
          v-model="form.endDate"
          input-class="form-control"
          placeholder="Select a date"
          :clear-button="true"
          @input="onEndDateSelected"
          @cleared="onEndDateCleared"
        ></datePicker>
      </b-input-group>
    </b-form> -->

    <!-- Financial funding -->

    <opensilex-InputForm
      :value.sync="form.hasFinancialFunding"
      label="component.project.financialFunding"
      type="text"
      placeholder="component.project.form-financialFunding-placeholder"
    ></opensilex-InputForm>

    <!-- Website -->

    <b-form-group :label="$t('component.project.website') + ':'" label-for="website">
      <b-form-input
        id="website"
        v-model="form.homePage"
        type="text"
        :placeholder="$t('component.project.form-website-placeholder')"
      ></b-form-input>
    </b-form-group>

    <!--Coordinators -->
    <!-- <opensilex-UserSelector
      label="component.project.coordinators"
      :users.sync="form.coordinators"
      :multiple="true"
    ></opensilex-UserSelector>-->

    <!-- Scientific contacts -->
    <!-- <opensilex-UserSelector
      label="component.project.scientificContacts"
      :users.sync="form.scientificContacts"
      :multiple="true"
    ></opensilex-UserSelector>-->

    <!-- Administrative contacts -->
    <!-- <opensilex-UserSelector
      label="component.project.administrativeContacts"
      :users.sync="form.administrativeContacts"
      :multiple="true"
    ></opensilex-UserSelector>-->

 <!-- Objective -->
    <opensilex-TextAreaForm
      :value.sync="form.objective"
      label="component.project.objective"
      :required="true"
      placeholder="component.project.form-objective-placeholder"
    ></opensilex-TextAreaForm>

    <!-- Description -->
    <opensilex-TextAreaForm
      :value.sync="form.description"
      label="component.project.description"
      :required="true"
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

  onStartDateSelected() {
    console.log("start date" + this.form.startDate);
    if (this.form.startDate !== null) {
      console.log("ici");
      this.form.startDate = this.format(this.form.startDate);
    }
  }

  onEndDateSelected() {
    console.log("end date" + this.form.endDate);
    if (this.form.endDate !== null) {
      this.form.endDate = this.format(this.form.endDate);
    }
  }

  onStartDateCleared() {
    console.log("start date cleared");
    this.form.startDate = "";
  }
  onEndDateCleared() {
    console.log("end date cleared");
    this.form.endDate = "";
  }

  format(date) {
    var d = new Date(date),
      month = "" + (d.getMonth() + 1),
      day = "" + d.getDate(),
      year = d.getFullYear();

    if (month.length < 2) month = "0" + month;
    if (day.length < 2) day = "0" + day;

    return [year, month, day].join("-");
  }
}
</script>

<style scoped>
div >>> label.required::after {
  content: " * ";
  color: red;
}
</style>

