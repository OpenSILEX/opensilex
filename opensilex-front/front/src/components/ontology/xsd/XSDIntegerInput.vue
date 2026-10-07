<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required">
  <InputForm
    v-model:value="internalValue"
    type="number"
    rules="integer"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
    :placeholder="t('component.ontology.externalOntologies.XSD-placehorlder.XSDIntegerInput-placeholder')"
  ></InputForm>
  </n-form-item>
</template>

<script setup lang="ts">

  import InputForm from "@/components/common/forms/InputForm.vue";
  import {FormItemRule, NFormItem} from "naive-ui";
  import {VueRDFTypePropertyDTO} from "@/lib";
  import {computed} from "vue";
  import {useI18n} from "vue-i18n";
  import {required} from "@/models/FormFieldsFormatter";

  const { t } = useI18n()

  const props = defineProps<{
    property: VueRDFTypePropertyDTO
    name: string;
    is_required: boolean;
    comment?: string;
}>();

const internalValue = defineModel<string>("value");

  const rule = computed<FormItemRule>(() => ({
    trigger: ['input', 'blur'],
    validator: () => {
      const value = internalValue.value;
      const isEmpty = value === undefined || value === null || String(value).trim() === ""
      // field required
      if (isEmpty) {
        return props.property.is_required
            ? new Error(t("validations.required_if", {_field_: props.property.name}))
            : true;
      }
      // integer only (no decimal)
      if (!/^-?\d+$/.test(String(value).trim())) {
        return new Error(t("validations.integer", {_field_: props.property.name}));
      }
      return true;
    }
  }));

</script>

<style scoped lang="scss">
</style>