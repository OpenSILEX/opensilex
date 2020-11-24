<template>
  <ValidationObserver ref="validatorRef">
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="experiment.uri"
      label="component.experiment.uri"
      helpMessage="component.experiment.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Label -->
    <opensilex-InputForm
      :value.sync="experiment.label"
      label="component.experiment.label"
      type="text"
      :required="true"
      placeholder="component.experiment.label-placeholder"
    ></opensilex-InputForm>

    <div class="row">
      <!-- Start Date -->
      <div class="col-lg-6">
        <opensilex-InputForm
          :value.sync="experiment.startDate"
          label="component.experiment.startDate"
          type="date"
          :required="true"
          vid="startDate"
        ></opensilex-InputForm>
      </div>

      <!-- End Date -->
      <div class="col-lg-6">
        <opensilex-InputForm
          :value.sync="experiment.endDate"
          label="component.experiment.endDate"
          type="date"
          rules="dateDiff:@startDate"
        ></opensilex-InputForm>
      </div>
    </div>

    <!-- Campaign
    <opensilex-InputForm
      :value.sync="experiment.campaign"
      label="component.experiment.campaign"
      helpMessage="component.experiment.campaign-help"
      type="text"
    ></opensilex-InputForm>
    -->
    <!-- Species -->
    <opensilex-SpeciesSelector
      :required="true"
      :multiple="true"
      :species.sync="experiment.species"
    ></opensilex-SpeciesSelector>
    <!-- Factor -->
    <opensilex-FactorSelector
      :multiple="true"
      :factors.sync="experiment.factors"
    ></opensilex-FactorSelector>
    <!-- Objective -->
    <opensilex-TextAreaForm
      :value.sync="experiment.objective"
      label="component.experiment.objective"
      :required="true"
      placeholder="component.experiment.objective-help"
    ></opensilex-TextAreaForm>

    <!-- Comment -->
    <opensilex-TextAreaForm
      :value.sync="experiment.comment"
      label="component.experiment.comment"
      placeholder="component.experiment.comment-help"
    ></opensilex-TextAreaForm>
  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import {
  ExperimentCreationDTO,
  SpeciesService,
  SpeciesDTO,
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ExperimentForm1 extends Vue {
  $opensilex: any;
  $store: any;

  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  @PropSync("form")
  experiment: ExperimentCreationDTO;

  reset() {
    this.uriGenerated = true;
    this.validatorRef.reset();
  }

  validate() {
    return this.validatorRef.validate();
  }
}
</script>
<style scoped lang="scss">
</style>
