<template>
  <div class="static-field">
    <span class="field-view-title">{{$t(label)}}</span>
    <span class="static-field-line">{{value}}</span>
    <opensilex-UriLink class="uri" :uri="uri" :url="uri" :value="uri" :to="to" :allowCopy="allowCopy"></opensilex-UriLink>
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

  @Prop({
    default: true
  })
  allowCopy: boolean;
}
</script>

<style scoped lang="scss">

.static-field .uri { padding-left: 20px;}

</style>