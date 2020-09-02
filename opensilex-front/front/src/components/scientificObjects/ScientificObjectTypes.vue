<template>
  <opensilex-OntologyClassView
    :rdfClass="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
    icon="ik#ik-settings"
    title="ScientificObjectTypes.title"
  ></opensilex-OntologyClassView>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectTypes extends Vue {
  $opensilex: any;
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ScientificObjectTypes:
    title: Scientific objects types

fr:
  ScientificObjectTypes:
    title: Types d'objets scientifiques
</i18n>
