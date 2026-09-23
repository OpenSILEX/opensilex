<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required">
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
import {computed} from "vue";


defineProps<{
    property: VueRDFTypePropertyDTO
    name: string;
    is_required: boolean;
    comment?: string;
}>();

const internalValue = defineModel<string>("value");

const rule = computed<FormItemRule>(() => {
  return {
    required: false,
    message: "A traduire",
    trigger: ['change', 'blur']
  }
})
</script>

<style scoped lang="scss">
</style>

