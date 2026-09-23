<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required">
  <DateTimeForm
    v-model:value="internalValue"
    :label="property.name"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
  ></DateTimeForm>
  </n-form-item>
</template>

<script setup lang="ts">

import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import {FormItemRule, NFormItem} from "naive-ui";
import {VueRDFTypePropertyDTO} from "@/lib";
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

