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

  const { t } = useI18n()

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
      message: t("component.ontology.externalontologies"),
      trigger: ['change', 'blur']
    }
  })

</script>

<style scoped lang="scss">
</style>