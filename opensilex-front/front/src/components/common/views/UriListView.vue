<template>
  <div class="static-field">
    <span class="field-view-title">{{ $t(label) }}</span>

    <span class="static-field-line">
      <span :key="index" v-for="(value, index) in list">
        <opensilex-UriLink
          :uri="value.uri"
          :value="value.value"
          :url="value.url"
          :to="value.to"
        ></opensilex-UriLink>
        <span v-if="index + 1 < list.length">, </span>
      </span>
    </span>
  </div>
</template>
<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";

@Component
export default class UriListView extends Vue {
  $opensilex;
  @Prop()
  label: string;

  @Prop()
  list: Array<any>;

}
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}
</style>

