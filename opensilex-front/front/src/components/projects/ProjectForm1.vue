<template>
  <ValidationObserver ref="validatorRef">
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.project.project-uri"
      helpMessage="component.common.uri-help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
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
    <opensilex-DateRangePickerForm
        :start.sync="form.start_date"
        :end.sync="form.end_date"
        labelStart="component.common.startDate"
        labelEnd="component.common.endDate"
        type="date"
        :requiredStart="true"
    ></opensilex-DateRangePickerForm>

    <!-- Financial funding -->

    <opensilex-InputForm
      :value.sync="form.financial_funding"
      label="component.project.financialFunding"
      type="text"
      placeholder="component.project.form-financialFunding-placeholder"
    ></opensilex-InputForm>

    <!-- Website -->

    <opensilex-InputForm
      :value.sync="form.website"
      label="component.project.website"
      type="url"
      rules="url"
      placeholder="component.project.form-website-placeholder"
    ></opensilex-InputForm>

   </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import { ProjectCreationDTO } from "opensilex-core/index";
@Component
export default class ProjectForm1 extends Vue {
  $opensilex: any;
  $store: any;

  get user() {
    return this.$store.state.user;
  }

  @Prop({ default: true })
  uriGenerated;


  @Ref("validatorRef") readonly validatorRef!: any;

  @Prop()
  editMode;


  @PropSync("form")
  project: ProjectCreationDTO;

  reset() {
    this.uriGenerated = true;
    this.validatorRef.reset();
  }

  validate() {

    return this.validatorRef.validate();
  }
}
</script>


