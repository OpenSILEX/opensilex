<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required" :path="property.uri" ref="formItemRef">
    <InputForm
        type="textarea"
        v-model:value="internalValue"
        :required="property.is_required"
        :helpMessage="property.comment"
        :placeholder="t('component.ontology.externalOntologies.XSD-placehorlder.XSDLongStringInput-placeholder')"
    />
  </n-form-item>
</template>

<script setup lang="ts">
import {FormItemRule, NFormItem} from "naive-ui";
import {useI18n} from "vue-i18n";
import InputForm from "@/components/common/forms/InputForm.vue";
import {computed, useTemplateRef, watch} from "vue";
import {required} from "@/models/FormFieldsFormatter";

const { t } = useI18n()

const props = defineProps<{
  property: {
    uri: string;
    name: string;
    is_required: boolean;
    comment?: string;
  };
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
