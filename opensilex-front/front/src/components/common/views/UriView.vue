<template>
  <div class="static-field">
  <span :class="['field-view-title', customClass]">{{$t(title)}}</span>
    <span class="static-field-line">
      <opensilex-UriLink :uri="uri" :value="value" :url="url" :to="to" @linkClicked="$emit('linkClicked')"></opensilex-UriLink>
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";

@Component
export default class UriView extends Vue {
  @Prop()
  uri: string;

  @Prop()
  value: string;

  @Prop()
  url: string;

  @Prop()
  to: string;
  
  @Prop({default : "component.common.uri"})
  title: string;

  @Prop({ default: '' })
customClass: string;


  copy = copy;
}
</script>

<style scoped lang="scss">
.static-field-line {
  word-wrap: normal;
}

.specificProperties {
  min-width: 250px
}
</style>

