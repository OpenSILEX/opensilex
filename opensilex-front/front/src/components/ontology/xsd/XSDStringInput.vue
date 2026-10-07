<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required">
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
import {computed} from "vue";
import {VueRDFTypePropertyDTO} from "@/lib";

defineProps<{
  property: VueRDFTypePropertyDTO
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

<i18n>
en:
  XSDStringInput:
    placeholder: "Enter text, ex : Opensilex"

fr:
  XSDStringInput:
    placeholder: "Saisir du texte, ex : Opensilex"
</i18n>