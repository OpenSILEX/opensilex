<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required" :path="property.uri" ref="formItemRef">
  <InputForm
      v-model:value="internalValue"
      type="text"
      :disabled="false"
      :required="property.is_required"
      :helpMessage="property.comment"
      :placeholder="$t('XSDStringInput.placeholder')"
  />
  </n-form-item>
</template>

<script setup lang="ts">
import InputForm from "@/components/common/forms/InputForm.vue";
import {FormItemRule, NFormItem} from "naive-ui";
import {computed, useTemplateRef, watch} from "vue";
import {VueRDFTypePropertyDTO} from "@/lib";
import {required} from "@/models/FormFieldsFormatter";

const props = defineProps<{
  property: VueRDFTypePropertyDTO
}>();

const internalValue = defineModel<string>("value");
const formItemRef = useTemplateRef<InstanceType<typeof NFormItem>>('formItemRef')

const rule = computed<FormItemRule>(() => props.property.is_required
    ? {...required(props.property.name), validator: () => !!internalValue.value}
    : undefined)

watch(
    internalValue,
    (newVal) => {
      if (newVal) {
        formItemRef.value?.validate()
      }
    }
);


</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  XSDStringInput:
    placeholder: "Enter text, ex : Opensilex"

fr:
  XSDStringInput:
    placeholder: "Saisir du texte, ex : Opensilex"
</i18n>