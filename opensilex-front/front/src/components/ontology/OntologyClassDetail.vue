<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>TITLE TODO</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.label"></opensilex-StringView>
      {{selected}}
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class OntologyClassDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;
}
</script>

<style scoped lang="scss">
</style>


<i18n>
</i18n>
