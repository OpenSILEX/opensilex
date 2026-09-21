<template>
   <TextAreaForm
      v-model:value="internalValue"
      :label="property.name"
      :required="property.is_required"
      :helpMessage="property.comment"
      :placeholder="$t('XSDLongStringInput.placeholder')"
   ></TextAreaForm>
</template>

<script setup lang="ts">
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";

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

<i18n>
en:
    XSDLongStringInput:
        placeholder: "Enter a long text, ex : OpenSILEX is an ontology-driven Information System designed for life science data."
fr:
    XSDLongStringInput:
        placeholder: "Saisissez un texte long, ex : OpenSILEX est un système d'information guidé par des ontologies, conçu pour les données issues des sciences de la vie "
</i18n>

