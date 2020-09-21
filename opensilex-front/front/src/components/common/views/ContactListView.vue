<template>
  <div class="static-field">
    <span class="static-field-key">{{ $t(label) }}{{ $t('component.common.colon') }}</span>
    <ul class="static-field-list" :key="index" v-for="(value, index) in values">
      <li class="inline-action">
        <span class="uri inline-action">
          {{ value.firstName }} {{ value.lastName }}
          <a
            href="#"
            v-on:click="copyUri(value.email, $event)"
            class="inline-action-btn"
            :title="$t('component.copyToClipboard.copyEmail')"
          >
            <i class="ik ik-copy"></i>
          </a>
        </span>
      </li>
    </ul>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ContactListView extends Vue {
  @Prop()
  label: string;

  @PropSync("contacts")
  values: Array<any>;
}
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}
</style>

