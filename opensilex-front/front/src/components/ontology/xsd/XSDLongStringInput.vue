<template>
   <n-form
      v-model:value="internalValue"
      :label="property.name"
      :required="property.is_required"
      :helpMessage="property.comment"
      :placeholder="t('component.ontology.externalOntologies.XSD-placehorlder.XSDLongStringInput-placeholder')"
   ></n-form>
</template>

<script setup lang="ts">
import {NForm} from "naive-ui";
import {useI18n} from "vue-i18n";

const { t } = useI18n()

defineProps<{
  property: {
    name: string;
    is_required: boolean;
    comment?: string;
  };
}>();

const internalValue = defineModel<string>("value");

</script>

<style scoped lang="scss">
</style>
