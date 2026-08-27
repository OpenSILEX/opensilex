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

<script setup lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue, {computed, inject, ref} from "vue";
// @ts-ignore
import { ExperimentCreationDTO } from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {boolean} from "yup";
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()

const props = withDefaults(
    defineProps<{
      editMode?: boolean
      uriGenerated?: boolean
    }>(),
    {
      uriGenerated: true
    }
)

const uriGenerated = ref(props.uriGenerated)
const validatorRef = ref<any>(null)

const user = computed(() => store.state.user)

const experiment = defineModel<ExperimentCreationDTO>('form', {
  required: true
})

function reset() {
  uriGenerated.value = true
  validatorRef.value?.reset()
}

  function validate() {
    return validatorRef.value.validate();
  }
defineExpose({
  reset,
  validate
})
</script>
<style scoped lang="scss">
</style>
