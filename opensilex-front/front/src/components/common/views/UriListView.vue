<template>
  <div class="static-field">
    <span class="field-view-title" :class="{'whole-line': !inline}">{{ $t(label) }}</span>

    <!-- Inline list separated by commas -->
    <span class="static-field-line" v-if="inline">
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

    <!-- Bullet points list -->
    <ul v-if="!inline">
      <li :key="index" v-for="(value, index) in list">
        <opensilex-UriLink
            :uri="value.uri"
            :value="value.value"
            :url="value.url"
            :to="value.to"
        ></opensilex-UriLink>
      </li>
    </ul>
  </div>
</template>
<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";
import {UriLinkDestination} from "./UriLink.vue";

export interface UriLinkDescription {
  uri: string,
  value: string,
  url?: string,
  to: UriLinkDestination
}

@Component
export default class UriListView extends Vue {
  $opensilex;
  @Prop()
  label: string;

  @Prop()
  list: Array<UriLinkDescription>;

  @Prop({
    default: true
  })
  inline: boolean;

}
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}
.whole-line {
  width: 100%;
}
</style>

