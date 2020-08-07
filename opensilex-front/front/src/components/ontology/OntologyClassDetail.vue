<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>{{$t("OntologyClassDetail.title")}}</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.label"></opensilex-StringView>
      <!-- {{selected}} -->
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
en:
  OntologyClassDetail:
    title: Object type detail 

fr:
  OntologyClassDetail:
    title: Détail du type d'objet

</i18n>

