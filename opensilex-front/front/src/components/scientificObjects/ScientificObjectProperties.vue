<template>
  <opensilex-OntologyPropertyView
    :domain="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
    icon="ik#ik-settings"
    title="ScientificObjectProperties.title"
    add="ScientificObjectProperties.add"
    update="ScientificObjectProperties.update"
    create="ScientificObjectProperties."
  />
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectProperties extends Vue {
  $opensilex: any;
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  ScientificObjectProperties:
    title: Scientific objects properties

fr:
  ScientificObjectProperties:
    title: Propriétés des objets scientifiques
</i18n>
