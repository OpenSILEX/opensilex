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
      :value.sync="experiment.name"
      label="component.experiment.label"
      type="text"
      :required="true"
      placeholder="component.experiment.label-placeholder"
    ></opensilex-InputForm>

    <!-- Period -->
    <opensilex-DateRangePickerForm
        :start.sync="form.start_date"
        :end.sync="form.end_date"
        labelStart="component.common.startDate"
        labelEnd="component.common.endDate"
        :requiredStart="true"
    ></opensilex-DateRangePickerForm>

    <!-- Species -->
    <opensilex-SpeciesSelector
      :multiple="true"
      :species.sync="experiment.species"
    ></opensilex-SpeciesSelector>

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
