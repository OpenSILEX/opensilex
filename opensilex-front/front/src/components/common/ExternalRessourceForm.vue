<template>
 <Multiselect></Multiselect>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Multiselect from 'vue-multiselect'

import { OntologyReferenceDTO }  from "opensilex-core/index"; 
import Vue from "vue";

@Component
export default class ExternalRessourceForm extends Vue {
  $opensilex: any;
  $store: any;

  @Prop()
  instanceUri: string;

  @Prop()
  ontologyReferences :OntologyReferenceDTO[];
}
</script>

<style scoped lang="scss">
</style>

