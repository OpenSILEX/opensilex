<template>
  <b-card v-if="selected && selected.uri">
    <opensilex-ScientificObjectDetailBasic
      :objectByContext="objectByContext"
      :selected="selected"
      :withReturnButton="withReturnButton"
      :lightTab="lightTab"
    ></opensilex-ScientificObjectDetailBasic>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  @Prop()
  lightTab;

  @Prop({
    default: () => []
  })
  objectByContext;

  @Prop({
    default: false
  })
  withReturnButton;

}
</script>