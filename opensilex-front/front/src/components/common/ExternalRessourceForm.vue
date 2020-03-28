<template>
 <Multiselect></Multiselect>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Multiselect from 'vue-multiselect'

import Vue from "vue";

@Component
export default class ExternalRessourceForm extends Vue {
  $opensilex: any;
  $store: any;

  @Prop()
  localUri: string;

  externalUris: any[];
 
}
</script>

<style scoped lang="scss">
</style>

