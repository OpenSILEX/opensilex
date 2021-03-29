<template>
  <div class="static-field">
    <span class="field-view-title">{{$t(label)}}:</span>
    <span class="static-field-line">{{value}}</span>
    <opensilex-UriLink class="uri" v-if="uri.startsWith('http')" :uri=uri :url=uri></opensilex-UriLink>
    <opensilex-UriLink class="uri" v-else :uri=uri :to=to></opensilex-UriLink>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class LabelUriView extends Vue {
  @Prop()
  label: string;

  @Prop()
  value: string;

  @Prop()
  uri: string;

  @Prop()
  to: string;
}
</script>

<style scoped lang="scss">

.static-field .uri { padding-left: 20px;}

</style>