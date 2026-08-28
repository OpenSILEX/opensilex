<template>
  <n-form
      ref="validatorRef"
      :rules="rules"
      :model="experiment"
  >
    <!-- URI -->
    <n-form-item
    path="uri">
    <UriForm
      v-model:uri="experiment.uri"
      label="component.experiment.uri"
      helpMessage="component.experiment.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></UriForm>
    </n-form-item>

    <div class="row">
      <!-- Name -->
      <div class="col-lg-6" id="v-step-name">
        <!-- Label -->
        <n-form-item
        path="name">
        <InputForm
          v-model:value="experiment.name"
          label="component.experiment.label"
          type="text"
          :required="true"
          :placeholder="t('component.project.filter-label-placeholder')"
        ></InputForm>
        </n-form-item>
      </div>

      <!-- AltName -->
      <div class="col-lg-6" id="v-step-alt">
        <n-form-item
        path="alternative_name">
        <InputForm
          v-model:value="experiment.alternative_name"
          label="component.common.altName"
          type="text"
        ></InputForm>
        </n-form-item>
      </div>
    </div>

    <!-- Period -->
    <DateRangePickerForm
        v-model:start="experiment.start_date"
        v-model:end="experiment.end_date"
        labelStart="component.experiment.startDate"
        labelEnd="component.experiment.endDate"
        :requiredStart="true"
        startDatePath="start_date"
        endDatePath="end_date"
    ></DateRangePickerForm>

    <!-- Objective -->
    <n-form-item
      path="objective">
    <TextAreaForm
      v-model:value="experiment.objective"
      label="component.experiment.objective"
      :required="true"
      :placeholder="t('component.experiment.objective-help')"
    ></TextAreaForm>
    </n-form-item>

    <!-- Comment -->
    <TextAreaForm
      v-model:value="experiment.description"
      label="component.experiment.comment"
      :placeholder="t('component.experiment.objective-help')"
    ></TextAreaForm>
  </n-form>
</template>

<script setup lang="ts">
import Vue, {computed, inject, ref, useTemplateRef} from "vue";
// @ts-ignore
import { ExperimentCreationDTO } from "core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {boolean} from "yup";
import UriForm from "@/components/common/forms/UriForm.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import DateRangePickerForm from "@/components/common/forms/DateRangePickerForm.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";
import {FormRules, NForm, NFormItem} from "naive-ui";
import {useI18n} from "vue-i18n";
import {required} from "@/models/FormFieldsFormatter";
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
const { t } = useI18n()


const rules = computed<FormRules>(() => ({
  name: {
    required: true,
    message: t("component.experiment.name-required"),
    trigger: ["blur", "change"],
  },

  start_date: {
    required: true,
    message: t("component.experiment.start-date-required"),
    trigger: ["blur", "change"],
  },

  objective: {
    required: true,
    message: t("component.experiment.objective-required"),
    trigger: ["blur", "change"],
  },
}));

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
const validatorRef = useTemplateRef<InstanceType<typeof NForm>>("validatorRef")

const user = computed(() => store.state.user)

const experiment = defineModel<ExperimentCreationDTO>('form', {
  required: true
})

function reset() {
  uriGenerated.value = true
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
