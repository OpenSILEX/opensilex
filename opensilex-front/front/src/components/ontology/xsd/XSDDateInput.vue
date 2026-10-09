<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required" :label="property.name" :path="property.uri" ref="formItemRef">
  <DateForm
    :disabled="false"
    :required="property.is_required"
    v-model:value="internalValue"
    :helpMessage="property.comment"
  ></DateForm>
  </n-form-item>
</template>

<script setup lang="ts">
import DateForm from "@/components/common/forms/DateForm.vue";
import {FormItemRule, NFormItem} from "naive-ui";
import type {VueRDFTypePropertyDTO} from "@/lib";
import {computed, useTemplateRef, watch} from "vue";
import {required} from "@/models/FormFieldsFormatter";


const props = defineProps<{
    property: VueRDFTypePropertyDTO
    name: string;
    is_required: boolean;
    comment?: string;
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

