<template>
  <div class="static-field">
    <span class="field-view-title">{{ $t(label) }}{{ $t('component.common.colon') }}</span>

    <span class="static-field-line">
      <span :key="index" v-for="(value, index) in values">
        <opensilex-UriLink v-if="to" :uri="value.uri" :value="value.name"  :to="{path: '/'+to+'/'+ encodeURIComponent(value.uri)}"></opensilex-UriLink>
        <opensilex-UriLink
          v-else
          :uri="value.uri"
          :value="value.name"
         
        ></opensilex-UriLink>
        <span v-if="index + 1 < values.length">,</span>
      </span>
    </span>
  </div>
</template>
<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";

@Component
export default class NamelListView extends Vue {
  $opensilex;
  @Prop()
  label: string;

  @Prop()
  to: string;

  @PropSync("objects")
  values: Array<any>;

}
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}
</style>

