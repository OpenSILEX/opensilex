<template>
  <opensilex-OntologyClassView :rdfClass="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"></opensilex-OntologyClassView>
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
</i18n>
