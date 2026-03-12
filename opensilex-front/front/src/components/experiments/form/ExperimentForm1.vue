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

    <div class="row">
      <!-- Name -->
      <div class="col-lg-6" id="v-step-name">
        <!-- Label -->
        <opensilex-InputForm
          :value.sync="experiment.name"
          label="component.experiment.label"
          type="text"
          :required="true"
          placeholder="component.experiment.label-placeholder"
        ></opensilex-InputForm>
      </div>

      <!-- AltName -->
      <div class="col-lg-6" id="v-step-alt">
        <opensilex-InputForm
          :value.sync="experiment.alternative_name"
          label="component.common.altName"
          type="text"
        ></opensilex-InputForm>
      </div>

    </div>

    <!-- Period -->
    <opensilex-DateRangePickerForm
        :start.sync="experiment.start_date"
        :end.sync="experiment.end_date"
        labelStart="component.common.startDate"
        labelEnd="component.common.endDate"
        :requiredStart="true"
    ></opensilex-DateRangePickerForm>

    <!-- Objective -->
    <opensilex-TextAreaForm
      :value.sync="experiment.objective"
      label="component.experiment.objective"
      :required="true"
      placeholder="component.experiment.objective-help"
    ></opensilex-TextAreaForm>

    <!-- Comment -->
    <opensilex-TextAreaForm
      :value.sync="experiment.description"
      label="component.experiment.comment"
      placeholder="component.experiment.comment-help"
    ></opensilex-TextAreaForm>
  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ExperimentCreationDTO } from "opensilex-core/index";

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
