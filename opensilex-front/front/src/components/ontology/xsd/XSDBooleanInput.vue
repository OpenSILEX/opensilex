<template>
  <n-form-item :show-require-mark="false">
    <CheckboxForm
        v-model:value="internalValue"
        :title="internalValue ? $t('component.common.yes') : $t('component.common.no')"
        :helpMessage="property.comment"
      ></CheckboxForm>
  </n-form-item>
</template>

<script setup lang="ts">

import CheckboxForm from "@/components/common/forms/CheckboxForm.vue";
import {NFormItem} from "naive-ui";
import {VueRDFTypePropertyDTO} from "@/lib";

defineProps<{
  property: VueRDFTypePropertyDTO
}>();

const internalValue = defineModel<string>("value");

</script>

<style scoped lang="scss">
</style>

